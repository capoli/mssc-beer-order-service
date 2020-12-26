package guru.sfg.brewery.model.events;

import guru.sfg.brewery.model.BeerOrderDto;
import lombok.*;

import java.io.Serializable;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "Builder")
public class ValidateOrderRequest implements Serializable {
    static final long serialVersionUID = -7392220653968861877L;

    @NonNull
    private BeerOrderDto beerOrder;
}
