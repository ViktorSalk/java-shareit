package ru.practicum.shareit.item.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.controller.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ItemControllerTest {
    @Autowired
    private ItemController itemController;

    @Autowired
    private UserController userController;

    @Autowired
    private UserMapper userMapper;

    private ItemDto itemDto;
    private UserDto userDto;
    private Long userId;

    @BeforeEach
    void setUp() {
        userDto = UserDto.builder()
                .name("Test Owner")
                .email("owner@email.com")
                .build();
        UserDto createdUser = userController.create(userMapper.toUser(userDto));
        userId = createdUser.getId();

        itemDto = ItemDto.builder()
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();
    }

    @Nested // Тесты на создание предметов
    @DisplayName("Creating items")
    class CreateItemTests {
        @Test
        @DisplayName("Successful creation of an item")
        void createItemTest() {
            ItemDto createdItem = itemController.create(userId, itemDto);

            assertNotNull(createdItem);
            assertEquals(1L, createdItem.getId());
            assertEquals(itemDto.getName(), createdItem.getName());
            assertEquals(itemDto.getDescription(), createdItem.getDescription());
            assertEquals(itemDto.getAvailable(), createdItem.getAvailable());
        }

        @Test
        @DisplayName("Error when creating an item with a non-existent user")
        void createItemWithNonExistentUserTest() {
            assertThrows(ShareItException.NotFoundException.class,
                    () -> itemController.create(999L, itemDto));
        }
    }

    @Nested // Тесты на получение предметов
    @DisplayName("Getting items")
    class GetItemTests {
        @Test
        @DisplayName("Getting an item by ID")
        void getItemByIdTest() {
            ItemDto createdItem = itemController.create(userId, itemDto);
            ItemDto retrievedItem = itemController.getById(createdItem.getId());

            assertNotNull(retrievedItem);
            assertEquals(createdItem.getId(), retrievedItem.getId());
            assertEquals(createdItem.getName(), retrievedItem.getName());
            assertEquals(createdItem.getDescription(), retrievedItem.getDescription());
            assertEquals(createdItem.getAvailable(), retrievedItem.getAvailable());
        }

        @Test
        @DisplayName("Getting all the user's items")
        void getAllUserItemsTest() {
            itemController.create(userId, itemDto);

            ItemDto secondItem = ItemDto.builder()
                    .name("Second Item")
                    .description("Another Description")
                    .available(true)
                    .build();
            itemController.create(userId, secondItem);

            List<ItemDto> items = itemController.getAll(userId);

            assertEquals(2, items.size());
            assertEquals("Test Item", items.get(0).getName());
            assertEquals("Second Item", items.get(1).getName());
        }

        @Test
        @DisplayName("Error when receiving a non-existent item")
        void getNonExistentItemTest() {
            assertThrows(ShareItException.NotFoundException.class,
                    () -> itemController.getById(999L));
        }
    }

    @Nested // Тесты на обновление предметов
    @DisplayName("Updating items")
    class UpdateItemTests {
        @Test
        @DisplayName("Full item update")
        void updateItemTest() {
            ItemDto createdItem = itemController.create(userId, itemDto);

            ItemDto updateRequest = ItemDto.builder()
                    .name("Updated Item")
                    .description("Updated Description")
                    .available(false)
                    .build();

            ItemDto updatedItem = itemController.update(updateRequest, createdItem.getId(), userId);

            assertEquals("Updated Item", updatedItem.getName());
            assertEquals("Updated Description", updatedItem.getDescription());
            assertEquals(false, updatedItem.getAvailable());

            ItemDto retrievedItem = itemController.getById(createdItem.getId());
            assertEquals("Updated Item", retrievedItem.getName());
            assertEquals("Updated Description", retrievedItem.getDescription());
            assertEquals(false, retrievedItem.getAvailable());
        }

        @Test
        @DisplayName("Partial item update")
        void partialUpdateItemTest() {
            ItemDto createdItem = itemController.create(userId, itemDto);
            ItemDto result;

            result = itemController.update(
                    ItemDto.builder().name("New Name").build(),
                    createdItem.getId(),
                    userId
            );
            assertEquals("New Name", result.getName());
            assertEquals(itemDto.getDescription(), result.getDescription());
            assertEquals(itemDto.getAvailable(), result.getAvailable());

            result = itemController.update(
                    ItemDto.builder().description("New Description").build(),
                    createdItem.getId(),
                    userId
            );
            assertEquals("New Name", result.getName());
            assertEquals("New Description", result.getDescription());
            assertEquals(itemDto.getAvailable(), result.getAvailable());

            result = itemController.update(
                    ItemDto.builder().available(false).build(),
                    createdItem.getId(),
                    userId
            );
            assertEquals("New Name", result.getName());
            assertEquals("New Description", result.getDescription());
            assertEquals(false, result.getAvailable());
        }

        @Test
        @DisplayName("Error when updating an item by a non-owner")
        void updateItemByNonOwnerTest() {
            ItemDto createdItem = itemController.create(userId, itemDto);

            UserDto anotherUserDto = UserDto.builder()
                    .name("Another User")
                    .email("another@email.com")
                    .build();
            UserDto anotherUser = userController.create(userMapper.toUser(anotherUserDto));

            ItemDto updateRequest = ItemDto.builder()
                    .name("Unauthorized Update")
                    .build();

            assertThrows(ShareItException.NotFoundException.class,
                    () -> itemController.update(updateRequest, createdItem.getId(), anotherUser.getId()));
        }
    }

    @Nested // Тесты на удаление предметов
    @DisplayName("Deleting items")
    class DeleteItemTests {
        @Test
        @DisplayName("Successful removal of an item")
        void deleteItemTest() {
            ItemDto createdItem = itemController.create(userId, itemDto);
            assertEquals(1, itemController.getAll(userId).size());

            itemController.delete(createdItem.getId());

            assertEquals(0, itemController.getAll(userId).size());
        }
    }

    @Nested // Тесты на поиск предметов
    @DisplayName("Searching for items")
    class SearchItemTests {
        @Test
        @DisplayName("Search for items based on various criteria")
        void searchItemsTest() {
            itemController.create(userId, itemDto);

            ItemDto secondItem = ItemDto.builder()
                    .name("Special Item")
                    .description("Unique features")
                    .available(true)
                    .build();
            itemController.create(userId, secondItem);

            ItemDto unavailableItem = ItemDto.builder()
                    .name("Unavailable Special")
                    .description("Not for rent")
                    .available(false)
                    .build();
            itemController.create(userId, unavailableItem);

            List<ItemDto> results = itemController.search("Test");
            assertEquals(1, results.size());
            assertEquals("Test Item", results.get(0).getName());

            results = itemController.search("Unique");
            assertEquals(1, results.size());
            assertEquals("Special Item", results.get(0).getName());

            results = itemController.search("Item");
            assertEquals(2, results.size());

            results = itemController.search("Unavailable");
            assertEquals(0, results.size());

            results = itemController.search("");
            assertEquals(0, results.size());
        }
    }
}