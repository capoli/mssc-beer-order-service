package guru.sfg.beer.order.service.domain;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
public enum BeerOrderEventEnum {
    VALIDATE_ORDER,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    ALLOCATED_ORDER,
    ALLOCATION_SUCCESS,
    ALLOCATION_FAILED,
    ALLOCATION_NO_INVENTORY,
    BEER_ORDER_PICKED_UP,
//    BEER_ORDER_DELIVERED,
//    BEER_ORDER_FAILED
}
