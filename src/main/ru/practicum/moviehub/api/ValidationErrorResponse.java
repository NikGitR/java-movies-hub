package ru.practicum.moviehub.api;

import java.util.List;

public class ValidationErrorResponse {
    private final List<String> errors;

    public ValidationErrorResponse(List<String> errors) {
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
