package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new HashMap<>();

    @Override
    public Collection<User> getAllUsers() {
        return users.values();
    }

    @Override
    public User create(User user) {
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }
        user.setId(getNextId());
        user.setBirthday(user.getBirthday());
        users.put(user.getId(), user);
        log.info("Новый пользователь успешно добавлен: ID = {}, Логин = {}, Имя = {}, Почта = {}, День рождения = {}",
                user.getId(), user.getLogin(), user.getName(), user.getEmail(), user.getBirthday());
        return user;
    }

    @Override
    public User update(User newUser) {
        if (newUser.getId() == null) {
            log.warn("Не указан Id пользователя");
            throw new ValidationException("Id пользователя должен быть указан");
        }
        if (users.containsKey(newUser.getId())) {
            User oldUser = users.get(newUser.getId());
            log.info("Пользователь для обновления данных найден: ID = {}, Логин = {}, Имя = {}, Почта = {}, День рождения = {}",
                    oldUser.getId(), oldUser.getLogin(), oldUser.getName(), oldUser.getEmail(), oldUser.getBirthday());
            if (newUser.getName() == null || newUser.getName().isEmpty()) {
                oldUser.setName(newUser.getLogin());
            }
            oldUser.setName(newUser.getName());
            oldUser.setEmail(newUser.getEmail());
            oldUser.setLogin(newUser.getLogin());
            oldUser.setBirthday(newUser.getBirthday());
            log.info("Данные пользователя обновлены: ID = {}, Логин = {}, Имя = {}, Почта = {}, День рождения = {}",
                    oldUser.getId(), oldUser.getLogin(), oldUser.getName(), oldUser.getEmail(), oldUser.getBirthday());
            return oldUser;
        }
        log.warn("Ошибка при обновлении пользователя: пользователь с ID {} не найден", newUser.getId());
        throw new NotFoundException("Пользователь с id = " + newUser.getId() + " не найден");
    }

    public User getUserById(Integer userId) {
        return users.get(userId);
    }

    private int getNextId() {
        int currentMaxId = users.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}