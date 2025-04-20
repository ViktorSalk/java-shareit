package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
@Builder(toBuilder = true)
public class UserDto {
    private Long id;

    @NotBlank
    private String name;

    @Email
    private String email;
}