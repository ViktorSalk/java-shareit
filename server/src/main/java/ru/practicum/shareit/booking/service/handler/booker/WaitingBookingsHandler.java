package ru.practicum.shareit.booking.service.handler.booker;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.handler.AbstractBookingStateHandler;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Qualifier("booker")
@Order(5)
public class WaitingBookingsHandler extends AbstractBookingStateHandler {

    public WaitingBookingsHandler(BookingRepository bookingRepository) {
        super(bookingRepository);
    }

    @Override
    public boolean canHandle(BookingState state) {
        return state == BookingState.WAITING;
    }

    @Override
    public List<Booking> getBookings(Long userId, LocalDateTime now) {
        return bookingRepository.findAllByBookerIdAndStatus(userId, BookingStatus.WAITING);
    }

    @Override
    public List<Booking> getBookings(Long userId, LocalDateTime now, PageRequest pageRequest) {
        if (pageRequest == null) {
            return getBookings(userId, now);
        }
        return bookingRepository.findAllByBookerIdAndStatus(userId, BookingStatus.WAITING, pageRequest);
    }
}