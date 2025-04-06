package ru.practicum.shareit.booking.repository;

import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);

    Optional<Booking> findById(Long id);

    List<Booking> findAllByBookerId(Long bookerId);

    List<Booking> findAllByBookerIdAndStartBeforeAndEndAfter(Long bookerId, LocalDateTime start, LocalDateTime end);

    List<Booking> findAllByBookerIdAndEndBefore(Long bookerId, LocalDateTime end);

    List<Booking> findAllByBookerIdAndStartAfter(Long bookerId, LocalDateTime start);

    List<Booking> findAllByBookerIdAndStatus(Long bookerId, BookingStatus status);

    List<Booking> findAllByItemOwnerId(Long ownerId);

    List<Booking> findAllByItemOwnerIdAndStartBeforeAndEndAfter(Long ownerId, LocalDateTime start, LocalDateTime end);

    List<Booking> findAllByItemOwnerIdAndEndBefore(Long ownerId, LocalDateTime end);

    List<Booking> findAllByItemOwnerIdAndStartAfter(Long ownerId, LocalDateTime start);

    List<Booking> findAllByItemOwnerIdAndStatus(Long ownerId, BookingStatus status);

    List<Booking> findLastBookingForItem(Long itemId, LocalDateTime now);

    List<Booking> findNextBookingForItem(Long itemId, LocalDateTime now);

    boolean hasUserBookedItem(Long userId, Long itemId, LocalDateTime now);
}