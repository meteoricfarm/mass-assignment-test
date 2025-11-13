package com.example.mass_assignment_test;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 테스트의 편의를 위해 ID로 사용자를 찾는 메소드 추가
    Optional<User> findById(Long id);
}