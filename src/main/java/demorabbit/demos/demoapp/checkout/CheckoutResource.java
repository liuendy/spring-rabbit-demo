package demorabbit.demos.demoapp.checkout;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mmoraes on 2016-01-11.
 */
@RestController
public class CheckoutResource {

    @Autowired
    private CheckoutService checkoutService;

    @RequestMapping("/checkout")
    public CheckoutInfo checkout() {
        return checkoutService.checkout();
    }
}
