package com.example.attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.AdminActionLog;

@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

	List<AdminActionLog> findTop20ByOrderByCreatedAtDesc();
}