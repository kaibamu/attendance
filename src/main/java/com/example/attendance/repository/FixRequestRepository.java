package com.example.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.FixRequest;

@Repository
public interface FixRequestRepository extends JpaRepository<FixRequest, Long> {

	long countByStatus(String status); // ★追加（pending件数用）
}