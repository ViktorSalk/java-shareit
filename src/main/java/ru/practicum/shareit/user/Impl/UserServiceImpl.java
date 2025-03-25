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

import static ru.practicum.shareit.user.UserMapper.toUserDto;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final Set<String> emailSet = new HashSet<>();

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
        userRepository.findAll().forEach(user -> emailSet.add(user.getEmail().toLowerCase()));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найден пользователь с id: " + id));

        return toUserDto(user);
    }

    @Override
    public UserDto create(User user) {
        checkEmailUniqueness(user);
        User createdUser = userRepository.create(user);
        emailSet.add(createdUser.getEmail().toLowerCase());
        return toUserDto(createdUser);
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
            updatedUser.setEmail(user.getEmail());
        }

        if (user.getName() != null) {
            updatedUser.setName(user.getName());
        }

        return toUserDto(userRepository.update(updatedUser));
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