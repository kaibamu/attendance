package com.example.attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.FixRequest;
import com.example.attendance.entity.User;

@Repository

public interface FixRequestRepository extends JpaRepository<FixRequest, Long> {

	List<FixRequest> findByUser(User user);

	List<FixRequest> findByStatus(String status);

}
