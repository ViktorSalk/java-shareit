package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.controller.BookingController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
@ActiveProfiles("test")
public class BookingControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private BookingDto bookingDto;
    private final Long userId = 1L;
    private final String userIdHeader = "X-Sharer-User-Id";
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        ItemDto itemDto = ItemDto.builder()
                .id(1L)
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();

        UserDto userDto = UserDto.builder()
                .id(2L)
                .name("Test User")
                .email("test@test.com")
                .build();

        bookingDto = BookingDto.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .item(itemDto)
                .booker(userDto)
                .status(BookingStatus.WAITING)
                .build();
    }

    @Test // Тест на создание бронирования
    @DisplayName("POST /bookings должен создавать новое бронирование")
    void createBooking() throws Exception {
        when(bookingService.create(any(BookingDto.class), anyLong())).thenReturn(bookingDto);

        mockMvc.perform(post("/bookings")
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingDto.getId().intValue())))
                .andExpect(jsonPath("$.status", is(bookingDto.getStatus().toString())));
    }

    @Test // Тест на подтверждение бронирования
    @DisplayName("PATCH /bookings/{bookingId} должен подтверждать бронирование")
    void approveBooking() throws Exception {
        BookingDto approvedBooking = BookingDto.builder()
                .id(bookingDto.getId())
                .start(bookingDto.getStart())
                .end(bookingDto.getEnd())
                .item(bookingDto.getItem())
                .booker(bookingDto.getBooker())
                .status(BookingStatus.APPROVED)
                .build();

        when(bookingService.approve(anyLong(), anyLong(), anyBoolean())).thenReturn(approvedBooking);

        mockMvc.perform(patch("/bookings/1")
                        .header(userIdHeader, userId)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(approvedBooking.getId().intValue())))
                .andExpect(jsonPath("$.status", is(approvedBooking.getStatus().toString())));
    }

    @Test // Тест на получение бронирования по ID
    @DisplayName("GET /bookings/{bookingId} должен возвращать бронирование по ID")
    void getBookingById() throws Exception {
        when(bookingService.getById(anyLong(), anyLong())).thenReturn(bookingDto);

        mockMvc.perform(get("/bookings/1")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(bookingDto.getId().intValue())))
                .andExpect(jsonPath("$.status", is(bookingDto.getStatus().toString())));
    }

    @Test // Тест на получение бронирований владельца
    @DisplayName("GET /bookings/owner должен возвращать бронирования владельца")
    void getAllBookingsByOwner() throws Exception {
        List<BookingDto> bookings = Arrays.asList(bookingDto);
        when(bookingService.getAllByOwner(anyLong(), anyString(), anyInt(), anyInt())).thenReturn(bookings);

        mockMvc.perform(get("/bookings/owner")
                        .header(userIdHeader, userId)
                        .param("state", "ALL")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(bookingDto.getId().intValue())))
                .andExpect(jsonPath("$[0].status", is(bookingDto.getStatus().toString())));
    }
}