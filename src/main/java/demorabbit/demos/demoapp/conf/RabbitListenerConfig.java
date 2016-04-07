package demorabbit.demos.demoapp.conf;

import demorabbit.demos.recovery.RetryOnQueueRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * Created by mmoraes on 12/12/15.
 */
@Configuration
public class RabbitListenerConfig implements RabbitListenerConfigurer {

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
        return noTxRabbitListenerContainerFactory();
    }

    /**
     * Configure the {@link org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory} to use
     * with RabbitListener annotations.
     */
    @Bean
    SimpleRabbitListenerContainerFactory txRabbitListenerContainerFactory() {
        final RetryOnQueueRabbitListenerContainerFactory factory = new RetryOnQueueRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(10);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setMaxConcurrentConsumers(10);
        factory.setChannelTransacted(true);

        return factory;
    }

    @Bean
    SimpleRabbitListenerContainerFactory noTxRabbitListenerContainerFactory() {
        final RetryOnQueueRabbitListenerContainerFactory factory = new RetryOnQueueRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(10);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setMaxConcurrentConsumers(10);
        factory.setChannelTransacted(false);

        return factory;
    }

    @Override
    public void configureRabbitListeners(final RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(customHandlerMethodFactory());
    }

    /**
     * Configure MessageHandlerMethodFactory to use JSON Message Converter
     */
    @Bean
    MessageHandlerMethodFactory customHandlerMethodFactory() {
        final DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        factory.setMessageConverter(new MappingJackson2MessageConverter());
        return factory;
    }

}
