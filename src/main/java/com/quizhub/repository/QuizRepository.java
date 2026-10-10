package com.quizhub.repository;

import com.quizhub.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByTitle(String title);
}
