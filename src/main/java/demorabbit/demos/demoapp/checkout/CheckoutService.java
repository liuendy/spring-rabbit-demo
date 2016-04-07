package demorabbit.demos.demoapp.checkout;

import demorabbit.demos.demoapp.conf.Queues;
import demorabbit.demos.demoapp.conf.RoutingKeys;
import demorabbit.demos.demoapp.order.OrderInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mmoraes on 2016-01-07.
 */
@Service
public class CheckoutService {

    private static final Log LOG = LogFactory.getLog(CheckoutService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CheckoutRepository checkoutRepository;

    @Autowired
    private Queues queues;

    @Autowired
    private RoutingKeys routingKeys;


    @Transactional
    public CheckoutInfo checkout() {
        final Checkout checkout = Checkout.create();
        checkoutRepository.save(checkout);
        final CheckoutInfo checkoutInfo = new CheckoutInfo(checkout.getId(), checkout.getStatus());
        rabbitTemplate.convertAndSend(queues.exchange(), routingKeys.createOrder(), checkoutInfo);
        LOG.info("** CHECKOUT SENT:" + checkout);
        return checkoutInfo;
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @org.springframework.amqp.rabbit.annotation.Queue(value = "#{queues.orderEvents()}", durable = "true"),
                    exchange = @Exchange(value = "#{queues.exchange()}", durable = "true"),
                    key = "#{routingKeys.orderCreated()}"
            )
    )
    void orderCreated(final OrderInfo orderInfo) {
        final Checkout checkout = checkoutRepository.findOne(orderInfo.getCheckoutId());
        checkout.orderCreated();
        LOG.info("** CHECKOUT ORDERED:" + checkout);
    }
}
