package ru.practicum.shareit.item.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractRepository<T, K> {
    protected final Map<K, T> entities = new HashMap<>();
    protected Long nextId = 1L;

    public List<T> findAll() {
        return new ArrayList<>(entities.values());
    }

    public Optional<T> findById(K id) {
        return entities.containsKey(id) ? Optional.of(entities.get(id)) : Optional.empty();
    }

    public void delete(K id) {
        entities.remove(id);
    }

    protected abstract void setEntityId(T entity, Long id);

    protected abstract K getEntityId(T entity);
}