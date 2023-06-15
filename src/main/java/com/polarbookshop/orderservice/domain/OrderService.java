package com.polarbookshop.orderservice.domain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {
    Flux<Order> getAll();

    Mono<Order> submit(String isbn, int quantity);
}
