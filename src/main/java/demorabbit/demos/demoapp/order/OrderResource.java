package demorabbit.demos.demoapp.order;

import demorabbit.demos.demoapp.checkout.CheckoutInfo;
import demorabbit.demos.demoapp.checkout.CheckoutStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mmoraes on 2016-01-07.
 */
@RestController
public class OrderResource {

    @Autowired
    private OrderService orderService;

    @RequestMapping("/order/{id}")
    public OrderInfo getOrder(@PathVariable("id") final Integer id) {
        return orderService.getOrder(id);
    }

    @RequestMapping(value = "/createTest/{checkoutRef}", method = RequestMethod.POST)
    public OrderInfo createOrder(@PathVariable("checkoutRef") final Integer id) {
        final CheckoutInfo checkoutInfo = new CheckoutInfo(id, CheckoutStatus.CREATED);
        return orderService.create(checkoutInfo);
    }

}
