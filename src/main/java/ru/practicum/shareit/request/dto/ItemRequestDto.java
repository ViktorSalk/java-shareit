package ru.practicum.shareit.request.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TODO Sprint add-item-requests.
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class ItemRequestDto {
    private Long id;

    private String description;

    private UserDto requestor;

    private LocalDateTime created;

    private List<ItemDto> items;
}