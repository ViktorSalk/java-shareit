package ru.practicum.shareit.user.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
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
        userDto = UserDto.builder()
                .name("Test User")
                .email("test@email.com")
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
            assertEquals(1L, createdUser.getId());
            assertEquals(userDto.getName(), createdUser.getName());
            assertEquals(userDto.getEmail(), createdUser.getEmail());
        }

        @Test
        @DisplayName("Error when creating a user with a duplicate email")
        void createUserWithDuplicateEmailTest() {
            userController.create(user);

            User duplicateUser = User.builder()
                    .name("Another User")
                    .email("test@email.com") // Тот же email
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
            userController.create(user);

            // Создаем второго пользователя
            UserDto secondUser = UserDto.builder()
                    .name("Second User")
                    .email("second@email.com")
                    .build();
            userController.create(userMapper.toUser(secondUser));

            List<UserDto> users = userController.getAll();

            assertEquals(2, users.size());
            assertEquals("Test User", users.get(0).getName());
            assertEquals("Second User", users.get(1).getName());
        }

        @Test
        @DisplayName("Error when receiving a non-existent user")
        void getNonExistentUserTest() {
            assertThrows(ShareItException.NotFoundException.class,
                    () -> userController.getById(999L));
        }
    }

    @Nested // Тесты на обновление пользователей
    @DisplayName("Updating users")
    class UpdateUserTests {
        @Test
        @DisplayName("Full user update")
        void updateUserTest() {
            UserDto createdUser = userController.create(user);

            User updatedUser = User.builder()
                    .name("Updated Name")
                    .email("updated@email.com")
                    .build();

            UserDto result = userController.update(updatedUser, createdUser.getId());

            assertEquals("Updated Name", result.getName());
            assertEquals("updated@email.com", result.getEmail());

            UserDto retrievedUser = userController.getById(createdUser.getId());
            assertEquals("Updated Name", retrievedUser.getName());
            assertEquals("updated@email.com", retrievedUser.getEmail());
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

            User emailUpdate = User.builder()
                    .email("new@email.com")
                    .build();
            result = userController.update(emailUpdate, createdUser.getId());
            assertEquals("New Name", result.getName());
            assertEquals("new@email.com", result.getEmail());
        }
    }

    @Nested // Тесты на удаление пользователей
    @DisplayName("Deleting Users")
    class DeleteUserTests {
        @Test
        @DisplayName("Successful user deletion")
        void deleteUserTest() {
            UserDto createdUser = userController.create(user);
            assertEquals(1, userController.getAll().size());

            userController.delete(createdUser.getId());

            assertEquals(0, userController.getAll().size());

            assertThrows(ShareItException.NotFoundException.class,
                    () -> userController.getById(createdUser.getId()));
        }
    }
}