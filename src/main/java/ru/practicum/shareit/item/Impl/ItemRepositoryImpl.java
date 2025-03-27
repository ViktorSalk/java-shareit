package ru.practicum.shareit.item.Impl;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.AbstractRepository;
import ru.practicum.shareit.item.repository.ItemRepository;

@Component
public class ItemRepositoryImpl extends AbstractRepository<Item, Long> implements ItemRepository {

    @Override
    public Item create(Item item) {
        setEntityId(item, nextId);
        nextId++;
        entities.put(item.getId(), item);
        return item;
    }

    @Override
    public Item update(Item item) {
        entities.put(item.getId(), item);
        return item;
    }

    @Override
    protected void setEntityId(Item entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected Long getEntityId(Item entity) {
        return entity.getId();
    }
}