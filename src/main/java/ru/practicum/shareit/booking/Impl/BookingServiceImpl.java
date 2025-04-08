package ru.practicum.shareit.booking.Impl;

import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.handler.AllBookingsHandler;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.service.handler.BookingStateHandler;
import ru.practicum.shareit.booking.service.handler.CurrentBookingsHandler;
import ru.practicum.shareit.booking.service.handler.FutureBookingsHandler;
import ru.practicum.shareit.booking.service.handler.OwnerAllBookingsHandler;
import ru.practicum.shareit.booking.service.handler.OwnerCurrentBookingsHandler;
import ru.practicum.shareit.booking.service.handler.OwnerFutureBookingsHandler;
import ru.practicum.shareit.booking.service.handler.OwnerPastBookingsHandler;
import ru.practicum.shareit.booking.service.handler.OwnerRejectedBookingsHandler;
import ru.practicum.shareit.booking.service.handler.OwnerWaitingBookingsHandler;
import ru.practicum.shareit.booking.service.handler.PastBookingsHandler;
import ru.practicum.shareit.booking.service.handler.RejectedBookingsHandler;
import ru.practicum.shareit.booking.service.handler.WaitingBookingsHandler;
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
    private final BookingStateHandler bookerStateHandler;
    private final BookingStateHandler ownerStateHandler;

    public BookingServiceImpl(BookingRepository bookingRepository, UserRepository userRepository,
                              ItemRepository itemRepository, BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.bookingMapper = bookingMapper;

        // Цепочки обязанностей для букера
        BookingStateHandler allHandler = new AllBookingsHandler(bookingRepository);
        BookingStateHandler currentHandler = new CurrentBookingsHandler(bookingRepository);
        BookingStateHandler pastHandler = new PastBookingsHandler(bookingRepository);
        BookingStateHandler futureHandler = new FutureBookingsHandler(bookingRepository);
        BookingStateHandler waitingHandler = new WaitingBookingsHandler(bookingRepository);
        BookingStateHandler rejectedHandler = new RejectedBookingsHandler(bookingRepository);

        allHandler.setNext(currentHandler)
                .setNext(pastHandler)
                .setNext(futureHandler)
                .setNext(waitingHandler)
                .setNext(rejectedHandler);

        this.bookerStateHandler = allHandler;

        // Цепочки обязанностей для владельца
        BookingStateHandler ownerAllHandler = new OwnerAllBookingsHandler(bookingRepository);
        BookingStateHandler ownerCurrentHandler = new OwnerCurrentBookingsHandler(bookingRepository);
        BookingStateHandler ownerPastHandler = new OwnerPastBookingsHandler(bookingRepository);
        BookingStateHandler ownerFutureHandler = new OwnerFutureBookingsHandler(bookingRepository);
        BookingStateHandler ownerWaitingHandler = new OwnerWaitingBookingsHandler(bookingRepository);
        BookingStateHandler ownerRejectedHandler = new OwnerRejectedBookingsHandler(bookingRepository);

        ownerAllHandler.setNext(ownerCurrentHandler)
                .setNext(ownerPastHandler)
                .setNext(ownerFutureHandler)
                .setNext(ownerWaitingHandler)
                .setNext(ownerRejectedHandler);

        this.ownerStateHandler = ownerAllHandler;
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
        List<Booking> bookings = bookerStateHandler.handle(state, userId, now);

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
        List<Booking> bookings = ownerStateHandler.handle(state, userId, now);

        return bookings.stream()
                .map(bookingMapper::toBookingDto)
                .collect(Collectors.toList());
    }
}