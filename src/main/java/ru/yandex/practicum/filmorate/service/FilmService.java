package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private final UserService userService;
    private final InMemoryFilmStorage filmStorage;

    public void addLike(Integer filmId, Integer userId) {
        Film film = getFilmOrThrow(filmId);
        userService.getUserOrThrow(userId);

        if (!film.getLikes().add(userId)) {
            log.warn("Пользователь ID = {} уже лайкнул фильм ID = {}", userId, filmId);
        } else {
            log.info("Лайк добавлен: фильм ID = {}, пользователь ID = {}", filmId, userId);
        }
    }

    public void deleteLike(Integer filmId, Integer userId) {
        Film film = getFilmOrThrow(filmId);
        userService.getUserOrThrow(userId);

        if (!film.getLikes().remove(userId)) {
            log.warn("Пользователь ID = {} не ставил лайк фильму ID = {}", userId, filmId);
        } else {
            log.info("Лайк удален: фильм ID = {}, пользователь ID = {}", filmId, userId);
        }
    }

    public List<Film> mostPopularFilms(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Количество фильмов должно быть больше 0");
        }
        return filmStorage.getAllFilms().stream()
                .sorted(Comparator.comparingInt(Film::getLikesCount).reversed())
                .limit(size)
                .collect(Collectors.toList());
    }

    private Film getFilmOrThrow(Integer filmId) {
        Film film = filmStorage.getFilmById(filmId);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
        return film;
    }
}