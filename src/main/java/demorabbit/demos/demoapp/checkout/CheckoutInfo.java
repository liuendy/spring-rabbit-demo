package demorabbit.demos.demoapp.checkout;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by mmoraes on 2016-01-07.
 */
public class CheckoutInfo {

    private final Integer id;

    private final CheckoutStatus status;

    @JsonCreator
    public CheckoutInfo(@JsonProperty("id") final Integer id,
                        @JsonProperty("status")final CheckoutStatus status) {
        this.id = id;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public CheckoutStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()//
                .append("CheckoutInfo [")//
                .append("status=")//
                .append(status)//
                .append(",id=")//
                .append(id)//
                .append("]");
        return builder.toString();
    }
}
