package demorabbit.demos.demoapp.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by mmoraes on 2016-01-05.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

}
