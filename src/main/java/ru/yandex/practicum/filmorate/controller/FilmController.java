package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;


import java.util.Collection;
import java.util.List;


@RestController
@RequestMapping("/films")
public class FilmController {

    private final FilmService filmService;

    public FilmController( FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping("/popular")
    public List<Film> mostPopularFilms(@RequestParam(defaultValue = "10") int count) {
        return filmService.mostPopularFilms(count);
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable Integer id) {
        return filmService.getFilmByIdOrThrow(id);
    }

    @GetMapping
    public Collection<Film> getAllFilms() {
        return filmService.getAllFilms();
    }

    @PostMapping
    public Film addNewFilm(@Valid @RequestBody Film film) {
        return filmService.addNewFilm(film);
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film newFilm) {
        return filmService.updateFilm(newFilm);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Integer id, @PathVariable Integer userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Integer id, @PathVariable Integer userId) {
        filmService.deleteLike(id, userId);
    }

}