package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private final long id;
    private final String title;
    private final int releaseYear;

    public Movie(long id, String title, int releaseYear) {
        this.id = id;
        this.title = title;
        this.releaseYear = releaseYear;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getReleaseYear() {
        return releaseYear;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Movie movie)) {
            return false;
        }

        return id == movie.id
                && releaseYear == movie.releaseYear
                && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, releaseYear);
    }
}