package guru.sfg.beer.order.service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jenspiegsa.wiremockextension.WireMockExtension;
import com.github.tomakehurst.wiremock.WireMockServer;
import guru.sfg.beer.order.service.domain.BeerOrder;
import guru.sfg.beer.order.service.domain.BeerOrderLine;
import guru.sfg.beer.order.service.domain.BeerOrderStatusEnum;
import guru.sfg.beer.order.service.domain.Customer;
import guru.sfg.beer.order.service.repositories.BeerOrderRepository;
import guru.sfg.beer.order.service.repositories.CustomerRepository;
import guru.sfg.beer.order.service.services.beer.BeerServiceImpl;
import guru.sfg.brewery.model.BeerDto;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.jenspiegsa.wiremockextension.ManagedWireMockServer.with;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.awaitility.Awaitility.await;
import static org.jgroups.util.Util.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(WireMockExtension.class)
@SpringBootTest
class BeerOrderManagerImplIT {
    public final ConditionFactory await = await().atMost(20, TimeUnit.SECONDS);

    @Autowired
    BeerOrderManager beerOrderManager;

    @Autowired
    BeerOrderRepository beerOrderRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    WireMockServer wireMockServer;

    Customer testCustomer;

    UUID beerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testCustomer = customerRepository.save(Customer.builder()
                .customerName("Test Customer")
                .build());
    }

    @Test
    void testNewToAllocated() throws JsonProcessingException, InterruptedException {
        String upc = "12345";
        BeerDto beerDto = BeerDto.builder().id(beerId).upc(upc).build();
        BeerOrder beerOrder = createBeerOrder();

        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + upc)
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        waitUntilBeerHasStatus(savedBeerOrder, BeerOrderStatusEnum.ALLOCATED);

        await.untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(savedBeerOrder.getId()).get();
            BeerOrderLine line = foundOrder.getBeerOrderLines().iterator().next();
            assertEquals(line.getOrderQuantity(), line.getQuantityAllocated());
        });

        BeerOrder orderAfterWaitingToBeAllocated = beerOrderRepository.findById(savedBeerOrder.getId()).get();

        assertNotNull(orderAfterWaitingToBeAllocated);
        assertEquals(BeerOrderStatusEnum.ALLOCATED, orderAfterWaitingToBeAllocated.getOrderStatus());
        orderAfterWaitingToBeAllocated.getBeerOrderLines().forEach(line ->
                assertEquals(line.getOrderQuantity(), line.getQuantityAllocated()));
    }

    @Test
    public void testNewToPickedUp() throws JsonProcessingException {
        String upc = "12345";
        BeerDto beerDto = BeerDto.builder().id(beerId).upc(upc).build();
        BeerOrder beerOrder = createBeerOrder();

        wireMockServer.stubFor(get(BeerServiceImpl.BEER_UPC_PATH_V1 + upc)
                .willReturn(okJson(objectMapper.writeValueAsString(beerDto))));

        BeerOrder savedBeerOrder = beerOrderManager.newBeerOrder(beerOrder);

        waitUntilBeerHasStatus(savedBeerOrder, BeerOrderStatusEnum.ALLOCATED);

        beerOrderManager.beerOrderPickedUp(savedBeerOrder.getId());

        waitUntilBeerHasStatus(savedBeerOrder, BeerOrderStatusEnum.PICKED_UP);

        BeerOrder beerOrderAfterPickUp = beerOrderRepository.findById(savedBeerOrder.getId()).get();
        assertNotNull(beerOrderAfterPickUp);
        assertEquals(BeerOrderStatusEnum.PICKED_UP, beerOrderAfterPickUp.getOrderStatus());
        beerOrderAfterPickUp.getBeerOrderLines().forEach(line ->
                assertEquals(line.getOrderQuantity(), line.getQuantityAllocated()));
    }

    private void waitUntilBeerHasStatus(BeerOrder order, BeerOrderStatusEnum statusEnum) {
        await.untilAsserted(() -> {
            BeerOrder foundOrder = beerOrderRepository.findById(order.getId()).get();
            assertEquals(statusEnum, foundOrder.getOrderStatus());
        });
    }

    public BeerOrder createBeerOrder() {
        BeerOrder beerOrder = BeerOrder.builder()
                .customer(testCustomer)
                .build();

        Set<BeerOrderLine> lines = new HashSet<>();
        lines.add(BeerOrderLine.builder()
                .beerId(beerId)
                .upc("12345")
                .orderQuantity(1)
                .beerOrder(beerOrder)
                .build());

        beerOrder.setBeerOrderLines(lines);

        return beerOrder;
    }

    @TestConfiguration
    static class RestTemplateBuilderProvider {

        //destroy method is recommended by wiremock docs
        @Bean(destroyMethod = "stop")
        public WireMockServer wireMockServer() {
            WireMockServer server = with(wireMockConfig().port(8083));
            server.start();
            return server;
        }
    }
}