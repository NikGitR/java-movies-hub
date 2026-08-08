package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Optional;

public class MovieByIdHandler extends BaseHttpHandler {
    private static final String PATH_PREFIX = "/movies/";

    private final MoviesStore store;
    private final Gson gson;

    public MovieByIdHandler(MoviesStore store, Gson gson) {
        this.store = store;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Long movieId = extractMovieId(exchange);

        if (movieId == null) {
            sendError(exchange, 400, "Некорректный идентификатор фильма");
            return;
        }

        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange, movieId);
            return;
        }

        if ("DELETE".equalsIgnoreCase(method)) {
            handleDelete(exchange, movieId);
            return;
        }

        exchange.getResponseHeaders().set("Allow", "GET, DELETE");
        sendError(exchange, 405, "Метод не поддерживается");
    }

    private void handleGet(HttpExchange exchange,
                           long movieId) throws IOException {
        Optional<Movie> movie = store.findById(movieId);

        if (movie.isEmpty()) {
            sendError(exchange, 404,
                    "Фильм с id=" + movieId + " не найден");
            return;
        }

        sendJson(exchange, 200, gson.toJson(movie.get()));
    }

    private void handleDelete(HttpExchange exchange,
                              long movieId) throws IOException {
        boolean deleted = store.delete(movieId);

        if (!deleted) {
            sendError(exchange, 404,
                    "Фильм с id=" + movieId + " не найден");
            return;
        }

        sendNoContent(exchange);
    }

    private Long extractMovieId(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();

        if (!path.startsWith(PATH_PREFIX)) {
            return null;
        }

        String idPart = path.substring(PATH_PREFIX.length());


        if (idPart.isBlank() || idPart.contains("/")) {
            return null;
        }

        try {
            long id = Long.parseLong(idPart);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendError(HttpExchange exchange,
                           int status,
                           String message) throws IOException {
        sendJson(
                exchange,
                status,
                gson.toJson(new ErrorResponse(message))
        );
    }
}
