package demorabbit.demos.demoapp.checkout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by mmoraes on 2016-01-07.
 */
@Repository
public interface CheckoutRepository extends JpaRepository<Checkout, Integer> {

}
