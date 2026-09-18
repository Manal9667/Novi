package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A thematic tag (e.g. "Political", "Psychological", "Coming of age") distinct
 * from Genre. Themes are assigned by {@code BookThemeTaggingService} using an
 * LLM call against the book's description, since they're more nuanced than a
 * catalog-provided genre list.
 */
@Entity
@Table(name = "themes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_themes_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
