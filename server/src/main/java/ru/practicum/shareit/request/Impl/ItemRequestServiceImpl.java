package ru.practicum.shareit.request.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemRequestMapper itemRequestMapper;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemRequestDto create(ItemRequestDto itemRequestDto, Long userId) {
        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().isBlank()) {
            throw new ShareItException.BadRequestException("Описание запроса не может быть пустым");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Пользователь не найден"));

        ItemRequest itemRequest = ItemRequest.builder()
                .description(itemRequestDto.getDescription())
                .requestor(user)
                .created(LocalDateTime.now())
                .build();

        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);
        return itemRequestMapper.toItemRequestDto(savedRequest);
    }

    @Override
    public List<ItemRequestDto> getAllByRequestor(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ShareItException.NotFoundException("Пользователь не найден");
        }

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);
        return getItemRequestDtosWithItems(requests);
    }

    @Override
    public List<ItemRequestDto> getAll(Long userId, int from, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ShareItException.NotFoundException("Пользователь не найден");
        }

        int page = from / size;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("created").descending());
        Page<ItemRequest> requestsPage = itemRequestRepository.findAllByRequestorIdNot(userId, pageRequest);
        List<ItemRequest> requests = requestsPage.getContent();

        return getItemRequestDtosWithItems(requests);
    }

    @Override
    public ItemRequestDto getById(Long requestId, Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ShareItException.NotFoundException("Пользователь не найден");
        }

        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new ShareItException.NotFoundException("Запрос не найден"));

        List<Item> items = itemRepository.findAllByRequestId(requestId);
        List<ItemDto> itemDtos = items.stream()
                .map(itemMapper::toItemDto)
                .collect(Collectors.toList());

        ItemRequestDto requestDto = itemRequestMapper.toItemRequestDto(request);
        requestDto.setItems(itemDtos);

        return requestDto;
    }

    private List<ItemRequestDto> getItemRequestDtosWithItems(List<ItemRequest> requests) {
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        Map<Long, List<Item>> itemsByRequestId = itemRepository.findAllByRequestIdIn(requestIds).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));

        return requests.stream()
                .map(request -> {
                    ItemRequestDto dto = itemRequestMapper.toItemRequestDto(request);
                    List<Item> items = itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList());
                    dto.setItems(items.stream()
                            .map(itemMapper::toItemDto)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}