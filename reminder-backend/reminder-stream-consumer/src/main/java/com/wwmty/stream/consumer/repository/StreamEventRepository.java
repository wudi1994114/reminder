package com.wwmty.stream.consumer.repository;

import com.wwmty.stream.consumer.model.StreamEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * StreamEvent 实体的 Spring Data JPA 仓库。
 */
@Repository
public interface StreamEventRepository extends JpaRepository<StreamEvent, Long> {
} 