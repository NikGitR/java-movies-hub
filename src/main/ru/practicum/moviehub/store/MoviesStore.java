package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    private final Map<Long, Movie> movies = new LinkedHashMap<>();

    public void add(Movie movie) {
        if (movies.putIfAbsent(movie.getId(), movie) != null) {
            throw new IllegalArgumentException(
                    "Фильм с id=" + movie.getId() + " уже существует");
        }
    }

    public List<Movie> findByReleaseYear(int releaseYear) {
        return movies.values().stream()
                .filter(movie ->
                        movie.getReleaseYear() == releaseYear)
                .toList();
    }

    public List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> findById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public void clear() {
        movies.clear();
    }
}
