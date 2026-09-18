package com.novi.repository;

import com.novi.entity.ReadingHistoryEvent;
import com.novi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReadingHistoryRepository extends JpaRepository<ReadingHistoryEvent, Long> {
    List<ReadingHistoryEvent> findByUserOrderByOccurredAtDesc(User user);
}
