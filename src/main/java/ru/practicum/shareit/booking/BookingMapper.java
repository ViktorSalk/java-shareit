package ru.practicum.shareit.booking;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.user.UserMapper;

@Mapper(componentModel = "spring",
        uses = {ItemMapper.class, UserMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BookingMapper {

    @Mapping(target = "item", source = "item")
    @Mapping(target = "booker", source = "booker")
    BookingDto toBookingDto(Booking booking);

    @Mapping(target = "id", ignore = true)
    Booking toBooking(BookingDto bookingDto);

    Booking updateBookingFields(@MappingTarget Booking targetBooking, BookingDto sourceBookingDto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "bookerId", source = "booker.id")
    BookingShortDto toBookingShortDto(Booking booking);
}