package ru.practicum.shareit.item.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.CommentMapper;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemMapper itemMapper;
    private final CommentMapper commentMapper;
    private final BookingMapper bookingMapper;

    @Override
    public List<ItemDto> getAll(Long userId) {
        List<Item> items = itemRepository.findByOwnerId(userId);
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        Map<Long, List<Comment>> commentsByItemId = commentRepository.findByItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId()));

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDto itemDto = itemMapper.toItemDto(item);

                    if (item.getOwner().getId().equals(userId)) {
                        List<Booking> lastBookings = bookingRepository.findLastBookingForItem(item.getId(), now);
                        if (!lastBookings.isEmpty()) {
                            itemDto.setLastBooking(bookingMapper.toBookingShortDto(lastBookings.get(0)));
                        }

                        List<Booking> nextBookings = bookingRepository.findNextBookingForItem(item.getId(), now);
                        if (!nextBookings.isEmpty()) {
                            itemDto.setNextBooking(bookingMapper.toBookingShortDto(nextBookings.get(0)));
                        }
                    }

                    List<Comment> comments = commentsByItemId.getOrDefault(item.getId(), Collections.emptyList());
                    itemDto.setComments(comments.stream()
                            .map(commentMapper::toCommentDto)
                            .collect(Collectors.toList()));

                    return itemDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ItemDto getById(Long id, Long userId) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));

        ItemDto itemDto = itemMapper.toItemDto(item);

        LocalDateTime now = LocalDateTime.now();
        if (item.getOwner().getId().equals(userId)) {
            List<Booking> lastBookings = bookingRepository.findLastBookingForItem(id, now);
            if (!lastBookings.isEmpty()) {
                itemDto.setLastBooking(bookingMapper.toBookingShortDto(lastBookings.get(0)));
            }

            List<Booking> nextBookings = bookingRepository.findNextBookingForItem(id, now);
            if (!nextBookings.isEmpty()) {
                itemDto.setNextBooking(bookingMapper.toBookingShortDto(nextBookings.get(0)));
            }
        }

        List<Comment> comments = commentRepository.findByItemId(id);
        itemDto.setComments(comments.stream()
                .map(commentMapper::toCommentDto)
                .collect(Collectors.toList()));

        return itemDto;
    }

    @Override
    @Transactional
    public ItemDto create(ItemDto itemDto, Long userId) {
        User user = getUserById(userId); // Отдельный метод без транзакции
        Item item = itemMapper.toItem(itemDto);
        item.setOwner(user);

        return itemMapper.toItemDto(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найден пользователь с id: " + userId));
    }

    @Override
    @Transactional
    public ItemDto update(ItemDto itemDto, Long id, Long userId) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));

        if (!item.getOwner().getId().equals(userId)) {
            throw new ShareItException.NotFoundException("Невозможно обновить вещь - у пользователя с id: " + userId + " нет такой вещи");
        }

        Item updatedItem = itemMapper.updateItemFields(item, itemDto);
        return itemMapper.toItemDto(itemRepository.save(updatedItem));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        itemRepository.findById(id)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + id));
        itemRepository.deleteById(id);
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text.isBlank()) {
            return new ArrayList<>();
        }

        return itemRepository.search(text).stream()
                .map(itemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto createComment(Long itemId, CommentDto commentDto, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найдена вещь с id: " + itemId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Не найден пользователь с id: " + userId));

        boolean hasBookedItem = bookingRepository.hasUserBookedItem(userId, itemId, LocalDateTime.now());

        if (!hasBookedItem) {
            throw new ShareItException.BadRequestException("Пользователь не может оставить отзыв, так как не брал вещь в аренду или аренда еще не завершена");
        }

        Comment comment = commentMapper.toComment(commentDto);
        comment.setItem(item);
        comment.setAuthor(user);
        comment.setCreated(LocalDateTime.now());

        return commentMapper.toCommentDto(commentRepository.save(comment));
    }
}