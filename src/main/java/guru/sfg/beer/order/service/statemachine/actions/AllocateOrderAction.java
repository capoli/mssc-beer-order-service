package guru.sfg.beer.order.service.statemachine.actions;

import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.util.LoggingUtil;
import guru.sfg.beer.order.service.web.mappers.BeerOrderMapper;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static guru.sfg.beer.order.service.config.JmsConfig.ALLOCATE_ORDER_QUEUE;
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
public class AllocateOrderAction implements Action<BeerOrderStatusEnum, BeerOrderEventEnum> {
    private final JmsTemplate jmsTemplate;
    private final BeerOrderRepository beerOrderRepository;
    private final BeerOrderMapper beerOrderMapper;

    @Override
    public void execute(StateContext<BeerOrderStatusEnum, BeerOrderEventEnum> stateContext) {
        var orderIdHeader = (String) stateContext.getMessageHeader(BEER_ORDER_ID_HEADER);
        UUID orderId = UUID.fromString(orderIdHeader);
        beerOrderRepository.findById(orderId).ifPresentOrElse(beerOrder -> {
            var allocateOrderRequest = AllocateOrderRequest.builder()
                    .beerOrder(beerOrderMapper.beerOrderToDto(beerOrder))
                    .build();
            jmsTemplate.convertAndSend(ALLOCATE_ORDER_QUEUE, allocateOrderRequest);
            log.debug("Sent Allocation request on QUEUE {} with order id {} and payload {}", ALLOCATE_ORDER_QUEUE, orderIdHeader, allocateOrderRequest);
        }, LoggingUtil.logError(orderId));
    }
}
