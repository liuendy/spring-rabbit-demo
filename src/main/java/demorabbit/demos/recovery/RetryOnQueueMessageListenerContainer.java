package demorabbit.demos.recovery;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Created by mmoraes on 19/12/15.
 */
public class RetryOnQueueMessageListenerContainer extends SimpleMessageListenerContainer {
    private final String retryQueuePrefix;
    private final String deadletterExchange;

    public RetryOnQueueMessageListenerContainer(final String retryQueuePrefix, final String deadletterExchange) {
        this.retryQueuePrefix = retryQueuePrefix;
        this.deadletterExchange = deadletterExchange;
    }
    @Override
    public void start() {
        DefaultRetryQueue shortRetry = DefaultRetryQueue
                .withName("demorabbit.app:retry:short")
                .exchange(deadletterExchange)
                .retryTime(10000)
                .maxRetry(1);
        DefaultRetryQueue longRetry = DefaultRetryQueue
                .withName("demorabbit.app:retry:long")
                .exchange(deadletterExchange)
                .retryTime(15000)
                .maxRetry(1);
        DefaultRetryQueue npeRetry = DefaultRetryQueue
                .withName("demorabbit.app:retry:npe")
                .causedBy(NullPointerException.class);
        DefaultRetryQueue dlq = DefaultRetryQueue
                .withName("demorabbit.app:retry:dlq");

        final RetryOnQueueMessageRecovery messageRecovery =
                new RetryOnQueueMessageRecovery(getConnectionFactory(), npeRetry, shortRetry, longRetry, dlq);

        final RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder
                .stateless()
                .maxAttempts(1)
                .recoverer(messageRecovery).build();

        setAdviceChain(new Advice[] {retryInterceptor});
        super.start();
    }
}
