package ru.practicum.shareit.request.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;


/**
 * TODO Sprint add-item-requests.
 */
@Getter
@Setter
public class ItemRequest {
    private Long id;

    @NotBlank
    private String description;

    private User requestor;

    private LocalDateTime created;
}