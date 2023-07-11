package com.polarbookshop.orderservice.dto.event;

public record OrderDispatchedMessage(
        Long orderId
) {
}
