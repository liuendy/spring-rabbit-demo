package demorabbit.demos.demoapp.conf;

import javax.persistence.EntityManagerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import demorabbit.demos.demoapp.conf.Queues;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Created by mmoraes on 12/12/15.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RabbitConfig {

    @Autowired
    private Queues queues;

    /**
     * Configure RabbitConnectionFactory
     */
    @Bean
    ConnectionFactory amqpConnectionFactory() {
        final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses("localhost:5672");
        // set to use channel pool
        connectionFactory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        // channel pool size
        connectionFactory.setChannelCacheSize(10);
        return connectionFactory;
    }

    /**
     * Create RabbitAdmin instance
     */
    @Bean
    RabbitAdmin rabbitAdmin() {
        return new RabbitAdmin(amqpConnectionFactory());
    }

    @Bean
    RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(amqpConnectionFactory());
        rabbitTemplate.setChannelTransacted(false);
        // set JSON Message Converter
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Create a Transactional RabbitTemplate
     */
    @Bean
    RabbitTemplate txRabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(amqpConnectionFactory());
        rabbitTemplate.setChannelTransacted(true);
        // set JSON Message Converter
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    RabbitTemplate noTxRabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(amqpConnectionFactory());
        rabbitTemplate.setChannelTransacted(false);
        // set JSON Message Converter
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
    /**
     * Configure JSON mapper used by JSON MessageConverter
     */
    @Bean
    ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper;
    }

    /**
     * Configure JPATransactionManager
     */
    @Bean
    PlatformTransactionManager transactionManager(final EntityManagerFactory entityManagerFactory) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

    @Bean()
    DirectExchange demorabbitExchange() {
        return new DirectExchange(queues.exchange(), true, false);
    }

}
