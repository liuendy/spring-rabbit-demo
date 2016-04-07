package demorabbit.demos.rabbitAPI;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StopWatch;

/**
 * Created at 07/01/16
 */
@SpringBootApplication
public class RabbitClientAPI {

    private static final Log LOG = LogFactory.getLog(RabbitClientAPI.class);
    public static final String QUEUE = "demorabbit.rabbitAPI";
    public static final String EXCHANGE = "demorabbit.directExchange";
    private static byte[] payload = null;

    private ConnectionFactory connectionFactory;

    private Connection connection;

    private static final int PERSISTENT = 1;
    private static final int TRANSACTIONAL = 2;

    public static void main(final String[] args) throws Exception {
        System.setProperty("spring.main.web_environment", "false");

        final RabbitClientAPI demo = SpringApplication.run(RabbitClientAPI.class, args).getBean(RabbitClientAPI.class);

        payload = new String(new char[2048]).replace('\0', 'X').getBytes();

        demo.run();
        System.exit(0);
    }

    void run() throws Exception {
        // inicializa o ConnectionFactory
        initializeConnectionFactory();

        // cria uma conexao
        connection = connectionFactory.newConnection();

        // declara a fila e faz o bind
        declareQueueAndBinding();

        publishAndConsumeDemo();
    }

    private void declareQueueAndBinding() throws IOException, TimeoutException {
        final Channel channel = connection.createChannel();
        try {
            channel.exchangeDeclare(EXCHANGE, "direct", false);
            channel.queueDeclare(QUEUE, false, false, false, null);
            channel.queueBind(QUEUE, EXCHANGE, QUEUE);
        } finally{
            channel.close();
        }

    }

    private void initializeConnectionFactory() throws IOException, TimeoutException {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setVirtualHost("/");
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setAutomaticRecoveryEnabled(true);
    }

    private void publishAndConsumeDemo() throws Exception {
        LOG.info("=============== NonTransaction NonPersistent =================================");
        publish();
        consume();

        LOG.info("================== Transaction Persistent ====================================");
        publish(TRANSACTIONAL + PERSISTENT);
        consume(TRANSACTIONAL);
        LOG.info("==============================================================================");
    }

    private void consume() throws Exception {
        consume(0);
    }

    private void publish() throws IOException, TimeoutException {
        publish(0);
    }

    private void publish(final int options) throws IOException, TimeoutException {
        final boolean persistent = (options & PERSISTENT) == PERSISTENT;
        final boolean transactional = (options & TRANSACTIONAL) == TRANSACTIONAL;

        String tag = "publish (transaction=%b, persistent=%b)";
        tag = String.format(tag, transactional, persistent);

        final StopWatch stopWatch = new StopWatch(tag);
        stopWatch.start();

        final Channel channel = connection.createChannel();
        beginTx(channel, transactional);

        try {
            final AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();
            if (persistent) {
                builder.deliveryMode(2);
            }
            final AMQP.BasicProperties props = builder.build();
            channel.basicPublish(EXCHANGE, QUEUE, props, payload);
            commitTx(channel, transactional);
        } catch (final Exception e) {
            rollbackTx(channel, transactional);
        } finally {
            channel.close();
        }

        stopWatch.stop();
        LOG.info(stopWatch);
    }

    private void consume(final int options) throws Exception {
        final boolean transactional = (options & TRANSACTIONAL) == TRANSACTIONAL;

        String tag = "consume (transaction=%b)";
        tag = String.format(tag, transactional);

        final StopWatch stopWatch = new StopWatch(tag);
        stopWatch.start();

        final Channel channel = connection.createChannel();
        beginTx(channel, transactional);

        try {
            final SimpleConsumer simpleConsumer = new SimpleConsumer(channel);
            // registra o consumidor
            channel.basicConsume(QUEUE, true, simpleConsumer);
            // espera o consumidor recever a mensagem
            simpleConsumer.futureMessage.get(3, TimeUnit.SECONDS);
            final String message = simpleConsumer.futureMessage.get();
            commitTx(channel, transactional);
        } catch (final Exception e) {
            rollbackTx(channel, transactional);
        } finally{
            channel.close();
        }

        stopWatch.stop();
        LOG.info(stopWatch);
    }

    private void commitTx(final Channel channel, final boolean transactional) throws IOException {
        if (transactional) {
            channel.txCommit();
        }
    }

    private void rollbackTx(final Channel channel, final boolean transactional) throws IOException {
        if (transactional) {
            channel.txRollback();
        }
    }

    private void beginTx(final Channel channel, final boolean transactional) throws IOException {
        if (transactional) {
            channel.txSelect();
        }
    }

    /**
     * Consumidor da fila
     */
    class SimpleConsumer extends DefaultConsumer {

        CompletableFuture<String> futureMessage = new CompletableFuture<>();

        public SimpleConsumer(final Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope,
                                   final AMQP.BasicProperties properties, final byte[] body)
                throws IOException {
            futureMessage.complete(new String(body));
        }
    }

}
