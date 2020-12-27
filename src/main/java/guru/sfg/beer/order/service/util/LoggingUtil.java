package guru.sfg.beer.order.service.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 27/12/2020
 **/
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtil {
    public static Runnable logError(UUID orderId) {
        return () -> log.error("Order not found for beerOrderId: {}", orderId);
    }
}
