package ru.practicum.shareit.item.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
public class ItemDto {
    private Long id;

    private String name;

    private String description;

    private Boolean available;

    private ItemRequest request;

    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;

    @Builder.Default
    private List<CommentDto> comments = new ArrayList<>();

    private Long requestId;
}