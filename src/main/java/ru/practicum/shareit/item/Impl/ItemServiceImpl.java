package ru.practicum.shareit.item.Impl;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;

    public ItemServiceImpl(ItemRepository itemRepository, UserRepository userRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.itemMapper = itemMapper;
    }

    @Override
    public List<ItemDto> getAll(Long userId) {
        return itemRepository.findAll().stream()
                .filter(item -> item.getOwner().getId().equals(userId))
                .map(itemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public ItemDto getById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));

        return itemMapper.toItemDto(item);
    }

    @Override
    public ItemDto create(ItemDto itemDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Невозможно создать вещь - " +
                        "не найден пользователь с id: " + userId));
        Item item = itemMapper.toItem(itemDto);
        item.setOwner(user);
        itemRepository.create(item);

        return itemMapper.toItemDto(item);
    }

    @Override
    public ItemDto update(ItemDto itemDto, Long id, Long userId) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ShareItException.NotFoundException("Невозможно обновить вещь - у пользователя с id: " + userId + " нет такой вещи");
        }

        Item updatedItem = itemMapper.updateItemFields(item, itemDto);
        return itemMapper.toItemDto(itemRepository.update(updatedItem));
    }

    @Override
    public void delete(Long id) {
        getById(id);
        itemRepository.delete(id);
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text.isBlank()) {
            return new ArrayList<>();
        }

        String searchText = text.toLowerCase();
        return itemRepository.findAll().stream()
                .filter(item -> item.getAvailable() &&
                        (item.getName().toLowerCase().contains(searchText) ||
                                item.getDescription().toLowerCase().contains(searchText)))
                .map(itemMapper::toItemDto)
                .collect(Collectors.toList());
    }
}