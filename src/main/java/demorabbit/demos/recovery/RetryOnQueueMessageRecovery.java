package demorabbit.demos.recovery;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;

/**
 * Created by mmoraes on 01/04/16.
 */
public class RetryOnQueueMessageRecovery implements MessageRecoverer {

    private final List<RetryQueue> retryQueues = new LinkedList<>();

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;
    private final String deadletterExchange;
    private final String dlq;
    private final String prefix;

    public RetryOnQueueMessageRecovery(final String prefix, final String deadletterExchange,
            final ConnectionFactory connectionFactory) {
        this.rabbitTemplate = createNoTxRabbitTemplate(connectionFactory);
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
        this.prefix = prefix;
        this.dlq = prefix + ":dlq";
        this.deadletterExchange = deadletterExchange;
        declareDLQ();
    }

    public RetryOnQueueMessageRecovery retry(final int retryTime, final int maxRetry) {
        return retry(retryTime, maxRetry, null);
    }

    /**
     * Adiciona uma fila para retentativa.
     *
     * @param retryTime
     * @param maxRetry
     * @return
     */
    public RetryOnQueueMessageRecovery retry(final int retryTime, final int maxRetry, final Throwable cause) {
        final String queueName = prefix + ":retry:" + retryQueues.size();
        final RetryQueue retryQueue = new RetryQueue(queueName, retryTime, maxRetry, cause);
        retryQueues.add(retryQueue);
        declareRetryQueue(retryQueue);
        return this;
    }

    @Override
    public void recover(final Message message, final Throwable cause) {
        final Retry retry = new Retry(message, cause);

        final Optional<RetryQueue> retryQueueOptional = retryQueues.stream()
                .filter(q -> q.accept(retry)).findFirst();

        if (retryQueueOptional.isPresent()) {
            sendToRetry(retry, retryQueueOptional.get());
        } else {
            sendToDLQ(retry);
        }
    }

    private void declareDLQ() {
        final Map<String, Object> args = new LinkedHashMap<>();
        final Queue queue = new Queue(dlq, true, false, false, args);
        rabbitAdmin.declareQueue(queue);

        final FanoutExchange dlqExchange = new FanoutExchange(queue.getName());
        rabbitAdmin.declareExchange(dlqExchange);
        final Binding binding = BindingBuilder.bind(queue).to(dlqExchange);
        rabbitAdmin.declareBinding(binding);
    }

    private void declareRetryQueue(final RetryQueue retryQueue) {
        final Map<String, Object> args = new LinkedHashMap<>();
        args.put("x-message-ttl", retryQueue.retryTime);
        args.put("x-dead-letter-exchange", deadletterExchange);
        final Queue queue = new Queue(retryQueue.name, true, false, false, args);

        rabbitAdmin.declareQueue(queue);
        final FanoutExchange retryQueueExchange = new FanoutExchange(queue.getName());
        rabbitAdmin.declareExchange(retryQueueExchange);
        final Binding binding = BindingBuilder.bind(queue).to(retryQueueExchange);
        rabbitAdmin.declareBinding(binding);
    }

    private void sendToDLQ(final Retry retry) {
        new RepublishMessageRecoverer(rabbitTemplate, dlq, retry.getOriginalRoutingKey())
                .recover(retry.message, retry.cause);
    }

    private void sendToRetry(final Retry retry, final RetryQueue retryQueue) {
        retry.update(retryQueue);

        new RepublishMessageRecoverer(rabbitTemplate, retryQueue.name, retry.getOriginalRoutingKey())
                .recover(retry.message, retry.cause);
   }

    protected RabbitTemplate createNoTxRabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setChannelTransacted(false);
        return rabbitTemplate;
    }

    private static class RetryQueue {
        private final Integer maxRetry;
        private final Integer retryTime;
        private final Throwable cause;
        private final String name;

        public RetryQueue(final String name, final Integer retryTime, final Integer maxRetry, final Throwable cause) {
            this.name = name;
            this.maxRetry = maxRetry;
            this.retryTime = retryTime;
            this.cause = cause;
        }

        public boolean accept(final Retry retry) {
            final Integer counter = retry.getCounter(this);
            if (cause != null) {
                return retry.cause.getClass().isInstance(cause) && counter <= this.maxRetry;
            } else {
                return counter <= this.maxRetry;
            }
        }
    }

    private class Retry {
        public static final String RETRY_COUNTER = ".retry.counter";
        private final Message message;
        private final Throwable cause;
        private final Map<String, Object> headers;

        public Retry(final Message message, final Throwable cause) {
            this.message = message;
            this.headers = message.getMessageProperties().getHeaders();
            this.cause = cause;
        }

        public void update(final RetryQueue retryQueue) {
            Integer counter = getCounter(retryQueue);
            counter++;
            headers.put(retryQueue.name + RETRY_COUNTER, counter);
        }

        private Integer getCounter(final RetryQueue retryQueue) {
            Integer counter = 1;
            if (headers.containsKey(retryQueue.name + RETRY_COUNTER)) {
                counter = (Integer) headers.get(retryQueue.name + RETRY_COUNTER);
            }
            return counter;
        }

        public String getOriginalRoutingKey() {
            return message.getMessageProperties().getReceivedRoutingKey();
        }
    }
}
