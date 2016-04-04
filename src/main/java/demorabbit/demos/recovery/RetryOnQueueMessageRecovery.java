package demorabbit.demos.recovery;

import com.sun.net.httpserver.Authenticator;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by mmoraes on 01/04/16.
 */
public class RetryOnQueueMessageRecovery implements MessageRecoverer {

    private final List<RetryQueue> retryQueues = new LinkedList<>();

    private RabbitTemplate rabbitTemplate;
    private RabbitAdmin rabbitAdmin;
    private DirectExchange exchange;
    private RetryQueue dlq;
    private String prefix;


    public RetryOnQueueMessageRecovery(String prefix, ConnectionFactory connectionFactory) {
        this.rabbitTemplate = createNoTxRabbitTemplate(connectionFactory);
        this.rabbitAdmin = createRabbitAdmin(connectionFactory);
        this.prefix = prefix;
        this.dlq = new RetryQueue(prefix + ":dlq");
        exchange = new DirectExchange(prefix + ":retry", true, false);
        rabbitAdmin.declareExchange(exchange);
        declareQueue(this.dlq);
    }

    private void declareQueue(RetryQueue retryQueue) {
        Queue queue = retryQueue.createQueue();
        rabbitAdmin.declareQueue(queue);
        BindingBuilder.bind(queue).to(exchange).withQueueName();
//        rabbitAdmin.declareBinding();
    }

    protected RabbitAdmin createRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    public RetryOnQueueMessageRecovery retry(int retryTime, int maxRetry) {
        RetryQueue retryQueue = new RetryQueue(retryTime, maxRetry);
        retryQueues.add(retryQueue);
        return this;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        Retry retry = new Retry(message);
        Optional<RetryQueue> candidateQueue = retryQueues.stream()
                .filter(q -> retry.accept(q)).findFirst();
        RetryQueue targetQueue = candidateQueue.orElse(dlq);
        retry.update(targetQueue);
        this.rabbitTemplate.send(exchange, retry.routingKey, retry.message);
    }

    protected RabbitTemplate createNoTxRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setChannelTransacted(false);
        return rabbitTemplate;
    }

    private static class RetryQueue {
        private Integer maxRetry;
        private Integer waitTime;
        private String name;

        public RetryQueue(String name) {
            this.name = name;
        }

        public RetryQueue(Integer maxRetry, Integer waitTime) {
            this.maxRetry = maxRetry;
            this.waitTime = waitTime;
        }

        public boolean accept(Retry retry) {
            return false;
        }

        public Queue createQueue() {
            return null;
        }
    }

    private class Retry {
        private final Message message;
        private String routingKey;

        public Retry(Message message) {
            this.message = message;
        }

        public void updateCounter() {

        }

        public void update(RetryQueue targetQueue) {

        }

        public boolean accept(RetryQueue q) {
            return false;
        }
    }
}
