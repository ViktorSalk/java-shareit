package ru.practicum.shareit.item.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.practicum.shareit.ShareItApp;
import ru.practicum.shareit.TestConfig;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ContextConfiguration(classes = {ShareItApp.class, TestConfig.class})
@ExtendWith(MockitoExtension.class)
class ItemControllerTest {
    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    private ItemDto itemDto;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        itemDto = ItemDto.builder()
                .id(1L)
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
            when(itemService.create(any(ItemDto.class), anyLong())).thenReturn(itemDto);

            ItemDto createdItem = itemController.create(userId, itemDto);

            assertNotNull(createdItem);
            assertEquals(itemDto.getId(), createdItem.getId());
            assertEquals(itemDto.getName(), createdItem.getName());
            assertEquals(itemDto.getDescription(), createdItem.getDescription());
            assertEquals(itemDto.getAvailable(), createdItem.getAvailable());
        }

        @Test
        @DisplayName("Error when creating an item with a non-existent user")
        void createItemWithNonExistentUserTest() {
            when(itemService.create(any(ItemDto.class), eq(999L)))
                    .thenThrow(new ShareItException.NotFoundException("User not found"));

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
            when(itemService.getById(anyLong(), anyLong())).thenReturn(itemDto);

            ItemDto retrievedItem = itemController.getById(1L, userId);

            assertNotNull(retrievedItem);
            assertEquals(itemDto.getId(), retrievedItem.getId());
            assertEquals(itemDto.getName(), retrievedItem.getName());
            assertEquals(itemDto.getDescription(), retrievedItem.getDescription());
            assertEquals(itemDto.getAvailable(), retrievedItem.getAvailable());
        }

        @Test
        @DisplayName("Getting all the user's items")
        void getAllUserItemsTest() {
            ItemDto secondItem = ItemDto.builder()
                    .id(2L)
                    .name("Second Item")
                    .description("Another Description")
                    .available(true)
                    .build();
            List<ItemDto> items = Arrays.asList(itemDto, secondItem);

            when(itemService.getAll(anyLong())).thenReturn(items);

            List<ItemDto> retrievedItems = itemController.getAll(userId);

            assertEquals(2, retrievedItems.size());
            assertEquals("Test Item", retrievedItems.get(0).getName());
            assertEquals("Second Item", retrievedItems.get(1).getName());
        }

        @Test
        @DisplayName("Error when receiving a non-existent item")
        void getNonExistentItemTest() {
            when(itemService.getById(eq(999L), anyLong()))
                    .thenThrow(new ShareItException.NotFoundException("Item not found"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> itemController.getById(999L, userId));
        }
    }

    @Nested // Тесты на обновление предметов
    @DisplayName("Updating items")
    class UpdateItemTests {
        @Test
        @DisplayName("Full item update")
        void updateItemTest() {
            ItemDto updateRequest = ItemDto.builder()
                    .name("Updated Item")
                    .description("Updated Description")
                    .available(false)
                    .build();

            ItemDto updatedItem = ItemDto.builder()
                    .id(1L)
                    .name("Updated Item")
                    .description("Updated Description")
                    .available(false)
                    .build();

            when(itemService.update(any(ItemDto.class), anyLong(), anyLong())).thenReturn(updatedItem);
            when(itemService.getById(anyLong(), anyLong())).thenReturn(updatedItem);

            ItemDto result = itemController.update(updateRequest, 1L, userId);

            assertEquals("Updated Item", result.getName());
            assertEquals("Updated Description", result.getDescription());
            assertEquals(false, result.getAvailable());

            ItemDto retrievedItem = itemController.getById(1L, userId);
            assertEquals("Updated Item", retrievedItem.getName());
            assertEquals("Updated Description", retrievedItem.getDescription());
            assertEquals(false, retrievedItem.getAvailable());
        }

        @Test
        @DisplayName("Partial item update")
        void partialUpdateItemTest() {
            ItemDto nameUpdateRequest = ItemDto.builder().name("New Name").build();
            ItemDto nameUpdatedItem = ItemDto.builder()
                    .id(1L)
                    .name("New Name")
                    .description("Test Description")
                    .available(true)
                    .build();

            when(itemService.update(eq(nameUpdateRequest), anyLong(), anyLong())).thenReturn(nameUpdatedItem);

            ItemDto result = itemController.update(nameUpdateRequest, 1L, userId);
            assertEquals("New Name", result.getName());
            assertEquals("Test Description", result.getDescription());
            assertEquals(true, result.getAvailable());

            ItemDto descUpdateRequest = ItemDto.builder().description("New Description").build();
            ItemDto descUpdatedItem = ItemDto.builder()
                    .id(1L)
                    .name("New Name")
                    .description("New Description")
                    .available(true)
                    .build();

            when(itemService.update(eq(descUpdateRequest), anyLong(), anyLong())).thenReturn(descUpdatedItem);

            result = itemController.update(descUpdateRequest, 1L, userId);
            assertEquals("New Name", result.getName());
            assertEquals("New Description", result.getDescription());
            assertEquals(true, result.getAvailable());

            ItemDto availUpdateRequest = ItemDto.builder().available(false).build();
            ItemDto availUpdatedItem = ItemDto.builder()
                    .id(1L)
                    .name("New Name")
                    .description("New Description")
                    .available(false)
                    .build();

            when(itemService.update(eq(availUpdateRequest), anyLong(), anyLong())).thenReturn(availUpdatedItem);

            result = itemController.update(availUpdateRequest, 1L, userId);
            assertEquals("New Name", result.getName());
            assertEquals("New Description", result.getDescription());
            assertEquals(false, result.getAvailable());
        }

        @Test
        @DisplayName("Error when updating an item by a non-owner")
        void updateItemByNonOwnerTest() {
            ItemDto updateRequest = ItemDto.builder()
                    .name("Unauthorized Update")
                    .build();

            when(itemService.update(any(ItemDto.class), anyLong(), eq(2L)))
                    .thenThrow(new ShareItException.NotFoundException("Item not found for this user"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> itemController.update(updateRequest, 1L, 2L));
        }
    }

    @Nested // Тесты на удаление предметов
    @DisplayName("Deleting items")
    class DeleteItemTests {
        @Test
        @DisplayName("Successful removal of an item")
        void deleteItemTest() {
            when(itemService.getAll(userId))
                    .thenReturn(Collections.singletonList(itemDto))
                    .thenReturn(Collections.emptyList()); // для второго вызова (после удаления)

            doNothing().when(itemService).delete(anyLong());

            assertEquals(1, itemController.getAll(userId).size());

            itemController.delete(1L);

            assertEquals(0, itemController.getAll(userId).size());
        }
    }

    @Nested // Тесты на поиск предметов
    @DisplayName("Searching for items")
    class SearchItemTests {
        @Test
        @DisplayName("Search for items based on various criteria")
        void searchItemsTest() {
            ItemDto testItem = ItemDto.builder()
                    .id(1L)
                    .name("Test Item")
                    .description("Test Description")
                    .available(true)
                    .build();

            ItemDto specialItem = ItemDto.builder()
                    .id(2L)
                    .name("Special Item")
                    .description("Unique features")
                    .available(true)
                    .build();

            when(itemService.search("Test"))
                    .thenReturn(Collections.singletonList(testItem));

            when(itemService.search("Unique"))
                    .thenReturn(Collections.singletonList(specialItem));

            when(itemService.search("Item"))
                    .thenReturn(Arrays.asList(testItem, specialItem));

            when(itemService.search("Unavailable"))
                    .thenReturn(Collections.emptyList());

            when(itemService.search(""))
                    .thenReturn(Collections.emptyList());

            List<ItemDto> results = itemController.search("Test");
            assertTrue(results.stream().anyMatch(item -> item.getName().equals("Test Item")));

            results = itemController.search("Unique");
            assertTrue(results.stream().anyMatch(item -> item.getName().equals("Special Item")));

            results = itemController.search("Item");
            assertTrue(results.stream().anyMatch(item -> item.getName().equals("Test Item")));
            assertTrue(results.stream().anyMatch(item -> item.getName().equals("Special Item")));

            results = itemController.search("Unavailable");
            assertEquals(0, results.size());

            results = itemController.search("");
            assertEquals(0, results.size());
        }
    }

    @Nested // Тесты на комментарии
    @DisplayName("Comment Tests")
    class CommentTests {
        @Test
        @DisplayName("Create comment successfully")
        void createCommentTest() {
            CommentDto commentDto = CommentDto.builder()
                    .text("Great item!")
                    .build();

            CommentDto createdComment = CommentDto.builder()
                    .id(1L)
                    .text("Great item!")
                    .authorName("User")
                    .build();

            when(itemService.createComment(anyLong(), any(CommentDto.class), anyLong()))
                    .thenReturn(createdComment);

            CommentDto result = itemController.createComment(1L, commentDto, userId);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Great item!", result.getText());
            assertEquals("User", result.getAuthorName());
        }

        @Test
        @DisplayName("Error when creating comment for non-existent item")
        void createCommentForNonExistentItemTest() {
            CommentDto commentDto = CommentDto.builder()
                    .text("Great item!")
                    .build();

            when(itemService.createComment(eq(999L), any(CommentDto.class), anyLong()))
                    .thenThrow(new ShareItException.NotFoundException("Item not found"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> itemController.createComment(999L, commentDto, userId));
        }

        @Test
        @DisplayName("Error when user hasn't booked the item")
        void createCommentWithoutBookingTest() {
            CommentDto commentDto = CommentDto.builder()
                    .text("Great item!")
                    .build();

            when(itemService.createComment(anyLong(), any(CommentDto.class), anyLong()))
                    .thenThrow(new ShareItException.BadRequestException("User hasn't booked this item"));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> itemController.createComment(1L, commentDto, userId));
        }
    }
}