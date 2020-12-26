package guru.sfg.beer.order.service.util;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.UUID;

import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.BEER_ORDER_ID_HEADER;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MessageUtil {
    public static Message<BeerOrderEventEnum> buildMessage(UUID orderId, BeerOrderEventEnum eventEnum) {
        return MessageBuilder.withPayload(BeerOrderEventEnum.VALIDATION_FAILED)
                .setHeader(BEER_ORDER_ID_HEADER, orderId)
                .build();
    }
}
