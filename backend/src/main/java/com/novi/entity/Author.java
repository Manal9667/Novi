package com.novi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "authors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_authors_name", columnNames = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;
}
