package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final Gson GSON = new Gson();

    private static final Type MOVIES_LIST_TYPE =
            new TypeToken<List<Movie>>() {
            }.getType();
    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 0);
        server.start();
        base = "http://localhost:" + server.getPort();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> response = getMovies();

        assertEquals(200, response.statusCode(),
                "GET /movies должен вернуть 200");
        assertEquals("application/json; charset=UTF-8",
                response.headers().firstValue("Content-Type").orElse(""),
                "Content-Type должен содержать формат данных и кодировку");

        List<Movie> movies = GSON.fromJson(
                response.body(), MOVIES_LIST_TYPE);
        assertEquals(List.of(), movies,
                "При пустом хранилище ожидается пустой массив");
    }

    @Test
    void getMovies_whenStoreContainsMovie_returnsMovie() throws Exception {
        Movie movie = new Movie(1, "Космическая одиссея", 1968);
        store.add(movie);

        HttpResponse<String> response = getMovies();
        List<Movie> movies = GSON.fromJson(
                response.body(), MOVIES_LIST_TYPE);

        assertEquals(200, response.statusCode());
        assertEquals(List.of(movie), movies);
    }


    @Test
    void getMovies_withYear_returnsMoviesOfSpecifiedYear()
            throws Exception {
        Movie first = new Movie(1, "Матрица", 1999);
        Movie second = new Movie(2, "Бойцовский клуб", 1999);
        Movie third = new Movie(3, "Интерстеллар", 2014);

        store.add(first);
        store.add(second);
        store.add(third);

        HttpResponse<String> response = send(
                "GET",
                "/movies?year=1999",
                null
        );

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);

        List<Movie> movies = GSON.fromJson(
                response.body(),
                MOVIES_LIST_TYPE
        );

        assertEquals(List.of(first, second), movies);
    }

    @Test
    void getMovies_withInvalidYear_returns400() throws Exception {
        HttpResponse<String> response = send(
                "GET",
                "/movies?year=unknown",
                null
        );

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
    }

    @Test
    void getMovies_withUnknownParameter_returns400()
            throws Exception {
        HttpResponse<String> response = send(
                "GET",
                "/movies?title=Матрица",
                null
        );

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
    }

    @Test
    void postMovie_withValidBody_returns201AndAddsMovie()
            throws Exception {
        Movie movie = new Movie(
                1,
                "Интерстеллар",
                2014
        );

        HttpResponse<String> response = send(
                "POST",
                "/movies",
                GSON.toJson(movie)
        );

        assertEquals(201, response.statusCode());
        assertJsonContentType(response);

        assertEquals(
                "/movies/1",
                response.headers()
                        .firstValue("Location")
                        .orElse("")
        );

        Movie responseMovie = GSON.fromJson(
                response.body(),
                Movie.class
        );

        assertEquals(movie, responseMovie);
        assertEquals(List.of(movie), store.findAll());
    }

    @Test
    void postMovie_withMalformedJson_returns400()
            throws Exception {
        HttpResponse<String> response = send(
                "POST",
                "/movies",
                "{incorrect json}"
        );

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
        assertTrue(store.findAll().isEmpty());
    }

    @Test
    void postMovie_withInvalidFields_returnsAllErrors()
            throws Exception {
        Movie invalidMovie = new Movie(
                0,
                "",
                1800
        );

        String requestBody = GSON.toJson(invalidMovie);

        HttpResponse<String> response = send(
                "POST",
                "/movies",
                requestBody
        );

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);

        JsonObject errorResponse = JsonParser
                .parseString(response.body())
                .getAsJsonObject();

        JsonArray errors = errorResponse.getAsJsonArray("errors");

        assertEquals(3, errors.size());
        assertEquals(
                "Идентификатор должен быть положительным",
                errors.get(0).getAsString()
        );
        assertEquals(
                "Название фильма не должно быть пустым",
                errors.get(1).getAsString()
        );
        assertEquals(
                "Год выпуска не может быть меньше 1888",
                errors.get(2).getAsString()
        );

        assertTrue(store.findAll().isEmpty());
    }

    @Test
    void postMovie_withExistingId_returns409()
            throws Exception {
        Movie existingMovie = new Movie(
                1,
                "Первый фильм",
                2000
        );

        store.add(existingMovie);

        Movie anotherMovie = new Movie(
                1,
                "Другой фильм",
                2010
        );

        HttpResponse<String> response = send(
                "POST",
                "/movies",
                GSON.toJson(anotherMovie)
        );

        assertEquals(409, response.statusCode());
        assertJsonContentType(response);
        assertEquals(List.of(existingMovie), store.findAll());
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie()
            throws Exception {
        Movie movie = new Movie(
                1,
                "Матрица",
                1999
        );

        store.add(movie);

        HttpResponse<String> response = send(
                "GET",
                "/movies/1",
                null
        );

        assertEquals(200, response.statusCode());
        assertJsonContentType(response);

        Movie responseMovie = GSON.fromJson(
                response.body(),
                Movie.class
        );

        assertEquals(movie, responseMovie);
    }

    @Test
    void getMovieById_whenMovieDoesNotExist_returns404()
            throws Exception {
        HttpResponse<String> response = send(
                "GET",
                "/movies/999",
                null
        );

        assertEquals(404, response.statusCode());
        assertJsonContentType(response);
    }

    @Test
    void getMovieById_withInvalidId_returns400()
            throws Exception {
        HttpResponse<String> response = send(
                "GET",
                "/movies/not-a-number",
                null
        );

        assertEquals(400, response.statusCode());
        assertJsonContentType(response);
    }

    @Test
    void deleteMovie_whenMovieExists_returns204AndRemovesMovie()
            throws Exception {
        Movie movie = new Movie(
                1,
                "Матрица",
                1999
        );

        store.add(movie);

        HttpResponse<String> response = send(
                "DELETE",
                "/movies/1",
                null
        );

        assertEquals(204, response.statusCode());
        assertTrue(response.body().isEmpty());
        assertTrue(store.findById(1).isEmpty());
    }

    @Test
    void deleteMovie_whenMovieDoesNotExist_returns404()
            throws Exception {
        HttpResponse<String> response = send(
                "DELETE",
                "/movies/999",
                null
        );

        assertEquals(404, response.statusCode());
        assertJsonContentType(response);
    }

    private static HttpResponse<String> send(
            String method,
            String path,
            String body
    ) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(base + path))
                .timeout(Duration.ofSeconds(2));

        if (body == null) {
            requestBuilder.method(
                    method,
                    HttpRequest.BodyPublishers.noBody()
            );
        } else {
            requestBuilder
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .method(
                            method,
                            HttpRequest.BodyPublishers.ofString(
                                    body,
                                    StandardCharsets.UTF_8
                            )
                    );
        }

        return client.send(
                requestBuilder.build(),
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );
    }

    private static void assertJsonContentType(
            HttpResponse<String> response
    ) {
        assertEquals(
                "application/json; charset=UTF-8",
                response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
        );
    }

    private static HttpResponse<String> getMovies() throws Exception {
        return send("GET", "/movies", null);
    }
}
