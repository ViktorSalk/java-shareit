package ru.practicum.shareit.user.Impl;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.repository.AbstractRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

@Component
public class UserRepositoryImpl extends AbstractRepository<User, Long> implements UserRepository {

    @Override
    public User create(User user) {
        setEntityId(user, nextId);
        nextId++;
        entities.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        entities.put(user.getId(), user);
        return user;
    }

    @Override
    protected void setEntityId(User entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected Long getEntityId(User entity) {
        return entity.getId();
    }
}