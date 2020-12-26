package guru.sfg.beer.order.service.services;

import guru.sfg.beer.order.service.domain.BeerOrder;

/**
 * @author Olivier Cappelle
 * @version x.x.x
 * @see
 * @since x.x.x 26/12/2020
 **/
public interface BeerOrderManager {

    BeerOrder newBeerOrder(BeerOrder beerOrder);
}
