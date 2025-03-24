package ru.practicum.shareit.item.Impl;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import static ru.practicum.shareit.item.ItemMapper.toItem;
import static ru.practicum.shareit.item.ItemMapper.toItemDto;

@Service
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ItemServiceImpl(ItemRepository itemRepository, UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ItemDto> getAll(Long userId) {
        List<ItemDto> items = new ArrayList<>();
        for (Item item : itemRepository.findAll()) {
            if (item.getOwner().getId().equals(userId)) {
                items.add(toItemDto(item));
            }
        }

        return items;
    }

    @Override
    public ItemDto getById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));

        return toItemDto(item);
    }

    @Override
    public ItemDto create(ItemDto itemDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Невозможно создать вещь - " +
                        "не найден пользователь с id: " + userId));
        Item item = toItem(itemDto);
        item.setOwner(user);
        itemRepository.create(item);

        return toItemDto(item);
    }

    @Override
    public ItemDto update(ItemDto itemDto, Long id, Long userId) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));
        if (!item.getOwner().getId().equals(userId)) {
            throw new ShareItException.NotFoundException("Невозможно обновить вещь - у пользователя с id: " + userId + "нет такой вещи");
        }
        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return toItemDto(itemRepository.update(item));
    }

    @Override
    public void delete(Long id) {
        getById(id);
        itemRepository.delete(id);
    }

    @Override
    public List<ItemDto> search(String text) {
        List<ItemDto> searchedItems = new ArrayList<>();
        if (text.isBlank()) {
            return searchedItems;
        }
        for (Item item : itemRepository.findAll()) {
            if (isSearched(text, item)) {
                searchedItems.add(toItemDto(item));
            }
        }

        return searchedItems;
    }

    private Boolean isSearched(String text, Item item) {
        return item.getAvailable() && (
                item.getName().toLowerCase().contains(text.toLowerCase()) ||
                        item.getDescription().toLowerCase().contains(text.toLowerCase())
        );
    }
}