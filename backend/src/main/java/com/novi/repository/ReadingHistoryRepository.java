package com.novi.repository;

import com.novi.entity.ReadingHistoryEvent;
import com.novi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEvent, Long> {
    // Unpaged: the taste-profile pipeline walks the whole history.
    List<ReadingHistoryEvent> findByUserOrderByOccurredAtDesc(User user);

    // Paged variant backing the /reading-history endpoint. The book is fetched
    // eagerly (to-one) so mapping to the response doesn't query per event.
    @EntityGraph(attributePaths = "book")
    Page<ReadingHistoryEvent> findByUser(User user, Pageable pageable);
}
