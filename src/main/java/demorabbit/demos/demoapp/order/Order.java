package demorabbit.demos.demoapp.order;

import java.util.Date;

import javax.persistence.*;

/**
 * Created by mmoraes on 2016-01-07.
 */
@Entity
@Table(name="orders")
public class Order {
    @Id
    @GeneratedValue
    private Integer id;

    private Integer checkoutRef;

    private OrderStatus status;

    private Date startDate;

    private Date updateDate;

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public Order setId(final Integer id) {
        this.id = id;
        return this;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Order setStatus(final OrderStatus status) {
        this.status = status;
        return this;
    }

    public Integer getCheckoutRef() {
        return checkoutRef;
    }

    public Order setCheckoutRef(final Integer checkoutRef) {
        this.checkoutRef = checkoutRef;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Order setStartDate(final Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public Order setUpdateDate(final Date updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public static Order create(final Integer checkoutRef) {
        final Order order = new Order();
        order.status = OrderStatus.CREATED;
        order.startDate = new Date();
        order.checkoutRef = checkoutRef;
        return order;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateDate = new Date();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()//
                .append("Order [")//
                .append("id=")//
                .append(id)//
                .append(",checkoutRef=")//
                .append(checkoutRef)//
                .append(",status=")//
                .append(status)//
                .append(",startDate=")//
                .append(startDate)//
                .append(",updateDate=")//
                .append(updateDate)//
                .append("]");
        return builder.toString();
    }
}
