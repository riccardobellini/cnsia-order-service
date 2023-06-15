package com.polarbookshop.orderservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotBlank(message = "The book ISBN must not be blank")
                           String isbn,
                           @NotNull(message = "Invalid order quantity")
                           @Min(value = 1, message = "Order quantity must be at least 1")
                           @Max(value = 5, message = "Order quantity must be at most 5")
                           Integer quantity) {
}
