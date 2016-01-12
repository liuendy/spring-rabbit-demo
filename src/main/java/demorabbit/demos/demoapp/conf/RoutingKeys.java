package demorabbit.demos.demoapp.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Created at 08/01/16
 */
@Order()
@Component
public class RoutingKeys {
    @Value("${demorabbit.routingKey.createOrder}")
    private String createOrder;
    @Value("${demorabbit.routingKey.orderCreated}")
    private String orderCreated;
    @Value("${demorabbit.routingKey.getOrder}")
    private String getOrder;

    public String createOrder() {
        return createOrder;
    }

    public String orderCreated() {
        return orderCreated;
    }

    public String getOrder() {
        return getOrder;
    }
}
