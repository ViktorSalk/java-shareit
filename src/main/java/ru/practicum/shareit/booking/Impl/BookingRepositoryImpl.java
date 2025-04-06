package ru.practicum.shareit.booking.Impl;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.repository.AbstractRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingRepositoryImpl extends AbstractRepository<Booking, Long> implements BookingRepository {

    @Override
    public Booking save(Booking booking) {
        if (booking.getId() == null) {
            setEntityId(booking, nextId);
            nextId++;
            entities.put(booking.getId(), booking);
        } else {
            entities.put(booking.getId(), booking);
        }
        return booking;
    }

    @Override
    public List<Booking> findAllByBookerId(Long bookerId) {
        return entities.values().stream()
                .filter(booking -> booking.getBooker().getId().equals(bookerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByBookerIdAndStartBeforeAndEndAfter(Long bookerId, LocalDateTime start, LocalDateTime end) {
        return entities.values().stream()
                .filter(booking -> booking.getBooker().getId().equals(bookerId))
                .filter(booking -> booking.getStart().isBefore(start))
                .filter(booking -> booking.getEnd().isAfter(end))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByBookerIdAndEndBefore(Long bookerId, LocalDateTime end) {
        return entities.values().stream()
                .filter(booking -> booking.getBooker().getId().equals(bookerId))
                .filter(booking -> booking.getEnd().isBefore(end))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByBookerIdAndStartAfter(Long bookerId, LocalDateTime start) {
        return entities.values().stream()
                .filter(booking -> booking.getBooker().getId().equals(bookerId))
                .filter(booking -> booking.getStart().isAfter(start))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByBookerIdAndStatus(Long bookerId, BookingStatus status) {
        return entities.values().stream()
                .filter(booking -> booking.getBooker().getId().equals(bookerId))
                .filter(booking -> booking.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByItemOwnerId(Long ownerId) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getOwner().getId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByItemOwnerIdAndStartBeforeAndEndAfter(Long ownerId, LocalDateTime start, LocalDateTime end) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getOwner().getId().equals(ownerId))
                .filter(booking -> booking.getStart().isBefore(start))
                .filter(booking -> booking.getEnd().isAfter(end))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByItemOwnerIdAndEndBefore(Long ownerId, LocalDateTime end) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getOwner().getId().equals(ownerId))
                .filter(booking -> booking.getEnd().isBefore(end))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByItemOwnerIdAndStartAfter(Long ownerId, LocalDateTime start) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getOwner().getId().equals(ownerId))
                .filter(booking -> booking.getStart().isAfter(start))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findAllByItemOwnerIdAndStatus(Long ownerId, BookingStatus status) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getOwner().getId().equals(ownerId))
                .filter(booking -> booking.getStatus().equals(status))
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findLastBookingForItem(Long itemId, LocalDateTime now) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getId().equals(itemId))
                .filter(booking -> booking.getStart().isBefore(now))
                .filter(booking -> booking.getStatus() == BookingStatus.APPROVED)
                .sorted((b1, b2) -> b2.getStart().compareTo(b1.getStart()))
                .limit(1)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findNextBookingForItem(Long itemId, LocalDateTime now) {
        return entities.values().stream()
                .filter(booking -> booking.getItem().getId().equals(itemId))
                .filter(booking -> booking.getStart().isAfter(now))
                .filter(booking -> booking.getStatus() == BookingStatus.APPROVED)
                .sorted((b1, b2) -> b1.getStart().compareTo(b2.getStart()))
                .limit(1)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasUserBookedItem(Long userId, Long itemId, LocalDateTime now) {
        return entities.values().stream()
                .anyMatch(booking ->
                        booking.getBooker().getId().equals(userId) &&
                                booking.getItem().getId().equals(itemId) &&
                                booking.getEnd().isBefore(now) &&
                                booking.getStatus() == BookingStatus.APPROVED
                );
    }

    @Override
    protected void setEntityId(Booking entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected Long getEntityId(Booking entity) {
        return entity.getId();
    }
}