package demorabbit.demos.recovery;

/**
 * Created at 19/12/15
 *
 * Configuration class for RetryAndDLQ message recovery
 */
public class RetryAndDLQConfig {
    public static final RetryAndDLQConfig DEFAULT = new RetryAndDLQConfig();
    private int retryTime = 5000;
    private float retryMultiplier = 1;
    private int maxRetry = 2;
    private Integer maxPriority;

    public int getRetryTime() {
        return retryTime;
    }

    public RetryAndDLQConfig setRetryTime(final int retryTime) {
        this.retryTime = retryTime;
        return this;
    }

    public float getRetryMultiplier() {
        return retryMultiplier;
    }

    public RetryAndDLQConfig setRetryMultiplier(final float retryMultiplier) {
        this.retryMultiplier = retryMultiplier;
        return this;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public RetryAndDLQConfig setMaxRetry(final int maxRetry) {
        this.maxRetry = maxRetry;
        return this;
    }

    public Integer getMaxPriority() {
        return maxPriority;
    }

    public RetryAndDLQConfig setMaxPriority(final Integer maxPriority) {
        this.maxPriority = maxPriority;
        return this;
    }

    /**
     * @param message
     * @return true if the retry counter was not exhausted
     */
    public boolean canRetry(final RecoverableMessage message) {
        return (message.getCounter() + 1) < maxRetry;
    }
}
