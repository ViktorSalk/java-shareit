package ru.practicum.shareit.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private UserDto owner;
    private UserDto user;

    @BeforeEach
    void setUp() {
        User ownerUser = User.builder()
                .name("Owner")
                .email("owner" + System.currentTimeMillis() + "@test.com")
                .build();
        owner = userService.create(ownerUser);

        User regularUser = User.builder()
                .name("User")
                .email("user" + System.currentTimeMillis() + "@test.com")
                .build();
        user = userService.create(regularUser);
    }

    @Test // Интеграционный тест: получение всех вещей пользователя владельца
    @DisplayName("Интеграционный тест: получение всех вещей пользователя")
    void getUserItemsIntegrationTest() {
        ItemDto item1 = ItemDto.builder()
                .name("Item 1")
                .description("Description 1")
                .available(true)
                .build();
        ItemDto item2 = ItemDto.builder()
                .name("Item 2")
                .description("Description 2")
                .available(true)
                .build();
        ItemDto item3 = ItemDto.builder()
                .name("Item 3")
                .description("Description 3")
                .available(false)
                .build();

        itemService.create(item1, owner.getId());
        itemService.create(item2, owner.getId());
        itemService.create(item3, owner.getId());

        ItemDto userItem = ItemDto.builder()
                .name("User Item")
                .description("User Item Description")
                .available(true)
                .build();
        itemService.create(userItem, user.getId());

        List<ItemDto> ownerItems = itemService.getAll(owner.getId());

        assertNotNull(ownerItems);
        assertEquals(3, ownerItems.size());
        assertTrue(ownerItems.stream().anyMatch(item -> item.getName().equals("Item 1")));
        assertTrue(ownerItems.stream().anyMatch(item -> item.getName().equals("Item 2")));
        assertTrue(ownerItems.stream().anyMatch(item -> item.getName().equals("Item 3")));
        assertFalse(ownerItems.stream().anyMatch(item -> item.getName().equals("User Item")));

        List<ItemDto> userItems = itemService.getAll(user.getId());

        assertNotNull(userItems);
        assertEquals(1, userItems.size());
        assertTrue(userItems.stream().anyMatch(item -> item.getName().equals("User Item")));
    }
}