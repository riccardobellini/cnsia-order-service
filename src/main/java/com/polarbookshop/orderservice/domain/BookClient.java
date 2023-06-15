package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.dto.book.Book;
import reactor.core.publisher.Mono;

public interface BookClient {
    Mono<Book> getByIsbn(String isbn);
}
