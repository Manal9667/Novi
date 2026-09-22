package com.novi.repository;

import com.novi.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByExternalMetadataSourceAndExternalMetadataId(String source, String externalId);
    Optional<Book> findByIsbn(String isbn);
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    java.util.List<Book> findByEmbeddingIsNotNull();
    java.util.List<Book> findByEmbeddingIsNull();

    // Candidate scoring pool for recommendations, bounded by the caller's
    // Pageable so we never materialize the whole catalog in memory. Two
    // variants because "id NOT IN (:empty)" is invalid SQL, so the caller
    // picks the exclusion-free query when the user owns no books yet.
    @Query("select b from Book b order by b.id")
    List<Book> findScoringCandidates(Pageable pageable);

    @Query("select b from Book b where b.id not in :excludedIds order by b.id")
    List<Book> findScoringCandidatesExcluding(@Param("excludedIds") Collection<Long> excludedIds, Pageable pageable);
}
