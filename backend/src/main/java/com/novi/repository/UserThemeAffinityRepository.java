package com.novi.repository;

import com.novi.entity.User;
import com.novi.entity.UserThemeAffinity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserThemeAffinityRepository extends JpaRepository<UserThemeAffinity, Long> {
    List<UserThemeAffinity> findByUserOrderByScoreDesc(User user);
    void deleteByUser(User user);
}
