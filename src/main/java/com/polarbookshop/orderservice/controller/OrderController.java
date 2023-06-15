package com.polarbookshop.orderservice.controller;

import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderService;
import com.polarbookshop.orderservice.dto.OrderRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Flux<Order> getAll() {
        return orderService.getAll();
    }

    @PostMapping
    public Mono<Order> create(@RequestBody @Valid OrderRequest orderRequest) {
        return orderService.submit(orderRequest.isbn(), orderRequest.quantity());
    }
}
