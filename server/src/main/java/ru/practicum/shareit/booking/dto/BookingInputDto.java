package ru.practicum.shareit.booking.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BookingInputDto {
    private Long itemId;
    private LocalDateTime start;
    private LocalDateTime end;
}