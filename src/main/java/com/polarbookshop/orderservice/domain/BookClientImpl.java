package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.config.ClientProperties;
import com.polarbookshop.orderservice.dto.book.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BookClientImpl implements BookClient {

    private final WebClient webClient;
    private final ClientProperties properties;


    @Override
    public Mono<Book> getByIsbn(String isbn) {
        return webClient
                .get()
                .uri("/books/%s".formatted(isbn))
                .retrieve()
                .bodyToMono(Book.class)
                .timeout(Duration.ofSeconds(properties.catalogServiceTimeoutSecs()), Mono.empty())
                .onErrorResume(WebClientResponseException.NotFound.class, nf -> Mono.empty());
    }
}
