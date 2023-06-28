package com.polarbookshop.orderservice.domain;

import com.polarbookshop.orderservice.dto.book.Book;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

@SpringBootTest
class BookClientTest {

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WebClient testWebClient() {
            return WebClient.builder()
                    .baseUrl(mockWebServer.url("/").uri().toString()).build();
        }
    }

    @Autowired
    private BookClient bookClient;

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
