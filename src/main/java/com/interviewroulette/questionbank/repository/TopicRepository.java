
package com.interviewroulette.questionbank.repository;

import com.interviewroulette.questionbank.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    Optional<Topic> findBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    List<Topic> findAllByActiveTrueOrderByNameAsc();
    
    // Add this method so Excel topic names can be matched to existing topics.
    Optional<Topic> findByNameIgnoreCase(String name);
}
