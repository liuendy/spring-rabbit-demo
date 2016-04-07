package demorabbit.demos.demoapp.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by mmoraes on 2016-01-07.
 */
public class OrderInfo {

    private final Integer orderId;

    private final Integer checkoutId;

    private final OrderStatus status;

    private final String description;

    @JsonCreator
    public OrderInfo(@JsonProperty("orderId") final Integer orderId,
            @JsonProperty("checkoutCode") final Integer checkoutCode,
            @JsonProperty("status") final OrderStatus status,
            @JsonProperty("description") final String description) {
        this.orderId = orderId;
        this.checkoutId = checkoutCode;
        this.status = status;
        // to simulate a more real object
        this.description = description;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public Integer getCheckoutId() {
        return checkoutId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()//
                .append("OrderInfo [")//
                .append("orderId=")//
                .append(orderId)//
                .append(",checkoutId=")//
                .append(checkoutId)//
                .append(",status=")//
                .append(status)//
                .append("]");
        return builder.toString();
    }
}
