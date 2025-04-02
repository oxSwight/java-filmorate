package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {
    private final InMemoryUserStorage userStorage;

    public UserService(InMemoryUserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public void addFriend(Integer userId, Integer friendId) {
        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (!user.getFriends().add(friend.getId())) {
            log.warn("Пользователь ID = {} уже добавил в друзья пользователя ID = {}", userId, friendId);
        } else {
            friend.getFriends().add(user.getId());
            log.info("Добавлен друг: {} -> {}", userId, friendId);
        }
    }

    public void deleteFriend(Integer userId, Integer friendId) {
        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (!user.getFriends().remove(friend.getId())) {
            log.warn("Пользователь ID = {} не был другом пользователя ID = {}", userId, friendId);
        } else {
            friend.getFriends().remove(user.getId());
            log.info("Удален друг: {} -> {}", userId, friendId);
        }
    }

    public List<User> findAllFriends(Integer userId) {
        User user = getUserOrThrow(userId);
        List<User> friends = getUsersByIds(user.getFriends());
        log.info("Список друзей пользователя ID = {}: {}", userId, friends);
        return friends;
    }

    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        User user = getUserOrThrow(userId);
        User otherUser = getUserOrThrow(otherId);

        Set<Integer> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(otherUser.getFriends());

        List<User> commonFriends = getUsersByIds(commonFriendIds);
        log.info("Общие друзья между {} и {}: {}", userId, otherId, commonFriends);
        return commonFriends;
    }

    public User findUser(Integer userId) {
        return getUserOrThrow(userId);
    }

    private User getUserOrThrow(Integer userId) {
        User user = userStorage.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        return user;
    }

    private List<User> getUsersByIds(Set<Integer> ids) {
        return ids.stream()
                .map(userStorage::getUserById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
