package com.novi.repository;

import com.novi.entity.User;
import com.novi.entity.UserGenreAffinity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserGenreAffinityRepository extends JpaRepository<UserGenreAffinity, Long> {
    List<UserGenreAffinity> findByUserOrderByScoreDesc(User user);
    void deleteByUser(User user);
}
