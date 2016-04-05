package demorabbit.demos.recovery;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Created by mmoraes on 19/12/15.
 */
public class RetryDLQMessageListenerContainer extends SimpleMessageListenerContainer {
    @Override
    public void start() {
        /*
         * Configure the RetryOperationInterceptor to use the custom RetryAndDLQMessageRecovery
         */
//        final RetryAndDLQMessageRecovery messageRecovery = new RetryAndDLQMessageRecovery(getConnectionFactory(),
//                getQueueNames());
//
//        messageRecovery.config()
//                .setMaxRetry(3)
//                .setRetryMultiplier(1.1f);

        final RetryOnQueueMessageRecovery messageRecovery = new RetryOnQueueMessageRecovery("test", getConnectionFactory());

        messageRecovery.retry(50000, 5);

        final RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder
                .stateless()
                .maxAttempts(1)
                .recoverer(messageRecovery).build();
        setAdviceChain(new Advice[] {retryInterceptor});
        super.start();
    }
}
