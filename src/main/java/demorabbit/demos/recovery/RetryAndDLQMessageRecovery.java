package demorabbit.demos.recovery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by mmoraes on 12/12/15.
 *
 * Custom Message Recovery to send the message to a RETRY queue or a DLQ queue.
 */
public class RetryAndDLQMessageRecovery implements MessageRecoverer {

    private static final Log LOG = LogFactory.getLog(RetryAndDLQMessageRecovery.class);

    // help constants
    public static final String DEFAULT_EXCHANGE = "retry-dlq-exchange";
    public static final String RETRY_SUFIX = ":retry";
    public static final String DLQ_SUFIX = ":dlq";
    public static final String X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    private final String retryDlqExchange = DEFAULT_EXCHANGE;
    private DirectExchange recoveryExchange;
    private final List<String> queueNames;

    @Autowired
    private final ConnectionFactory connectionFactory;

    private final RetryAndDLQConfig config = RetryAndDLQConfig.DEFAULT;

    private RabbitTemplate nonTransactionalRabbitTemplate;

    /**
     * @param connectionFactory to use
     * @param queueNames
     */
    public RetryAndDLQMessageRecovery(final ConnectionFactory connectionFactory, final String... queueNames) {
        this.connectionFactory = connectionFactory;
        this.queueNames = Arrays.asList(queueNames);
        init();
    }

    /**
     * Initialize
     */
    private void init() {
        // creates a non transactional rabbit template
        this.nonTransactionalRabbitTemplate = new RabbitTemplate(connectionFactory);
        this.nonTransactionalRabbitTemplate.setChannelTransacted(false);
        createRetryQueueAndBindings(queueNames);
    }

    /**
     * Send the message to Retry or to the DLQ depending the retry counter.
     *
     * @param message message
     *
     * @param cause  cause
     */
    @Override
    public void recover(final Message message, final Throwable cause) {
        final RecoverableMessage messageToRecovery = new RecoverableMessage(message, config.getRetryTime());
        final String targetQueue = messageToRecovery.getTargetQueue();
        final String rejectionMessage;

        if (config.canRetry(messageToRecovery)) {
            sendToRetryQueue(messageToRecovery, targetQueue, cause);
            rejectionMessage ="Sent to RETRY QUEUE:" + getRetryRoutingKey(targetQueue);
        } else {
            sendToDQL(messageToRecovery, targetQueue, cause);
            rejectionMessage = "Sent to DLQ QUEUE:" + getDlqRoutingKey(targetQueue);
        }
        // after redirect the message reject the message to remove from original queue
        throw new AmqpRejectAndDontRequeueException(rejectionMessage + ":" + targetQueue, cause);
    }

    public RetryAndDLQConfig config() {
        return config;
    }

    /**
     * Send the message to the DLQ
     * @param message
     * @param targetQueue
     * @param cause
     */
    private void sendToDQL(final RecoverableMessage message, final String targetQueue, final Throwable cause) {
        final String routingKey = getDlqRoutingKey(targetQueue);
        message.clearCounter();
        message.clearExpiration();
        message.restorePriority();
        // use the RepublishMessageRecoverer to send to add extra message headers
        new RepublishMessageRecoverer(nonTransactionalRabbitTemplate, recoveryExchange.getName(), routingKey)
                .recover(message.getValue(), cause);

    }

    private void sendToRetryQueue(final RecoverableMessage message, final String targetQueue, final Throwable cause) {
        final String routingKey = getRetryRoutingKey(targetQueue);
        message.increaseCounter();
        message.increaseExpiration(config.getRetryTime());
        message.increasePriority(config.getMaxPriority());

        new RepublishMessageRecoverer(nonTransactionalRabbitTemplate, recoveryExchange.getName(), routingKey)
                .recover(message.getValue(), cause);
    }

    /**
     * Based on the original queue create the DLQ routing key
     *
     * @param queueName
     *
     * @return DLQ routing key
     */
    private String getDlqRoutingKey(final String queueName) {
        return queueName+ DLQ_SUFIX;
    }

    /**
     * Based on the original queue create the RETRY routing key
     *
     * @param queueName
     *
     * @return RETRY routing key
     */
    private String getRetryRoutingKey(final String queueName) {
        return queueName + RETRY_SUFIX;
    }

    /**
     * Create Retry Queue and Bindings for each target queue
     *
     * @param queueNames queue name list
     */
    private void createRetryQueueAndBindings(final List<String> queueNames) {

        this.recoveryExchange = new DirectExchange(retryDlqExchange, true, false);

        final RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.declareExchange(recoveryExchange);

        queueNames.forEach(targetQueue -> {
            delareAndBindRetry(rabbitAdmin, targetQueue);
            final Queue dlqQueue = declareandBindDlq(rabbitAdmin, targetQueue);
            bindTargetQueueToRetry(rabbitAdmin, targetQueue, dlqQueue);
        });
    }

    /**
     * Bind the target queue with the recovery exchange to make the message come back to original queue after expiration
     *
     * @param rabbitAdmin
     * @param targetQueueName
     * @param dlqQueue
     */
    private void bindTargetQueueToRetry(final RabbitAdmin rabbitAdmin, final String targetQueueName, final Queue dlqQueue) {
        final Queue targetQueue = new Queue(targetQueueName);
        final Binding targetBinding = BindingBuilder.bind(targetQueue)
                .to(recoveryExchange).with(targetQueueName);
        rabbitAdmin.declareQueue(dlqQueue);
        rabbitAdmin.declareBinding(targetBinding);
    }

    /**
     * Declare and bind DLQ Queue
     *
     * @param rabbitAdmin
     * @param queueName
     * @return
     */
    private Queue declareandBindDlq(final RabbitAdmin rabbitAdmin, final String queueName) {
        final Queue dlqQueue = new Queue(getDlqRoutingKey(queueName), true);
        final Binding dlqBinding = BindingBuilder.bind(dlqQueue)
                .to(recoveryExchange).with(getDlqRoutingKey(queueName));
        rabbitAdmin.declareQueue(dlqQueue);
        rabbitAdmin.declareBinding(dlqBinding);
        return dlqQueue;
    }

    /**
     * Declare and bind Retry Queue
     *
     * @param rabbitAdmin
     * @param queueName
     */
    private void delareAndBindRetry(final RabbitAdmin rabbitAdmin, final String queueName) {
        final Map<String, Object> args = new HashMap<>();
        args.put(X_DEAD_LETTER_EXCHANGE, retryDlqExchange);
        args.put(X_DEAD_LETTER_ROUTING_KEY, queueName);
        final Queue retryQueue = new Queue(getRetryRoutingKey(queueName), true, false, false, args);
        final Binding retryBinding = BindingBuilder.bind(retryQueue)
                .to(recoveryExchange).with(getRetryRoutingKey(queueName));
        rabbitAdmin.declareQueue(retryQueue);
        rabbitAdmin.declareBinding(retryBinding);
    }
}
