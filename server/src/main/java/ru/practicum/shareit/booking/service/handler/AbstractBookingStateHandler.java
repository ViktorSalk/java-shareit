package ru.practicum.shareit.booking.service.handler;

import org.springframework.data.domain.PageRequest;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.handler.booker.BookingStateHandler;

import java.time.LocalDateTime;
import java.util.List;

public abstract class AbstractBookingStateHandler implements BookingStateHandler {
    protected final BookingRepository bookingRepository;

    public AbstractBookingStateHandler(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Override
    public abstract boolean canHandle(BookingState state);

    @Override
    public abstract List<Booking> getBookings(Long userId, LocalDateTime now);

    @Override
    public List<Booking> getBookings(Long userId, LocalDateTime now, PageRequest pageRequest) {
        return getBookings(userId, now);
    }
}