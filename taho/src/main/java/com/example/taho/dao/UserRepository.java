package com.example.taho.dao;

import com.example.taho.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 必要なカスタムメソッドを追加できます
    User findByUsername(String username);

    boolean existsByUsername(String username);
}
