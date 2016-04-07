package demorabbit.demos.demoapp.order;

import demorabbit.demos.demoapp.checkout.CheckoutInfo;
import demorabbit.demos.demoapp.conf.Queues;
import demorabbit.demos.demoapp.conf.RoutingKeys;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mmoraes on 2016-01-07.
 */
@Service
public class OrderService {

    private static final Log LOG = LogFactory.getLog(OrderService.class);
    public static int delayToRespondeGetOrder = 200;

    public static int sizeToRespondeGetOrder = 2048;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private Queues queues;

    @Autowired
    private RoutingKeys routingKeys;

    @Autowired
    private RabbitAdmin rabbitAdmin;
    private static Boolean error;

    @RabbitListener(
            containerFactory = "txRabbitListenerContainerFactory",
            bindings = @QueueBinding(
                value = @org.springframework.amqp.rabbit.annotation.Queue(value="#{queues.order()}", durable = "true"),
                exchange = @Exchange(value = "#{queues.exchange()}", durable = "true"),
                key = "#{routingKeys.createOrder()}"
            )
    )
    @SendTo("demorabbit.app:orderEvents")
    @Transactional
    public OrderInfo create(final CheckoutInfo checkoutInfo) {

        final Order order = Order.create(checkoutInfo.getId());
        orderRepository.save(order);

        if (error) {
            throw new IllegalArgumentException();
        }

        LOG.info("** ORDER CREATED:" + order);
        final OrderInfo orderInfo = new OrderInfo(order.getId(), order.getCheckoutRef(), order.getStatus(),
                new String(new char[2048]).replace('\0', 'X'));

        return orderInfo;
    }

    @RabbitListener(
            containerFactory = "noTxRabbitListenerContainerFactory",
            bindings = @QueueBinding(
            value = @org.springframework.amqp.rabbit.annotation.Queue(value="#{queues.getOrder()}", durable = "true"),
                    exchange = @Exchange(value = "#{queues.exchange()}", durable = "true"),
            key = "#{routingKeys.getOrder()}"
        )
    )
    public OrderInfo getOrder(final Integer orderId) {
        return createOrderInfo(orderId);
    }

    @RabbitListener(
            containerFactory = "txRabbitListenerContainerFactory",
            bindings = @QueueBinding(
                    value = @org.springframework.amqp.rabbit.annotation.Queue(value="#{queues.getOrder() + 'Tx'}", durable = "true"),
                    exchange = @Exchange(value = "#{queues.exchange()}", durable = "true"),
                    key = "#{routingKeys.getOrder() + 'Tx'}"
            )
    )
    public OrderInfo getOrderTx(final Integer orderId) {
        return createOrderInfo(orderId);
    }

    private OrderInfo createOrderInfo(final Integer orderId) {
        final Order order = orderRepository.findOne(orderId);

        try {
            Thread.sleep(delayToRespondeGetOrder);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        final OrderInfo orderInfo = new OrderInfo(order.getId(), order.getCheckoutRef(), order.getStatus(),
                new String(new char[sizeToRespondeGetOrder]).replace('\0', 'X'));

        return orderInfo;
    }

}
