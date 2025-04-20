package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.request.controller.ItemRequestController;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
public class ItemRequestControllerMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService itemRequestService;

    private ItemRequestDto itemRequestDto;
    private final Long userId = 1L;
    private final String userIdHeader = "X-Sharer-User-Id";

    @BeforeEach
    void setUp() {
        UserDto requestor = UserDto.builder()
                .id(userId)
                .name("Requestor")
                .email("requestor@test.com")
                .build();

        itemRequestDto = ItemRequestDto.builder()
                .id(1L)
                .description("Test Request")
                .requestor(requestor)
                .created(LocalDateTime.now())
                .items(Collections.emptyList())
                .build();
    }

    @Test // Тест на создание запроса
    @DisplayName("POST /requests должен создавать новый запрос")
    void createRequest() throws Exception {
        when(itemRequestService.create(any(ItemRequestDto.class), anyLong())).thenReturn(itemRequestDto);

        mockMvc.perform(post("/requests")
                        .header(userIdHeader, userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemRequestDto.getId().intValue())))
                .andExpect(jsonPath("$.description", is(itemRequestDto.getDescription())));
    }

    @Test // Тест на получение запросов пользователя
    @DisplayName("GET /requests должен возвращать запросы пользователя")
    void getAllRequestsByRequestor() throws Exception {
        List<ItemRequestDto> requests = Arrays.asList(itemRequestDto);
        when(itemRequestService.getAllByRequestor(anyLong())).thenReturn(requests);

        mockMvc.perform(get("/requests")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(itemRequestDto.getId().intValue())))
                .andExpect(jsonPath("$[0].description", is(itemRequestDto.getDescription())));
    }

    @Test // Тест на получение всех запросов
    @DisplayName("GET /requests/all должен возвращать все запросы")
    void getAllRequests() throws Exception {
        List<ItemRequestDto> requests = Arrays.asList(itemRequestDto);
        when(itemRequestService.getAll(anyLong(), anyInt(), anyInt())).thenReturn(requests);

        mockMvc.perform(get("/requests/all")
                        .header(userIdHeader, userId)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(itemRequestDto.getId().intValue())))
                .andExpect(jsonPath("$[0].description", is(itemRequestDto.getDescription())));
    }

    @Test // Тест на получение запроса по ID
    @DisplayName("GET /requests/{requestId} должен возвращать запрос по ID")
    void getRequestById() throws Exception {
        when(itemRequestService.getById(anyLong(), anyLong())).thenReturn(itemRequestDto);

        mockMvc.perform(get("/requests/1")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemRequestDto.getId().intValue())))
                .andExpect(jsonPath("$.description", is(itemRequestDto.getDescription())));
    }
}