package demorabbit.demos.recovery;

import org.springframework.amqp.core.Message;

import java.util.Map;

/**
 * Created by mmoraes on 06/04/16.
 */
public class RetryAttempt {

    public static final String RETRY_COUNTER = ".retry.counter";
    private final Message message;
    private final Throwable cause;
    private final Map<String, Object> headers;

    RetryAttempt(final Message message, final Throwable cause) {
        this.message = message;
        this.headers = message.getMessageProperties().getHeaders();
        this.cause = cause;
    }


    void update(final RetryQueue retryQueue) {
        Integer counter = getCounter(retryQueue);
        counter++;
        headers.put(retryQueue.getName() + RETRY_COUNTER, counter);
    }

    public Throwable getCause() {
        return cause;
    }

    public Integer getCounter(final RetryQueue retryQueue) {
        Integer counter = 1;
        if (headers.containsKey(retryQueue.getName() + RETRY_COUNTER)) {
            counter = (Integer) headers.get(retryQueue.getName() + RETRY_COUNTER);
        }
        return counter;
    }

    public String getOriginalRoutingKey() {
        return message.getMessageProperties().getReceivedRoutingKey();
    }

    public Message getMessage() {
        return message;
    }
}
