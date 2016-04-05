package demorabbit.demos.recovery;

import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

import java.util.*;

/**
 * Created by mmoraes on 01/04/16.
 */
public class RetryOnQueueMessageRecovery implements MessageRecoverer {

    private final List<RetryQueue> retryQueues = new LinkedList<>();

    private RabbitTemplate rabbitTemplate;
    private RabbitAdmin rabbitAdmin;
    private DirectExchange exchange;
    private String dlq;
    private String prefix;


    public RetryOnQueueMessageRecovery(String prefix, ConnectionFactory connectionFactory) {
        this.rabbitTemplate = createNoTxRabbitTemplate(connectionFactory);
        this.rabbitAdmin = createRabbitAdmin(connectionFactory);
        this.prefix = prefix;
        this.dlq = prefix + ":dlq";
        exchange = new DirectExchange(prefix + ":retry", true, false);
        rabbitAdmin.declareExchange(exchange);
        declareDLQ();
    }

    private void declareDLQ() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("", "");
        Queue queue = new Queue(dlq, true, false, false, args);
        rabbitAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(exchange).withQueueName();
        rabbitAdmin.declareBinding(binding);
    }

    protected RabbitAdmin createRabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    public RetryOnQueueMessageRecovery retry(int retryTime, int maxRetry) {
        RetryQueue retryQueue = new RetryQueue(prefix + ":" + retryQueues.size() + ":retry", retryTime, maxRetry);
        retryQueues.add(retryQueue);
        Queue queue = retryQueue.createQueue();
        rabbitAdmin.declareQueue(queue);
        Binding binding = BindingBuilder.bind(queue).to(exchange).withQueueName();
        rabbitAdmin.declareBinding(binding);
        return this;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        Retry retry = new Retry(message);

        Optional<RetryQueue> retryQueueOptional = retryQueues.stream()
                .filter(q -> retry.accept(q)).findFirst();

        if (retryQueueOptional.isPresent()) {
            sendToRetry(retry, retryQueueOptional.get(), cause);
        } else {
            sendToDLQ(retry, cause);
        }
    }

    private void sendToDLQ(Retry retry, Throwable cause) {
        new RepublishMessageRecoverer(rabbitTemplate, exchange.getName(), dlq)
                .recover(retry.message, cause);
    }

    private void sendToRetry(Retry retry, RetryQueue retryQueue, Throwable cause) {
        retry.update(retryQueue);
        new RepublishMessageRecoverer(rabbitTemplate, exchange.getName(), retryQueue.name)
                .recover(retry.message, cause);
   }

    protected RabbitTemplate createNoTxRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setChannelTransacted(false);
        return rabbitTemplate;
    }

    private static class RetryQueue {
        private Integer maxRetry;
        private Integer retryTime;
        private String name;

        public RetryQueue(String name, Integer retryTime, Integer maxRetry) {
            this.name = name;
            this.maxRetry = maxRetry;
            this.retryTime = retryTime;
        }

        public boolean accept(Retry retry) {
            return false;
        }

        public Queue createQueue() {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("x-message-ttl", retryTime);
            return new Queue(name, true, false, false, args);
        }
    }

    private class Retry {
        public static final String RETRY_COUNTER = ".retry.counter";
        private final Message message;
        private final Map<String, Object> headers;

        public Retry(Message message) {
            this.message = message;
            this.headers = message.getMessageProperties().getHeaders();
        }

        public void update(RetryQueue retryQueue) {
            Integer counter = getCounter(retryQueue);
            counter++;
            headers.put(retryQueue + RETRY_COUNTER, counter);
            headers.put("dead-letter-exchange", message.getMessageProperties().getReceivedExchange());
        }

        public boolean accept(RetryQueue retryQueue) {
            Integer counter = getCounter(retryQueue);
            return counter <= retryQueue.maxRetry;
        }

        private Integer getCounter(RetryQueue retryQueue) {
            Integer counter = 1;
            if (headers.containsKey(retryQueue.name + RETRY_COUNTER)) {
                counter = (Integer) headers.get(retryQueue.name + RETRY_COUNTER);
            }
            return counter;
        }
    }
}
