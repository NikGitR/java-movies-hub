package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.ValidationErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class MoviesHandler extends BaseHttpHandler {
    private static final long MIN_MOVIE_ID = 1;
    private static final int MIN_RELEASE_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;

    private final MoviesStore store;


    public MoviesHandler(MoviesStore store, Gson gson) {
        super(gson);
        this.store = store;
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

        OptionalInt parsedYear = parseYear(query);

        if (parsedYear.isEmpty()) {
            sendError(
                    exchange,
                    400,
                    "Ожидается параметр year в формате YYYY"
            );
            return;
        }

        int year = parsedYear.getAsInt();

        List<Movie> movies = store.findByReleaseYear(year);
        sendJson(exchange, 200, gson.toJson(movies));
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        Movie movie;

        try {
            String requestBody = new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            movie = gson.fromJson(requestBody, Movie.class);
        } catch (JsonParseException exception) {
            sendError(exchange, 400, "Некорректный JSON");
            return;
        }

        List<String> validationErrors = validate(movie);

        if (!validationErrors.isEmpty()) {
            sendJson(
                    exchange,
                    400,
                    gson.toJson(new ValidationErrorResponse(validationErrors))
            );
            return;
        }

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

    private List<String> validate(Movie movie) {
        List<String> errors = new ArrayList<>();

        if (movie == null) {
            errors.add("Тело запроса не должно быть пустым");
            return errors;
        }

        if (movie.getId() < MIN_MOVIE_ID) {
            errors.add("Идентификатор должен быть положительным");
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errors.add("Название фильма не должно быть пустым");
        } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            errors.add(
                    "Название фильма не должно превышать "
                            + MAX_TITLE_LENGTH + " символов"
            );
        }

        if (movie.getReleaseYear() < MIN_RELEASE_YEAR) {
            errors.add(
                    "Год выпуска не может быть меньше "
                            + MIN_RELEASE_YEAR
            );
        }

        return errors;
    }

    private OptionalInt parseYear(String query) {
        String[] parameters = query.split("&");

        if (parameters.length != 1) {
            return OptionalInt.empty();
        }

        String[] pair = parameters[0].split("=", -1);

        if (pair.length != 2 || !"year".equals(pair[0])) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.parseInt(pair[1]));
        } catch (NumberFormatException exception) {
            return OptionalInt.empty();
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

}
