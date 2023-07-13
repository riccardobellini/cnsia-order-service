package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.config.ClientProperties;
import com.polarbookshop.orderservice.dto.book.Book;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(ClientProperties.class)
class BookClientTest {

    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        final var webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").uri().toString())
                .build();
        this.bookClient = new BookClientImpl(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Autowired
    private ClientProperties properties;

    @Test
    void whenBookExistsThenReturnBook() {
        final String isbn = "1234567890";
        final MockResponse response = new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                            "isbn": %s,
                            "title": "Title",
                            "author": "Author",
                            "price": 9.90,
                            "publisher": "Polarsophia"
                        }
                        """.formatted(isbn));
        mockWebServer.enqueue(response);

        final Mono<Book> book = bookClient.getByIsbn(isbn);

        StepVerifier.create(book)
                .expectNextMatches(b -> b.isbn().equals(isbn))
                .verifyComplete();
    }

    @Test
    void whenBookDoesNotExistThenReturnEmpty() {
        final String isbn = "1234567890";
        final MockResponse response = new MockResponse()
                .setResponseCode(HttpStatus.NOT_FOUND.value());
        mockWebServer.enqueue(response);

        final Mono<Book> book = bookClient.getByIsbn(isbn);

        StepVerifier.create(book)
                .expectComplete()
                .verify();
    }
}
