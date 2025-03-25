package ru.practicum.shareit.item;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ItemMapper {
    @Mapping(target = "request", expression = "java(item.getRequest() != null ? item.getRequest() : null)")
    ItemDto toItemDto(Item item);
    Item toItem(ItemDto itemDto);
    Item updateItemFields(@MappingTarget Item targetItem, ItemDto sourceItemDto);
}