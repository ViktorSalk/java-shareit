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
    @Mapping(target = "lastBooking", expression = "java(null)")
    @Mapping(target = "nextBooking", expression = "java(null)")
    @Mapping(target = "comments", expression = "java(new java.util.ArrayList<>())")
    ItemDto toItemDto(Item item);

    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "request", ignore = true)
    Item toItem(ItemDto itemDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "request", ignore = true)
    Item updateItemFields(@MappingTarget Item targetItem, ItemDto sourceItemDto);
}