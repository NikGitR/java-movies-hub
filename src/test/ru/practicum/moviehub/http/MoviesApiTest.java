package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoviesApiTest {
    private static final Gson GSON = new Gson();
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
                response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(List.of(), movies,
                "При пустом хранилище ожидается пустой массив");
    }

    @Test
    void getMovies_whenStoreContainsMovie_returnsMovie() throws Exception {
        Movie movie = new Movie(1, "Космическая одиссея", 1968);
        store.add(movie);

        HttpResponse<String> response = getMovies();
        List<Movie> movies = GSON.fromJson(
                response.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(200, response.statusCode());
        assertEquals(List.of(movie), movies);
    }

    private static HttpResponse<String> getMovies() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base + "/movies"))
                .GET()
                .build();
        return client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }
}
