package demorabbit.demos.recovery;

import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Custom RabbitListenerContainerFactory configurated with RetryDLQMessage message recovery
 */
@Component
public class RetryDLQRabbitListenerContainerFactory extends SimpleRabbitListenerContainerFactory {
    @Override
    protected SimpleMessageListenerContainer createContainerInstance() {
        return  new RetryDLQMessageListenerContainer();
    }
}
