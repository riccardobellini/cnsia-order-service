package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.dto.book.Book;
import com.polarbookshop.orderservice.dto.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.dto.event.OrderDispatchedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final BookClient bookClient;
    private final StreamBridge streamBridge;

    @Override
    public Flux<Order> getAll() {
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public Mono<Order> submit(String isbn, int quantity) {
        return bookClient.getByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);
    }

    @Override
    public Flux<Order> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> event) {
        return event
                .flatMap(message -> orderRepository.findById(message.orderId()))
                .map(this::buildDispatchedOrder)
                .flatMap(orderRepository::save);
    }

    private void publishOrderAcceptedEvent(Order order) {
        if (order.status() != OrderStatus.ACCEPTED) {
            return;
        }
        final var orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event. [order-id={}]", order.id());
        final var result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
        log.info("Order accepted event processed. [sent-successfully={}]", result);
    }

    private Order buildDispatchedOrder(Order existingOrder) {
        return new Order(
                existingOrder.id(),
                existingOrder.bookIsbn(),
                existingOrder.bookName(),
                existingOrder.bookPrice(),
                existingOrder.quantity(),
                OrderStatus.DISPATCHED,
                existingOrder.createdDate(),
                existingOrder.lastModifiedDate(),
                existingOrder.version()
        );
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
