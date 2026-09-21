package com.novi.repository;

import com.novi.entity.ScanSession;
import com.novi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScanSessionRepository extends JpaRepository<ScanSession, Long> {

    Optional<ScanSession> findByIdAndUser(Long id, User user);

    List<ScanSession> findByUserOrderByCreatedAtDesc(User user);
}
