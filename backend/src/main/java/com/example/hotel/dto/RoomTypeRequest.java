package com.example.hotel.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomTypeRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "basePrice is required")
        BigDecimal basePrice,
        @NotNull(message = "maxGuests is required")
        @Min(value = 1, message = "maxGuests must be at least 1")
        Integer maxGuests,
        @NotBlank(message = "bedType is required")
        String bedType,
        @NotNull(message = "area is required")
        @Min(value = 1, message = "area must be at least 1")
        Integer area,
        @NotBlank(message = "description is required")
        String description,
        @NotBlank(message = "amenities is required")
        String amenities,
        String coverImage
) {
}
