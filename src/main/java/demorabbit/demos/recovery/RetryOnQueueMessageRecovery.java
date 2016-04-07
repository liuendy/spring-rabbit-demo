package demorabbit.demos.recovery;

import java.util.*;

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

    private List<RetryQueue> retryQueues = new LinkedList<>();

    private final RabbitTemplate rabbitTemplate;
    private final RabbitAdmin rabbitAdmin;

    public RetryOnQueueMessageRecovery(final ConnectionFactory connectionFactory, RetryQueue... retryQueues) {
        this.rabbitTemplate = createNoTxRabbitTemplate(connectionFactory);
        this.rabbitAdmin = new RabbitAdmin(connectionFactory);
        this.retryQueues = Arrays.asList(retryQueues);
        this.retryQueues.forEach(q -> declareRetryQueue(q));
    }


    @Override
    public void recover(final Message message, final Throwable wrappeedCause) {
        final RetryAttempt retry = new RetryAttempt(message, wrappeedCause.getCause());

        final Optional<RetryQueue> retryQueueOptional = retryQueues.stream()
                .filter(q -> q.accept(retry)).findFirst();

        if (retryQueueOptional.isPresent()) {
            sendToRetry(retry, retryQueueOptional.get());
        }
    }

    private void declareRetryQueue(final RetryQueue retryQueue) {
        final Map<String, Object> args = retryQueue.getArgs();
        final Queue queue = new Queue(retryQueue.getName(), true, false, false, args);

        rabbitAdmin.declareQueue(queue);
        final FanoutExchange retryQueueExchange = new FanoutExchange(queue.getName());
        rabbitAdmin.declareExchange(retryQueueExchange);
        final Binding binding = BindingBuilder.bind(queue).to(retryQueueExchange);
        rabbitAdmin.declareBinding(binding);
    }

    private void sendToRetry(final RetryAttempt retry, final RetryQueue retryQueue) {
        retry.update(retryQueue);

        new RepublishMessageRecoverer(rabbitTemplate, retryQueue.getName(), retry.getOriginalRoutingKey())
                .recover(retry.getMessage(), retry.getCause());
   }

    protected RabbitTemplate createNoTxRabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setChannelTransacted(false);
        return rabbitTemplate;
    }
}
