package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "genres", uniqueConstraints = {
        @UniqueConstraint(name = "uk_genres_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;
}
