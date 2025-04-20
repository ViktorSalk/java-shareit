package ru.practicum.shareit.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.ShareItServerApp;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(classes = ShareItServerApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerTest {
    @Autowired
    private UserController userController;

    @Autowired
    private UserMapper userMapper;

    private UserDto userDto;
    private User user;

    @BeforeEach
    void setUp() {
        String uniqueEmail = "test" + System.currentTimeMillis() + "@email.com";

        userDto = UserDto.builder()
                .name("Test User")
                .email(uniqueEmail)
                .build();
        user = userMapper.toUser(userDto);
    }

    @Nested // Тесты на создание пользователей
    @DisplayName("Creating Users")
    class CreateUserTests {
        @Test
        @DisplayName("Successful user creation")
        void createUserTest() {
            UserDto createdUser = userController.create(user);

            assertNotNull(createdUser);
            assertNotNull(createdUser.getId()); // Проверяем, что ID не null, а не конкретное значение
            assertEquals(userDto.getName(), createdUser.getName());
            assertEquals(userDto.getEmail(), createdUser.getEmail());
        }

        @Test
        @DisplayName("Error when creating a user with a duplicate email")
        void createUserWithDuplicateEmailTest() {
            userController.create(user);

            User duplicateUser = User.builder()
                    .name("Another User")
                    .email(user.getEmail()) // Тот же email
                    .build();

            assertThrows(ShareItException.ConflictException.class,
                    () -> userController.create(duplicateUser));
        }
    }

    @Nested // Тесты на получение пользователей
    @DisplayName("Getting Users")
    class GetUserTests {
        @Test
        @DisplayName("Getting a user by ID")
        void getUserByIdTest() {
            UserDto createdUser = userController.create(user);
            UserDto retrievedUser = userController.getById(createdUser.getId());

            assertNotNull(retrievedUser);
            assertEquals(createdUser.getId(), retrievedUser.getId());
            assertEquals(createdUser.getName(), retrievedUser.getName());
            assertEquals(createdUser.getEmail(), retrievedUser.getEmail());
        }

        @Test
        @DisplayName("Getting all users")
        void getAllUsersTest() {
            UserDto createdUser = userController.create(user);

            String secondEmail = "second" + System.currentTimeMillis() + "@email.com";
            UserDto secondUser = UserDto.builder()
                    .name("Second User")
                    .email(secondEmail)
                    .build();
            UserDto createdSecondUser = userController.create(userMapper.toUser(secondUser));

            List<UserDto> users = userController.getAll();

            assertTrue(users.size() >= 2);
            assertTrue(users.stream().anyMatch(u -> u.getId().equals(createdUser.getId())));
            assertTrue(users.stream().anyMatch(u -> u.getId().equals(createdSecondUser.getId())));
        }

        @Test
        @DisplayName("Error when receiving a non-existent user")
        void getNonExistentUserTest() {
            List<UserDto> allUsers = userController.getAll();
            long maxId = allUsers.stream()
                    .mapToLong(UserDto::getId)
                    .max()
                    .orElse(0);

            long nonExistentId = maxId + 1000;

            assertThrows(ShareItException.NotFoundException.class,
                    () -> userController.getById(nonExistentId));
        }
    }

    @Nested // Тесты на обновление пользователей
    @DisplayName("Updating users")
    class UpdateUserTests {
        @Test
        @DisplayName("Full user update")
        void updateUserTest() {
            UserDto createdUser = userController.create(user);

            String updatedEmail = "updated" + System.currentTimeMillis() + "@email.com";
            User updatedUser = User.builder()
                    .name("Updated Name")
                    .email(updatedEmail)
                    .build();

            UserDto result = userController.update(updatedUser, createdUser.getId());

            assertEquals("Updated Name", result.getName());
            assertEquals(updatedEmail, result.getEmail());

            UserDto retrievedUser = userController.getById(createdUser.getId());
            assertEquals("Updated Name", retrievedUser.getName());
            assertEquals(updatedEmail, retrievedUser.getEmail());
        }

        @Test // Тест на частичное обновление пользователя
        @DisplayName("Partial user update")
        void partialUpdateUserTest() {
            UserDto createdUser = userController.create(user);
            UserDto result;

            User nameUpdate = User.builder()
                    .name("New Name")
                    .build();
            result = userController.update(nameUpdate, createdUser.getId());
            assertEquals("New Name", result.getName());
            assertEquals(userDto.getEmail(), result.getEmail());

            String newEmail = "new" + System.currentTimeMillis() + "@email.com";
            User emailUpdate = User.builder()
                    .email(newEmail)
                    .build();
            result = userController.update(emailUpdate, createdUser.getId());
            assertEquals("New Name", result.getName());
            assertEquals(newEmail, result.getEmail());
        }
    }

    @Nested // Тесты на удаление пользователей
    @DisplayName("Deleting Users")
    class DeleteUserTests {
        @Test
        @DisplayName("Successful user deletion")
        void deleteUserTest() {
            UserDto createdUser = userController.create(user);
            int initialSize = userController.getAll().size();

            userController.delete(createdUser.getId());

            int newSize = userController.getAll().size();
            assertEquals(initialSize - 1, newSize);

            assertThrows(ShareItException.NotFoundException.class,
                    () -> userController.getById(createdUser.getId()));
        }
    }
}