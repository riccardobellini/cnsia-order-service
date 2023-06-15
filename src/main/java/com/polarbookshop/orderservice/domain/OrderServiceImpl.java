package com.polarbookshop.orderservice.domain;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Flux<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    public Mono<Order> submit(String isbn, int quantity) {
        return Mono.just(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save);
    }

    private static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }


}
