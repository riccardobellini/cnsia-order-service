package com.polarbookshop.orderservice.dto.event;

public record OrderAcceptedMessage(
        Long orderId
) {
}
