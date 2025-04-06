package ru.practicum.shareit.item.Impl;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.repository.AbstractRepository;
import ru.practicum.shareit.item.repository.CommentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CommentRepositoryImpl extends AbstractRepository<Comment, Long> implements CommentRepository {

    @Override
    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            setEntityId(comment, nextId);
            nextId++;
        }
        entities.put(comment.getId(), comment);
        return comment;
    }

    @Override
    public List<Comment> findByItemId(Long itemId) {
        return entities.values().stream()
                .filter(comment -> comment.getItem().getId().equals(itemId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Comment> findByItemIdIn(List<Long> itemIds) {
        return entities.values().stream()
                .filter(comment -> itemIds.contains(comment.getItem().getId()))
                .collect(Collectors.toList());
    }

    @Override
    protected void setEntityId(Comment entity, Long id) {
        entity.setId(id);
    }

    @Override
    protected Long getEntityId(Comment entity) {
        return entity.getId();
    }
}