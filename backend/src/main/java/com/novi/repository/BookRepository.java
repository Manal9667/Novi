package com.novi.repository;

import com.novi.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Index-backed approximate-nearest-neighbour retrieval against the user's
     * taste vector, using pgvector's cosine-distance operator ({@code <=>}) and
     * the HNSW index. The taste vector is passed as its JSON float-array text
     * (valid pgvector input) and cast to a vector. Returns managed Book entities
     * ordered most-similar first. This replaces scanning the whole catalog and
     * scoring in the application layer when embeddings are available.
     */
    @Query(value = """
            SELECT * FROM books b
            WHERE b.embedding_vec IS NOT NULL
            ORDER BY b.embedding_vec <=> CAST(:taste AS vector)
            LIMIT :k
            """, nativeQuery = true)
    List<Book> findNearestByTasteVector(@Param("taste") String tasteVectorJson, @Param("k") int k);

    /**
     * Generic index-backed nearest-neighbour retrieval against ANY query vector
     * (e.g. an embedded natural-language request), not just a user's taste
     * vector. Same pgvector cosine-distance ANN as {@link #findNearestByTasteVector};
     * kept as a separate method purely for call-site clarity.
     */
    @Query(value = """
            SELECT * FROM books b
            WHERE b.embedding_vec IS NOT NULL
            ORDER BY b.embedding_vec <=> CAST(:vec AS vector)
            LIMIT :k
            """, nativeQuery = true)
    List<Book> findNearestByVector(@Param("vec") String vectorJson, @Param("k") int k);

    /**
     * Lexical (keyword) retrieval for explicit-query recommendations. Matches a
     * single {@code %term%} pattern (already lower-cased by the caller) against
     * the book's title, description, and the names of its authors, genres and
     * themes. {@code distinct} because the collection joins can multiply rows.
     * This is what lets a request like "Chinese books" actually surface books
     * whose metadata mentions "Chinese", independent of the reader's taste.
     */
    @Query("""
            select distinct b from Book b
            left join b.authors a
            left join b.genres g
            left join b.themes t
            where lower(b.title) like :term
               or lower(coalesce(b.description, '')) like :term
               or lower(a.name) like :term
               or lower(g.name) like :term
               or lower(t.name) like :term
            """)
    List<Book> searchByTerm(@Param("term") String lowercaseLikePattern, Pageable pageable);

    /**
     * Mirrors a book's JSON embedding into the native pgvector column. Native
     * because {@code embedding_vec} is intentionally not mapped on the entity
     * (keeping JPA {@code validate} independent of the vector type).
     */
    @Modifying
    @Query(value = "UPDATE books SET embedding_vec = CAST(:vec AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbeddingVector(@Param("id") Long id, @Param("vec") String vec);

    /**
     * Whether the native pgvector column exists (i.e. the V6 migration created
     * it because the extension was available). Lets the app avoid issuing
     * vector queries against a database that skipped the pgvector path, which
     * would otherwise error and poison the surrounding transaction.
     */
    @Query(value = "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
            + "WHERE table_name = 'books' AND column_name = 'embedding_vec')", nativeQuery = true)
    boolean isEmbeddingVectorColumnPresent();
}
