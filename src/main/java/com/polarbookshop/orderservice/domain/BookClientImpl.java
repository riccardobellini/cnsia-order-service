package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.dto.book.Book;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class BookClientImpl implements BookClient {

    private final WebClient webClient;

    public BookClientImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<Book> getByIsbn(String isbn) {
        return webClient
                .get()
                .uri("/books/%s".formatted(isbn))
                .retrieve()
                .bodyToMono(Book.class)
                .onErrorResume(WebClientResponseException.NotFound.class, nf -> Mono.empty());
    }
}
