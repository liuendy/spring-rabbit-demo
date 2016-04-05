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
        final RetryOnQueueMessageRecovery messageRecovery =
                new RetryOnQueueMessageRecovery(retryQueuePrefix, deadletterExchange, getConnectionFactory());

        messageRecovery.retry(5000, 2);
        messageRecovery.retry(10000, 2);

        final RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder
                .stateless()
                .maxAttempts(1)
                .recoverer(messageRecovery).build();
        setAdviceChain(new Advice[] {retryInterceptor});
        super.start();
    }
}
