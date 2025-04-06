package ru.practicum.shareit.user.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public UserDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найден пользователь с id: " + id));
        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto create(User user) {
        try {
            User createdUser = userRepository.save(user);
            return userMapper.toUserDto(createdUser);
        } catch (DataIntegrityViolationException e) {
            throw new ShareItException.ConflictException("Пользователь с таким email уже зарегистрирован");
        }
    }

    @Override
    @CachePut(value = "users", key = "#id")
    @Transactional
    public UserDto update(User user, Long id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Невозможно обновить данные пользователя. " +
                        "Не найден пользователь с id: " + id));

        try {
            User updated = userMapper.updateUserFields(existingUser, userMapper.toUserDto(user));
            return userMapper.toUserDto(userRepository.save(updated));
        } catch (DataIntegrityViolationException e) {
            throw new ShareItException.ConflictException("Пользователь с таким email уже зарегистрирован");
        }
    }

    @Override
    @CacheEvict(value = "users", key = "#id")
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ShareItException.NotFoundException("Не найден пользователь с id: " + id);
        }
        userRepository.deleteById(id);
    }
}