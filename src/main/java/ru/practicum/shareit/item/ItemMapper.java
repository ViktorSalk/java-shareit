package ru.practicum.shareit.item;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {CommentMapper.class})
public interface ItemMapper {

    @Mapping(target = "request", expression = "java(item.getRequest() != null ? item.getRequest() : null)")
    @Mapping(target = "lastBooking", ignore = true)
    @Mapping(target = "nextBooking", ignore = true)
    @Mapping(target = "comments", ignore = true)
    ItemDto toItemDto(Item item);

    Item toItem(ItemDto itemDto);

    Item updateItemFields(@MappingTarget Item targetItem, ItemDto sourceItemDto);
}