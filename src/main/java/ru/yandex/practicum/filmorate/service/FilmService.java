package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private final UserService userService;
    private final FilmDbStorage filmStorage;

    public FilmService(UserService userService, FilmDbStorage filmStorage) {
        this.userService = userService;
        this.filmStorage = filmStorage;
    }

    public void addLike(Integer filmId, Integer userId) {
        Film film = getFilmByIdOrThrow(filmId);
        userService.findUser(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, film.getName());
    }

    public void deleteLike(Integer filmId, Integer userId) {
        Film film = getFilmByIdOrThrow(filmId);
        userService.findUser(userId);
        filmStorage.deleteLike(filmId, userId);
        log.info("Пользователь {} убрал лайк у фильма {}", userId, film.getName());
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film addNewFilm(Film film) {
        log.info("Добавление нового фильма: {}", film.getName());
        return filmStorage.addNewFilm(film);
    }

    public Film updateFilm(Film newFilm) {
        log.info("Обновление фильма с id={}", newFilm.getId());
        return filmStorage.updateFilm(newFilm);
    }

    public List<Film> mostPopularFilms(int size) {
        return filmStorage.findMostPopularFilms(size);
    }


    public Film getFilmByIdOrThrow(Integer filmId) {
        return filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + filmId + " не найден"));
    }
}