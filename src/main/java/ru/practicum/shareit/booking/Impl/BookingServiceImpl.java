package ru.practicum.shareit.booking.Impl;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BookingMapper bookingMapper;

    public BookingServiceImpl(BookingRepository bookingRepository, UserRepository userRepository,
                              ItemRepository itemRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.bookingMapper = bookingMapper;
    }

    @Override
    public BookingDto create(BookingDto bookingDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Пользователь с id " + userId + " не найден"));

        if (bookingDto.getItemId() == null) {
            throw new ShareItException.BadRequestException("ID предмета не указан");
        }

        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new ShareItException.NotFoundException("Предмет с id " + bookingDto.getItemId() + " не найден"));

        if (!item.getAvailable()) {
            throw new ShareItException.BadRequestException("Предмет недоступен для бронирования");
        }

        if (item.getOwner().getId().equals(userId)) {
            throw new ShareItException.NotFoundException("Владелец не может бронировать свой предмет");
        }

        LocalDateTime now = LocalDateTime.now();
        if (bookingDto.getStart() == null) {
            throw new ShareItException.BadRequestException("Дата начала бронирования не указана");
        }
        if (bookingDto.getEnd() == null) {
            throw new ShareItException.BadRequestException("Дата окончания бронирования не указана");
        }
        if (bookingDto.getStart().isBefore(now)) {
            throw new ShareItException.BadRequestException("Дата начала бронирования не может быть в прошлом");
        }
        if (bookingDto.getEnd().isBefore(now)) {
            throw new ShareItException.BadRequestException("Дата окончания бронирования не может быть в прошлом");
        }
        if (bookingDto.getEnd().isBefore(bookingDto.getStart()) || bookingDto.getEnd().equals(bookingDto.getStart())) {
            throw new ShareItException.BadRequestException("Дата окончания должна быть позже даты начала");
        }

        Booking booking = new Booking();
        booking.setStart(bookingDto.getStart());
        booking.setEnd(bookingDto.getEnd());
        booking.setItem(item);
        booking.setBooker(user);
        booking.setStatus(BookingStatus.WAITING);

        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.toBookingDto(savedBooking);
    }

    @Override
    public BookingDto approve(Long bookingId, Long userId, Boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Бронирование с id " + bookingId + " не найдено"));

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ShareItException.ForbiddenException("Только владелец предмета может подтверждать бронирование");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ShareItException.BadRequestException("Бронирование уже обработано");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return bookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Бронирование с id " + bookingId + " не найдено"));

        if (!booking.getBooker().getId().equals(userId) && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new ShareItException.NotFoundException("Доступ запрещен");
        }

        return bookingMapper.toBookingDto(booking);
    }

    @Override
    public List<BookingDto> getAllByBooker(Long userId, String state) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Пользователь с id " + userId + " не найден"));

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state.toUpperCase()) {
            case "ALL":
                bookings = bookingRepository.findAllByBookerId(userId);
                break;
            case "CURRENT":
                bookings = bookingRepository.findAllByBookerIdAndStartBeforeAndEndAfter(userId, now, now);
                break;
            case "PAST":
                bookings = bookingRepository.findAllByBookerIdAndEndBefore(userId, now);
                break;
            case "FUTURE":
                bookings = bookingRepository.findAllByBookerIdAndStartAfter(userId, now);
                break;
            case "WAITING":
                bookings = bookingRepository.findAllByBookerIdAndStatus(userId, BookingStatus.WAITING);
                break;
            case "REJECTED":
                bookings = bookingRepository.findAllByBookerIdAndStatus(userId, BookingStatus.REJECTED);
                break;
            default:
                throw new ShareItException.BadRequestException("Неизвестный статус: " + state);
        }

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, String state) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Пользователь с id " + userId + " не найден"));

        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state.toUpperCase()) {
            case "ALL":
                bookings = bookingRepository.findAllByItemOwnerId(userId);
                break;
            case "CURRENT":
                bookings = bookingRepository.findAllByItemOwnerIdAndStartBeforeAndEndAfter(userId, now, now);
                break;
            case "PAST":
                bookings = bookingRepository.findAllByItemOwnerIdAndEndBefore(userId, now);
                break;
            case "FUTURE":
                bookings = bookingRepository.findAllByItemOwnerIdAndStartAfter(userId, now);
                break;
            case "WAITING":
                bookings = bookingRepository.findAllByItemOwnerIdAndStatus(userId, BookingStatus.WAITING);
                break;
            case "REJECTED":
                bookings = bookingRepository.findAllByItemOwnerIdAndStatus(userId, BookingStatus.REJECTED);
                break;
            default:
                throw new ShareItException.BadRequestException("Неизвестный статус: " + state);
        }

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }
}