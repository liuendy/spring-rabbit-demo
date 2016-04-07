package demorabbit.demos.recovery;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mmoraes on 06/04/16.
 */
public class DefaultRetryQueue implements RetryQueue {

    private final String name;
    private String exchange;
    private Class<? extends Exception> cause;
    private Integer maxRetry;
    private Integer retryTime;



    public DefaultRetryQueue(final String name, final String exchange) {
        this.name = name;
        this.exchange = exchange;
    }

    public DefaultRetryQueue(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean accept(final RetryAttempt retry) {
        final boolean acceptCause = cause == null || cause.isInstance(retry.getCause());
        final boolean acceptCounter = maxRetry == null || retry.getCounter(this) <= maxRetry;
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

    public DefaultRetryQueue exchange(final String exchange) {
        this.exchange = exchange;
        return this;
    }

    public DefaultRetryQueue retryTime(final int retryTime) {
        this.retryTime = retryTime;
        return this;
    }

    public DefaultRetryQueue maxRetry(final int maxRetry) {
        this.maxRetry = maxRetry;
        return this;
    }

    public DefaultRetryQueue causedBy(final Class<? extends Exception> cause) {
        this.cause = cause;
        return this;
    }
}
