package com.novi.support;

import com.novi.entity.Author;
import com.novi.entity.Book;
import com.novi.entity.Genre;
import com.novi.entity.Theme;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Builders for constructing catalog entities in unit tests. */
public final class TestBooks {

    private TestBooks() {}

    public static Genre genre(long id, String name) {
        return Genre.builder().id(id).name(name).build();
    }

    public static Theme theme(long id, String name) {
        return Theme.builder().id(id).name(name).build();
    }

    public static Author author(long id, String name) {
        return Author.builder().id(id).name(name).build();
    }

    public static Book book(long id, String title, Set<Genre> genres, Set<Theme> themes) {
        return Book.builder()
                .id(id)
                .title(title)
                .description(title + " description")
                .coverImageUrl("https://example.test/" + id + ".jpg")
                .authors(new LinkedHashSet<>(Set.of(author(id, "Author " + id))))
                .genres(genres)
                .themes(themes)
                .build();
    }

    @SafeVarargs
    public static <T> Set<T> set(T... items) {
        return Arrays.stream(items).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
