package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class BeerOrderAllocationListener {
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(Message<AllocateOrderRequest> msg) {
        BeerOrderDto beerOrder = msg.getPayload().getBeerOrder();

        //fully allocate
        beerOrder.getBeerOrderLines().forEach(beerOrderLineDto ->
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity()));

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE, AllocateOrderResult.builder()
                .beerOrder(beerOrder)
                .allocationError(false)
                .pendingInventory(false)
                .build());
    }
}
