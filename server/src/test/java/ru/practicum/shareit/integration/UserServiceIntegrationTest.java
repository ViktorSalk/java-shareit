package ru.practicum.shareit.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Test // Тест на создание и получение пользователя по id
    @DisplayName("Интеграционный тест: создание и получение пользователя")
    void createAndGetUserIntegrationTest() {
        User user = User.builder()
                .name("Test User")
                .email("test" + System.currentTimeMillis() + "@test.com")
                .build();

        UserDto createdUser = userService.create(user);

        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        assertEquals(user.getName(), createdUser.getName());
        assertEquals(user.getEmail(), createdUser.getEmail());

        UserDto retrievedUser = userService.getById(createdUser.getId());

        assertNotNull(retrievedUser);
        assertEquals(createdUser.getId(), retrievedUser.getId());
        assertEquals(createdUser.getName(), retrievedUser.getName());
        assertEquals(createdUser.getEmail(), retrievedUser.getEmail());
    }

    @Test // Тест на обновление пользователя по id
    @DisplayName("Интеграционный тест: обновление пользователя")
    void updateUserIntegrationTest() {
        User user = User.builder()
                .name("Original Name")
                .email("original" + System.currentTimeMillis() + "@test.com")
                .build();

        UserDto createdUser = userService.create(user);

        User updatedUser = User.builder()
                .name("Updated Name")
                .email("updated" + System.currentTimeMillis() + "@test.com")
                .build();

        UserDto result = userService.update(updatedUser, createdUser.getId());

        assertNotNull(result);
        assertEquals(createdUser.getId(), result.getId());
        assertEquals("Updated Name", result.getName());
        assertEquals(updatedUser.getEmail(), result.getEmail());

        UserDto retrievedUser = userService.getById(createdUser.getId());

        assertEquals("Updated Name", retrievedUser.getName());
        assertEquals(updatedUser.getEmail(), retrievedUser.getEmail());
    }

    @Test // Тест на получение всех пользователей
    @DisplayName("Интеграционный тест: получение всех пользователей")
    void getAllUsersIntegrationTest() {
        User user1 = User.builder()
                .name("User 1")
                .email("user1" + System.currentTimeMillis() + "@test.com")
                .build();
        User user2 = User.builder()
                .name("User 2")
                .email("user2" + System.currentTimeMillis() + "@test.com")
                .build();
        User user3 = User.builder()
                .name("User 3")
                .email("user3" + System.currentTimeMillis() + "@test.com")
                .build();

        UserDto createdUser1 = userService.create(user1);
        UserDto createdUser2 = userService.create(user2);
        UserDto createdUser3 = userService.create(user3);

        List<UserDto> users = userService.getAll();

        assertNotNull(users);
        assertTrue(users.size() >= 3);
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(createdUser1.getId())));
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(createdUser2.getId())));
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(createdUser3.getId())));
    }

    @Test // Тест на удаление пользователя
    @DisplayName("Интеграционный тест: удаление пользователя")
    void deleteUserIntegrationTest() {
        User user = User.builder()
                .name("User to Delete")
                .email("delete" + System.currentTimeMillis() + "@test.com")
                .build();

        UserDto createdUser = userService.create(user);

        assertNotNull(userService.getById(createdUser.getId()));

        userService.delete(createdUser.getId());

        assertThrows(ShareItException.NotFoundException.class, () -> userService.getById(createdUser.getId()));
    }
}