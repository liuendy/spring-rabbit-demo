package demorabbit.demos.recovery;

import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

/**
 * Created at 19/12/15
 *
 * Wrapper class for {@link Message} object to handle RetryAndDLQ operations
 */
public class RecoverableMessage {
    // header constants
    public static final String RETRY_TIME = "retryDlq.retryTime";
    public static final String RETRY_COUNTER = "retryDlq.counter";
    public static final String RETRY_ORIGINAL_PRIORITY = "retry.originalPriority";
    // target message
    private final Message message;
    // headers
    private final Map<String, Object> headers;

    /**
     * @param message target message
     * @param initialRetryTime initial retry time
     */
    public RecoverableMessage(final Message message, final int initialRetryTime) {
        this.message = message;
        this.headers = getProperties().getHeaders();

        initHeaders(initialRetryTime);
    }

    /**
     * Initialize headers variables
     *
     * @param initialRetryTime
     */
    private void initHeaders(final int initialRetryTime) {
        if (notContainsHeader(RETRY_COUNTER)) {
            headers.put(RETRY_COUNTER, 0);
        }
        if (notContainsHeader(RETRY_TIME)) {
            headers.put(RETRY_TIME, initialRetryTime);
        }
        if (notContainsHeader(RETRY_ORIGINAL_PRIORITY)) {
            headers.put(RETRY_ORIGINAL_PRIORITY, message.getMessageProperties().getPriority());
        }
    }

    /**
     * @return target message
     */
    public Message getValue() {
        return message;
    }

    /**
     * increase retry counter
     */
    public void increaseCounter() {
        headers.put(RETRY_COUNTER, getCounter() + 1);
    }

    /**
     * increase priority
     */
    public void increasePriority(final Integer maxPriority) {
        if (allowIncreasePriority(maxPriority)) {
            getProperties().setPriority(getPriority() + maxPriority);
        }
    }

    /**
     * set expiration
     */
    public void setExpiration(final Integer expiration) {
        getProperties().setExpiration(String.valueOf(expiration));
    }

    /**
     * @return the consumer queue
     */
    public String getTargetQueue() {
        return getProperties().getConsumerQueue();
    }

    /**
     * @return retry counter
     */
    public Integer getCounter() {
        return (Integer) getProperties().getHeaders().get(RETRY_COUNTER);
    }

    /**
     * @return true if configuration allow increate priority
     */
    public boolean allowIncreasePriority(final Integer maxPriority) {
        return maxPriority != null && (getPriority() + 1) < maxPriority;
    }

    /**
     * @return message priority
     */
    public Integer getPriority() {
        Integer priority = getProperties().getPriority();
        priority = (priority != null) ? priority + 1 : 0;
        return priority;
    }

    /**
     * increase message expiration
     * @param factor
     */
    public void increaseExpiration(final float factor) {
        final Integer current = (Integer) headers.get(RETRY_TIME);
        setExpiration((int) (current * factor));
    }

    /**
     * clear counter
     */
    public void clearCounter() {
        headers.remove(RETRY_COUNTER);
    }

    /**
     * clear expiration
     */
    public void clearExpiration() {
        headers.remove(RETRY_TIME);
    }

    /**
     * restore the orignal message priority
     */
    public void restorePriority() {
        final Integer originalPriority = (Integer) headers.get(RETRY_ORIGINAL_PRIORITY);
        getProperties().setPriority(originalPriority);
    }

    private boolean notContainsHeader(final String key) {
        return !headers.containsKey(key);
    }

    private MessageProperties getProperties() {
        return message.getMessageProperties();
    }
}
