package demorabbit.demos.demoapp.checkout;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

/**
 * Created by mmoraes on 2016-01-07.
 */
@Entity
@Table(name = "carts")
public class Checkout {
    @Id
    @GeneratedValue
    private Integer id;

    private CheckoutStatus status;

    private Date startDate;

    private Date updateDate;

    public Integer getId() {
        return id;
    }

    public Checkout setId(final Integer id) {
        this.id = id;
        return this;
    }

    public CheckoutStatus getStatus() {
        return status;
    }

    public Checkout setStatus(final CheckoutStatus status) {
        this.status = status;
        return this;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Checkout setStartDate(final Date startDate) {
        this.startDate = startDate;
        return this;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public Checkout setUpdateDate(final Date updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public static Checkout create() {
        final Checkout checkout = new Checkout();
        checkout.status = CheckoutStatus.CREATED;
        checkout.startDate = new Date();
        return checkout;
    }

    public void orderCreated() {
        this.status = CheckoutStatus.ORDERED;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateDate = new Date();
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()//
                .append("Checkout [")//
                .append("id=")//
                .append(id)//
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
