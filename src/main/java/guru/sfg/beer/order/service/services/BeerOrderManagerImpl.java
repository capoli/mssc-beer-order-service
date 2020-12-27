package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderEventEnum;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.statemachine.BeerOrderStateChangeInterceptor;
import guru.sfg.brewery.model.BeerOrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static guru.sfg.beer.order.service.util.LoggingUtil.logError;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BeerOrderManagerImpl implements BeerOrderManager {
    public static final String BEER_ORDER_ID_HEADER = "beer_order_id";

    private final StateMachineFactory<BeerOrderStatusEnum, BeerOrderEventEnum> stateMachineFactory;
    private final BeerOrderStateChangeInterceptor beerOrderStateChangeInterceptor;
    private final BeerOrderRepository beerOrderRepository;

    @Override
    public BeerOrder newBeerOrder(BeerOrder beerOrder) {
        log.debug("Create new beer order {}", beerOrder);
        beerOrder.setId(null);
        beerOrder.setOrderStatus(BeerOrderStatusEnum.NEW);

        BeerOrder savedBeerOrder = beerOrderRepository.saveAndFlush(beerOrder);
        sendBeerOrderEvent(savedBeerOrder, BeerOrderEventEnum.VALIDATE_ORDER);
        return savedBeerOrder;
    }

    @Override
    public void processValidationResult(UUID orderId, Boolean isValid) {
        log.debug("Process validation result for beerOrderId: {} Valid? {}", orderId, isValid);

        beerOrderRepository.findById(orderId).ifPresentOrElse(beerOrder -> {
            if (isValid) {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_PASSED);

                //Get order again, after event is sent, BeerOrderStateChangeInterceptor will change and persist order to db
                //And trigger next event
                BeerOrder validatedOrder = beerOrderRepository.findById(orderId).get();
                sendBeerOrderEvent(validatedOrder, BeerOrderEventEnum.ALLOCATED_ORDER);
            } else {
                sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.VALIDATION_FAILED);
            }
        }, logError(orderId));
    }

    @Override
    public void beerOrderAllocationPassed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_SUCCESS);
            updateAllocatedQty(beerOrderDto);
        }, logError(beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationPendingInventory(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(beerOrder -> {
            sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_NO_INVENTORY);
            updateAllocatedQty(beerOrderDto);
        }, logError(beerOrderDto.getId()));
    }

    @Override
    public void beerOrderAllocationFailed(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(
                beerOrder -> sendBeerOrderEvent(beerOrder, BeerOrderEventEnum.ALLOCATION_FAILED),
                logError(beerOrderDto.getId()));
    }

    private void updateAllocatedQty(BeerOrderDto beerOrderDto) {
        beerOrderRepository.findById(beerOrderDto.getId()).ifPresentOrElse(allocatedOrder -> {
            allocatedOrder.getBeerOrderLines().forEach(beerOrderLine ->
                    beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
                        if (beerOrderLine.getId().equals(beerOrderLineDto.getId())) {
                            beerOrderLine.setQuantityAllocated(beerOrderLineDto.getQuantityAllocated());
                        }
                    }));

            beerOrderRepository.saveAndFlush(allocatedOrder);
        }, logError(beerOrderDto.getId()));
    }

    private void sendBeerOrderEvent(BeerOrder beerOrder, BeerOrderEventEnum eventEnum) {
        var stateMachine = build(beerOrder);
        var msg = MessageBuilder.withPayload(eventEnum)
                .setHeader(BEER_ORDER_ID_HEADER, beerOrder.getId().toString())
                .build();
        stateMachine.sendEvent(msg);
    }

    private StateMachine<BeerOrderStatusEnum, BeerOrderEventEnum> build(BeerOrder beerOrder) {
        var stateMachine = stateMachineFactory.getStateMachine(beerOrder.getId());
        stateMachine.stop();
        stateMachine.getStateMachineAccessor()
                .doWithAllRegions(sma -> {
                    sma.addStateMachineInterceptor(beerOrderStateChangeInterceptor);
                    sma.resetStateMachine(new DefaultStateMachineContext<>(beerOrder.getOrderStatus(), null, null, null));
                });
        stateMachine.start();
        return stateMachine;
    }
}
