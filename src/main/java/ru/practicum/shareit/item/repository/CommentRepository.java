package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Comment;

import java.util.List;

public interface CommentRepository {
    Comment save(Comment comment);

    List<Comment> findByItemId(Long itemId);

    List<Comment> findByItemIdIn(List<Long> itemIds);
}