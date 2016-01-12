package demorabbit.demos.demoapp.conf;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by mmoraes on 2016-01-07.
 */
@Component
public class Queues {

    @Value("${demorabbit.queue.orderEvents}")
    private String orderEvents;

    @Value("${demorabbit.queue.order}")
    private String order;

    @Value("${demorabbit.queue.getOrder}")
    private String getOrder;

    @Value("${demorabbit.directExchange}")
    private String exchange;

    @Autowired
    private RabbitAdmin rabbitAdmin;


    public String orderEvents() {
        return orderEvents;
    }

    public String exchange() {
        return exchange;
    }

    public String order() {
        return order;
    }

    public String getOrder() {
        return getOrder;
    }
}
