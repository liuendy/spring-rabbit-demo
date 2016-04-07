package demorabbit.demos.recovery;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mmoraes on 06/04/16.
 */
public class DefaultRetryQueue implements RetryQueue {

    private String name;
    private String exchange;
    private Class<? extends Exception> cause;
    private Integer maxAttempt;
    private Integer retryTime;



    public DefaultRetryQueue(String name, String exchange) {
        this.name = name;
        this.exchange = exchange;
    }

    public DefaultRetryQueue(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean accept(RetryAttempt retry) {
        boolean acceptCause = cause != null ? cause.isInstance(retry.getCause()) : true;
        boolean acceptCounter = maxAttempt != null ? retry.getCounter(this) <= maxAttempt : true;
        return acceptCause && acceptCounter;
    }

    @Override
    public Map<String, Object> getArgs() {
        final Map<String, Object> args = new LinkedHashMap<>();
        if (retryTime != null) {
            args.put("x-message-ttl", retryTime);
        }
        if (exchange != null) {
            args.put("x-dead-letter-exchange", exchange);
        }
        return args;
    }

    public static DefaultRetryQueue withName(final String name) {
        return new DefaultRetryQueue(name);
    }

    public DefaultRetryQueue exchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public DefaultRetryQueue retryTime(int retryTime) {
        this.retryTime = retryTime;
        return this;
    }

    public DefaultRetryQueue maxRetry(int maxAttempt) {
        this.maxAttempt = maxAttempt;
        return this;
    }

    public DefaultRetryQueue causedBy(Class<? extends Exception> cause) {
        this.cause = cause;
        return this;
    }
}
