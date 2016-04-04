package demorabbit.demos.recovery;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.doReturn;

/**
 * Created by mmoraes on 01/04/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class RetryOnQueueMessageRecoveryTest {

    @Mock
    private Message message;
    @Mock
    private RabbitTemplate rabbitTemplate;

    private Throwable cause = new NullPointerException();

    private RetryOnQueueMessageRecovery messageRecovery;

    @Before
    public void setUp() throws Exception {
        messageRecovery = new RetryOnQueueMessageRecovery("test", null) {
            @Override
            protected RabbitTemplate createNoTxRabbitTemplate(ConnectionFactory connectionFactory) {
                return rabbitTemplate;
            }
        };
    }

    @Test
    public void test() {
        messageRecovery
                .retry(300, 2)
                .retry(2000, 1)
                .retry(10000, 1);

        messageRecovery.recover(message, cause);
    }
}