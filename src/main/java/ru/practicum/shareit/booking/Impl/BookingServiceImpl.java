package ru.practicum.shareit.booking.Impl;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookerStateProcessor;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.service.handler.owner.OwnerStateProcessor;
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
    private final BookerStateProcessor bookerStateProcessor;
    private final OwnerStateProcessor ownerStateProcessor;

    public BookingServiceImpl(BookingRepository bookingRepository,
                              UserRepository userRepository,
                              ItemRepository itemRepository,
                              BookingMapper bookingMapper,
                              BookerStateProcessor bookerStateProcessor,
                              OwnerStateProcessor ownerStateProcessor) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.bookingMapper = bookingMapper;
        this.bookerStateProcessor = bookerStateProcessor;
        this.ownerStateProcessor = ownerStateProcessor;
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
        if (bookingDto.getStart() != null && bookingDto.getEnd() != null) {
            if (bookingDto.getEnd().isBefore(bookingDto.getStart()) || bookingDto.getEnd().equals(bookingDto.getStart())) {
                throw new ShareItException.BadRequestException("Дата окончания должна быть позже даты начала");
            }
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
    public List<BookingDto> getAllByBooker(Long userId, String stateParam) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Пользователь с id " + userId + " не найден"));

        BookingState state;
        try {
            state = BookingState.valueOf(stateParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ShareItException.BadRequestException("Неизвестный статус: " + stateParam);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookerStateProcessor.process(state, userId, now);

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getAllByOwner(Long userId, String stateParam) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Пользователь с id " + userId + " не найден"));

        BookingState state;
        try {
            state = BookingState.valueOf(stateParam.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ShareItException.BadRequestException("Неизвестный статус: " + stateParam);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = ownerStateProcessor.process(state, userId, now);

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }
}