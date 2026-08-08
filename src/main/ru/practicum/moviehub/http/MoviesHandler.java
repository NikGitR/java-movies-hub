package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private static final long MIN_MOVIE_ID = 1;
    private static final int MIN_RELEASE_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    private final MoviesStore store;
    private final Gson gson;

    public MoviesHandler(MoviesStore store, Gson gson) {
        this.store = store;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
            return;
        }

        exchange.getResponseHeaders().set("Allow", "GET, POST");
        sendError(exchange, 405, "Метод не поддерживается");
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();

        if (query == null || query.isBlank()) {
            sendJson(exchange, 200, gson.toJson(store.findAll()));
            return;
        }

        Integer year = parseYear(query);

        if (year == null) {
            sendError(
                    exchange,
                    400,
                    "Ожидается параметр year в формате YYYY"
            );
            return;
        }

        Optional<Movie> movies = store.findById(year);
        sendJson(exchange, 200, gson.toJson(movies));
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        MovieRequest request;

        try {
            String requestBody = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            request = gson.fromJson(requestBody, MovieRequest.class);
        } catch (JsonParseException exception) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        String validationError = validate(request);

        if (validationError != null) {
            sendError(exchange, 400, validationError);
            return;
        }

        Movie movie = new Movie(
                request.id,
                request.title,
                request.releaseYear
        );

        try {
            store.add(movie);
        } catch (IllegalArgumentException exception) {
            sendError(exchange, 409, exception.getMessage());
            return;
        }

        exchange.getResponseHeaders()
                .set("Location", "/movies/" + movie.getId());

        sendJson(exchange, 201, gson.toJson(movie));
    }

    private String validate(MovieRequest request) {
        if (request == null) {
            return "Тело запроса не должно быть пустым";
        }

        if (request.id < MIN_MOVIE_ID) {
            return "Идентификатор должен быть положительным";
        }

        if (request.title == null || request.title.isBlank()) {
            return "Название фильма не должно быть пустым";
        }

        if (request.title.length() > MAX_TITLE_LENGTH) {
            return "Название фильма не должно превышать "
                    + MAX_TITLE_LENGTH + " символов";
        }

        if (request.releaseYear < MIN_RELEASE_YEAR) {
            return "Год выпуска не может быть меньше "
                    + MIN_RELEASE_YEAR;
        }

        return null;
    }

    private Integer parseYear(String query) {
        String[] parameters = query.split("&");

        if (parameters.length != 1) {
            return null;
        }

        String[] pair = parameters[0].split("=", -1);

        if (pair.length != 2 || !"year".equals(pair[0])) {
            return null;
        }

        try {
            return Integer.parseInt(pair[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendError(
            HttpExchange exchange,
            int status,
            String message
    ) throws IOException {
        sendJson(
                exchange,
                status,
                gson.toJson(new ErrorResponse(message))
        );
    }

    private static class MovieRequest {
        private long id;
        private String title;
        private int releaseYear;
    }
}
