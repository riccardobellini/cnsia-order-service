package com.polarbookshop.orderservice.service;

import com.polarbookshop.orderservice.domain.OrderService;
import com.polarbookshop.orderservice.dto.event.OrderDispatchedMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class OrderFunctions {

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(OrderService orderService) {
        return flux -> orderService.consumeOrderDispatchedEvent(flux)
                .doOnNext(order -> log.info("Order dispatched. [order-id={}]", order.id()))
                .subscribe();
    }
}
