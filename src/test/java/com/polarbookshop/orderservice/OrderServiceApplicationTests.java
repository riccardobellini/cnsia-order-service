package com.polarbookshop.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.orderservice.config.DataConfig;
import com.polarbookshop.orderservice.domain.BookClient;
import com.polarbookshop.orderservice.domain.Order;
import com.polarbookshop.orderservice.domain.OrderStatus;
import com.polarbookshop.orderservice.dto.OrderRequest;
import com.polarbookshop.orderservice.dto.book.Book;
import com.polarbookshop.orderservice.dto.event.OrderAcceptedMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DataConfig.class)
@Testcontainers
class OrderServiceApplicationTests {

    @MockBean
    private BookClient bookClient;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutputDestination outputDestination;

    @Container
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15.2"));

    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", OrderServiceApplicationTests::r2dbcUrl);
        registry.add("spring.r2dbc.username", postgresql::getUsername);
        registry.add("spring.r2dbc.password", postgresql::getPassword);
        registry.add("spring.flyway.url", postgresql::getJdbcUrl);
    }

    private static String r2dbcUrl() {
        return String.format("r2dbc:postgresql://%s:%s/%s", postgresql.getHost(),
                postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT), postgresql.getDatabaseName());
    }

    @Test
    void whenGetOrdersThenReturn() throws IOException {
        final var isbn = "1234567890";
        final var book = new Book(isbn, "Title", "Author", BigDecimal.valueOf(9.90));
        given(bookClient.getByIsbn(isbn)).willReturn(Mono.just(book));
        final var request = new OrderRequest(isbn, 1);

        final var expectedOrder = webTestClient.post().uri("/orders")
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(expectedOrder).isNotNull();
        assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(expectedOrder.id()));

        webTestClient.get().uri("/orders")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Order.class).value(orders -> {
                    assertThat(orders).anyMatch(order -> order.bookIsbn().equals(isbn));
                });
    }

    @Test
    void whenPostRequestAndBookExistsThenOrderAccepted() throws IOException {
        final String bookIsbn = "1234567899";
        final Book book = new Book(bookIsbn, "Title", "Author", BigDecimal.valueOf(9.90));
        given(bookClient.getByIsbn(bookIsbn)).willReturn(Mono.just(book));
        final OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

        final Order createdOrder = webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.bookName()).isEqualTo(book.title() + " - " + book.author());
        assertThat(createdOrder.bookPrice()).isEqualTo(book.price());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.ACCEPTED);

        assertThat(objectMapper.readValue(outputDestination.receive().getPayload(), OrderAcceptedMessage.class))
                .isEqualTo(new OrderAcceptedMessage(createdOrder.id()));
    }

    @Test
    void whenPostRequestAndBookNotExistsThenOrderRejected() {
        final String bookIsbn = "1234567894";
        given(bookClient.getByIsbn(bookIsbn)).willReturn(Mono.empty());
        final OrderRequest orderRequest = new OrderRequest(bookIsbn, 3);

        final Order createdOrder = webTestClient.post().uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Order.class).returnResult().getResponseBody();

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.bookIsbn()).isEqualTo(orderRequest.isbn());
        assertThat(createdOrder.quantity()).isEqualTo(orderRequest.quantity());
        assertThat(createdOrder.status()).isEqualTo(OrderStatus.REJECTED);
    }

}
