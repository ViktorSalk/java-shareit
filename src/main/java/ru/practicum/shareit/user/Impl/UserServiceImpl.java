package ru.practicum.shareit.user.Impl;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final Set<String> emailSet = new HashSet<>();

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найден пользователь с id: " + id));

        return userMapper.toUserDto(user);
    }

    @Override
    public UserDto create(User user) {
        checkEmailUniqueness(user);
        User createdUser = userRepository.create(user);
        emailSet.add(createdUser.getEmail().toLowerCase());
        return userMapper.toUserDto(createdUser);
    }

    @Override
    public UserDto update(User user, Long id) {
        User updatedUser = userRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Невозможно обновить данные пользователя. " +
                        "Не найден пользователь с id: " + id));

        if (user.getEmail() != null && !user.getEmail().equals(updatedUser.getEmail())) {
            checkEmailUniqueness(user);
            emailSet.remove(updatedUser.getEmail().toLowerCase());
            emailSet.add(user.getEmail().toLowerCase());
        }

        User updated = userMapper.updateUserFields(updatedUser, userMapper.toUserDto(user));
        return userMapper.toUserDto(userRepository.update(updated));
    }

    @Override
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найден пользователь с id: " + id));
        emailSet.remove(user.getEmail().toLowerCase());
        userRepository.delete(id);
    }

    private void checkEmailUniqueness(User user) {
        if (user.getEmail() == null) {
            return;
        }

        String email = user.getEmail().toLowerCase();
        if (emailSet.contains(email)) {
            throw new ShareItException.ConflictException("Пользователь с таким email уже зарегистрирован");
        }
    }
}