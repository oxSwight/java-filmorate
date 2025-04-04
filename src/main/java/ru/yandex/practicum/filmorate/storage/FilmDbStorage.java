package ru.yandex.practicum.filmorate.storage;

import lombok.Data;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.interfaces.FilmStorage;
import ru.yandex.practicum.filmorate.storage.mappers.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mappers.GenreRowMapper;

import java.sql.*;
import java.sql.Date;
import java.util.*;

@Data
@Component
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final GenreRowMapper genreRowMapper;
    private static final String INSERT_NEW_FILM = "INSERT INTO film(name, description, duration,release_date,rating_id) " +
            "VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_FILM_GENRE = "INSERT INTO film_genre(film_id, genre_id) VALUES (?, ?)";
    private static final String DELETE_FILM_GENRE = "DELETE FROM film_genre WHERE film_id = ?";
    private static final String UPDATE_FILM_GENRE = "INSERT INTO film_genre (film_id, genre_id) VALUES (?, ?)";
    private static final String FIND_GENRES_FILM_BY_ID = """
            SELECT g.genre_id, g.name
            FROM film_genre AS fg
            JOIN genre AS g ON fg.genre_id = g.genre_ID
            WHERE fg.film_id = ?
            """;
    private static final String FIND_GENRES_FILMS = """
            SELECT fg.film_id, g.genre_id, g.name
            FROM film_genre fg
            JOIN genre g ON fg.genre_id = g.genre_id
            """;
    private static final String ADD_LIKE_FILM = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
    private static final String DELETE_LIKE_FILM = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
    private static final String GET_LIKES_BY_FILM_ID = "SELECT user_id FROM likes WHERE film_id = ?";
    private static final String FIND_LIKES_OF_FILMS = """
            SELECT l.film_id, l.user_id
            FROM likes AS l
            JOIN film AS f ON f.film_id = l.film_id
            """;
    private static final String FIND_ALL = """
            SELECT f.*, r.rating_name
            FROM film f
            JOIN rating r ON f.rating_id = r.rating_id
            """;
    private static final String FIND_BY_ID = """
                SELECT f.*, r.rating_name
                FROM film f
                LEFT JOIN rating r ON f.rating_id = r.rating_id
                WHERE f.film_id = ?
            """;
    private static final String DELETE = "DELETE FROM film WHERE film_id = ?";
    private static final String UPDATE = "UPDATE film SET name = ?, description = ?, duration = ?, " +
            "release_date = ?, rating_id = ? WHERE film_id = ?";
    private static final String FIND_MOST_POPULAR_FILMS = """
            SELECT f.film_id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       f.rating_id,
                       r.rating_name,
                       COUNT(l.user_id) AS likes_film
                FROM film AS f
                JOIN rating r ON f.rating_id = r.rating_id
                LEFT JOIN likes AS l ON f.film_id = l.film_id
                GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.rating_id, r.rating_name
                ORDER BY likes_film DESC
            """;

    @Override
    public List<Film> getAllFilms() {
        List<Film> films = jdbcTemplate.query(FIND_ALL, filmRowMapper);
        Map<Integer, Set<Genre>> filmGenres = new HashMap<>();
        jdbcTemplate.query(FIND_GENRES_FILMS,
                (rs) -> {
                    int filmId = rs.getInt("film_id");
                    Genre genre = new Genre();
                    genre.setId(rs.getInt("genre_id"));
                    genre.setName(rs.getString("name"));
                    filmGenres.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
                });
        Map<Integer, Set<Integer>> filmLikes = new HashMap<>();
        jdbcTemplate.query(FIND_LIKES_OF_FILMS,
                (rs) -> {
                    int filmId = rs.getInt("film_id");
                    filmLikes.computeIfAbsent(filmId, k -> new HashSet<>()).add(rs.getInt("user_id"));
                });
        for (Film film : films) {
            int id = film.getId();
            film.setGenres(filmGenres.getOrDefault(id, new HashSet<>()));
            film.setLikes(filmLikes.getOrDefault(id, new HashSet<>()));
        }
        return films;
    }

    @Override
    public Film addNewFilm(Film film) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection
                    .prepareStatement(INSERT_NEW_FILM, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setInt(3, film.getDuration());
            ps.setDate(4, Date.valueOf(film.getReleaseDate()));
            if (film.getRating() == null) {
                throw new RuntimeException("Поле rating (mpa) обязательно для заполнения.");
            }
            ps.setInt(5, film.getRating().getId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            film.setId(key.intValue());
            if (film.getGenres() != null) {
                jdbcTemplate.batchUpdate(INSERT_FILM_GENRE,
                        new BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                Genre genre = new ArrayList<>(film.getGenres()).get(i);
                                ps.setInt(1, film.getId());
                                ps.setInt(2, genre.getId());
                            }

                            @Override
                            public int getBatchSize() {
                                return film.getGenres().size();
                            }
                        }
                );
            }
            return film;
        } else {
            throw new InternalServerException("Не удалось сохранить данные");
        }
    }

    @Override
    public Film updateFilm(Film newFilm) {
        int rowsUpdated = jdbcTemplate.update(UPDATE, newFilm.getName(),
                newFilm.getDescription(), newFilm.getDuration(), newFilm.getReleaseDate(), newFilm.getRating().getId(),
                newFilm.getId());
        if (rowsUpdated == 0) {
            throw new NotFoundException("Фильм с " + newFilm.getId() + " не найден.");
        }
        jdbcTemplate.update(DELETE_FILM_GENRE, newFilm.getId());
        if (newFilm.getGenres() != null) {
            jdbcTemplate.batchUpdate(UPDATE_FILM_GENRE,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            Genre genre = new ArrayList<>(newFilm.getGenres()).get(i);
                            ps.setInt(1, newFilm.getId());
                            ps.setInt(2, genre.getId());
                        }

                        @Override
                        public int getBatchSize() {
                            return newFilm.getGenres().size();
                        }
                    }
            );
        }
        return newFilm;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        try {
            Film result = jdbcTemplate.queryForObject(FIND_BY_ID, filmRowMapper, id);
            if (result != null) {
                result.setLikes(getLikesByFilmId(id));
                result.setGenres(getGenresByFilmId(id));
            }
            return Optional.ofNullable(result);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public List<Film> findMostPopularFilms() {
        return jdbcTemplate.query(FIND_MOST_POPULAR_FILMS, filmRowMapper);
    }

    @Override
    public void addLike(Integer filmId, Integer userID) {
        jdbcTemplate.update(ADD_LIKE_FILM, filmId, userID);
    }

    @Override
    public void deleteLike(Integer filmId, Integer userID) {
        jdbcTemplate.update(DELETE_LIKE_FILM, filmId, userID);
    }

    @Override
    public Set<Integer> getLikesByFilmId(Integer filmId) {
        return new HashSet<>(jdbcTemplate.query(GET_LIKES_BY_FILM_ID,
                (rs, rowNum) -> rs.getInt("user_id"), filmId));
    }

    @Override
    public Set<Genre> getGenresByFilmId(Integer filmId) {
        return new HashSet<>(jdbcTemplate.query(FIND_GENRES_FILM_BY_ID, genreRowMapper, filmId));
    }
}