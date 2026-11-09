package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A thematic tag (e.g. "unreliable narrator", "found family", "slow burn")
 * distinct from Genre. Themes are curated metadata loaded with the seed catalog
 * and used by the ranking step to reward books whose themes match the request.
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
