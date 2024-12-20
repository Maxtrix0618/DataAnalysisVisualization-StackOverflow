package com.example.repository;

import com.example.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, Long> {
    List<Answer> findByQuestionIdOrderByCreationDateAsc(Long questionId);
    List<Answer> findByIsAcceptedTrue();
    List<Answer> findByIsAcceptedFalse();
}
