package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;

    public MoviesServer(MoviesStore store, int port) {
        try {
            Gson gson = new Gson();

            server = HttpServer.create(
                    new InetSocketAddress(port),
                    0
            );

            server.createContext(
                    "/movies",
                    new MoviesHandler(store, gson)
            );

            server.createContext(
                    "/movies/",
                    new MovieByIdHandler(store, gson)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Не удалось создать HTTP-сервер",
                    exception
            );
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }
}
