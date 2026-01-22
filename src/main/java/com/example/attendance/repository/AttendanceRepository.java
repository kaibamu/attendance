package com.example.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;

@Repository

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

	List<Attendance> findByUserOrderByRecordDateDesc(User user);

	Optional<Attendance> findByUserAndRecordDate(User user, LocalDate recordDate);

	List<Attendance> findByUserId(Long userId);

	List<Attendance> findByRecordDateBetween(LocalDate startDate, LocalDate endDate);

	List<Attendance> findByRecordDateBetweenOrderByRecordDateDesc(LocalDate startDate, LocalDate endDate);

	List<Attendance> findByUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

	List<Attendance> findByUserIdAndRecordDateBetweenOrderByRecordDateDesc(
			Long userId, LocalDate startDate, LocalDate endDate);

	Page<Attendance> findByRecordDateBetween(LocalDate from, LocalDate to, Pageable pageable);

	Page<Attendance> findByUserIdAndRecordDateBetween(Long userId, LocalDate from, LocalDate to, Pageable pageable);

}
