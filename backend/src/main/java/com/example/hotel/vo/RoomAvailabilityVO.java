package com.example.hotel.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RoomAvailabilityVO {

    private Long id;

    private String roomNumber;

    private Long roomTypeId;

    private Integer floor;

    private String cleanStatus;

    private String roomTypeName;

    private BigDecimal basePrice;

    private Integer maxGuests;

    private String bedType;

    private Integer area;

    private String description;

    private String amenities;
}
