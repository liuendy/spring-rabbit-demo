package demorabbit.demos.recovery;

import static demorabbit.demos.recovery.RetryAndDLQMessageRecovery.DEFAULT_EXCHANGE;
import static demorabbit.demos.recovery.RetryAndDLQMessageRecovery.DLQ_SUFIX;
import static demorabbit.demos.recovery.RetryAndDLQMessageRecovery.RETRY_SUFIX;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created at 22/12/15
 */
@SpringApplicationConfiguration(RetryAndDLQMessageRecoveryTest.class)
@EnableRabbit
@SpringBootApplication
@RunWith(SpringJUnit4ClassRunner.class)
public class RetryAndDLQMessageRecoveryTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String testQueue = "testQueue";

    private Message message;

    private RabbitAdmin rabbitAdmin;

    @Before
    public void setUp() throws Exception {
        rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        rabbitAdmin.declareQueue(new Queue(testQueue));
        message = MessageBuilder.withBody("message".getBytes()).build();
        message.getMessageProperties().setConsumerQueue(testQueue);
    }

    @After
    public void tearDown() throws Exception {
        rabbitAdmin.deleteQueue(testQueue);
        rabbitAdmin.deleteQueue(testQueue + RETRY_SUFIX);
        rabbitAdmin.deleteQueue(testQueue + DLQ_SUFIX);
        rabbitAdmin.deleteExchange(DEFAULT_EXCHANGE);
    }

    @Test
    public void testRecover_dlq() throws Exception {
        final ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        final RetryAndDLQMessageRecovery messageRecovery = new RetryAndDLQMessageRecovery(connectionFactory, testQueue);
        messageRecovery.config().setMaxRetry(2);

        final Integer originalPriority = message.getMessageProperties().getPriority();
        try {
            messageRecovery.recover(message, new RuntimeException());
        } catch (final AmqpRejectAndDontRequeueException e1) {
            try {
                message.getMessageProperties().setConsumerQueue(testQueue);
                messageRecovery.recover(message, new RuntimeException());
            } catch (final AmqpRejectAndDontRequeueException e2) {
                // assert header was clear
                assertThat(message, notNullValue());
                final Map<String, Object> headers = message.getMessageProperties().getHeaders();
                assertThat(headers.get(RecoverableMessage.RETRY_COUNTER), nullValue());
                assertThat(headers.get(RecoverableMessage.RETRY_TIME), nullValue());
                //assertThat(headers.get(RecoverableMessage.RETRY_ORIGINAL_PRIORITY), is(originalPriority));
            }

        }
    }

    @Test(expected = AmqpRejectAndDontRequeueException.class)
    public void testRecover_retry() throws Exception {
        final ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        final RetryAndDLQMessageRecovery messageRecovery = new RetryAndDLQMessageRecovery(connectionFactory, testQueue);
        messageRecovery.config().setMaxRetry(3);

        final Integer originalPriority = message.getMessageProperties().getPriority();
        try {
            messageRecovery.recover(message, new RuntimeException());
        } catch (final AmqpRejectAndDontRequeueException e1) {
            try {
                message.getMessageProperties().setConsumerQueue(testQueue);
                messageRecovery.recover(message, new RuntimeException());
            } catch (final AmqpRejectAndDontRequeueException e2) {
                assertThat(e1, Matchers.notNullValue());
            }
        }
        messageRecovery.recover(message, new RuntimeException());
    }

    class DummyConsumer extends DefaultConsumer {

        private final CountDownLatch countDown = new CountDownLatch(1);

        public DummyConsumer(final Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties, final byte[] body) throws IOException {
            countDown.countDown();
            getChannel().basicAck(1, false);
        }

        public void waitDelivery() throws InterruptedException {
            countDown.await(5000, TimeUnit.MILLISECONDS);
        }
    }
}