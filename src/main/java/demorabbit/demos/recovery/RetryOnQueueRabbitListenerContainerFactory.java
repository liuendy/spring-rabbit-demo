package demorabbit.demos.recovery;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Custom RabbitListenerContainerFactory configurated with RetryDLQMessage message recovery
 */
@Component
public class RetryOnQueueRabbitListenerContainerFactory extends SimpleRabbitListenerContainerFactory {

    @Value("${demorabbit.directExchange}")
    private String exchange;

    @Override
    protected SimpleMessageListenerContainer createContainerInstance() {
        return  new RetryOnQueueMessageListenerContainer("demorabbit.app", exchange);
    }
}
