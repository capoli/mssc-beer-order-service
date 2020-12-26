package guru.sfg.beer.order.service.statemachine.actions;

import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.ValidateBeerOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.JmsConfig.VALIDATE_ORDER_QUEUE;
import static guru.sfg.beer.order.service.services.BeerOrderManagerImpl.BEER_ORDER_ID_HEADER;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderStatusEnum> {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderStatusEnum> stateContext) {
        var orderIdHeader = (String) stateContext.getMessageHeader(BEER_ORDER_ID_HEADER);
//        var orderIdHeader = (String) stateContext.getMessage().getHeaders().get(BEER_ORDER_ID_HEADER);
        var beerOrder = beerOrderRepository.getOne(UUID.fromString(orderIdHeader));

        var validateBeerOrderRequest = ValidateBeerOrderRequest.builder()
                .beerOrder(beerOrderMapper.beerOrderToDto(beerOrder))
                .build();
        jmsTemplate.convertAndSend(VALIDATE_ORDER_QUEUE, validateBeerOrderRequest);
        log.debug("Sent Validation request on QUEUE {} with order id {} and payload {}", VALIDATE_ORDER_QUEUE, orderIdHeader, validateBeerOrderRequest);
    }
}
