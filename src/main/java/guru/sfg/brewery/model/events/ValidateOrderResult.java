package guru.sfg.brewery.model.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
public class ValidateOrderResult {
    private UUID orderId;
    private Boolean isValid;
}
