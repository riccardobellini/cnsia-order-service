package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.dto.book.Book;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BookClient bookClient;

    public OrderServiceImpl(OrderRepository orderRepository, BookClient bookClient) {
        this.orderRepository = orderRepository;
        this.bookClient = bookClient;
    }

    @Override
    public Flux<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    public Mono<Order> submit(String isbn, int quantity) {
        return bookClient.getByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save);
    }

    private static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.of(book.isbn(),
                book.title() + " - " + book.author(),
                book.price(),
                quantity,
                OrderStatus.ACCEPTED);
    }

    private static Order buildRejectedOrder(String bookIsbn, int quantity) {
        return Order.of(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
    }


}
