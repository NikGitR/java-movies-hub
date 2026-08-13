package ru.practicum.moviehub.api;

public class ErrorResponse {
    private final String error;

    public ErrorResponse(String error) {
        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("Описание ошибки не должно быть пустым");
        }
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
