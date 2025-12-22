package com.example.attendance.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fix_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FixRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "attendance_id")
	private Attendance attendance;

	@Column(name = "request_date", nullable = false)
	private LocalDate requestDate;

	@Column(name = "new_check_in_time")
	private LocalDateTime newCheckInTime;

	@Column(name = "new_check_out_time")
	private LocalDateTime newCheckOutTime;

	@Column(name = "new_break_start_time")
	private LocalDateTime newBreakStartTime;

	@Column(name = "new_break_end_time")
	private LocalDateTime newBreakEndTime;

	@Column(nullable = false)
	private String reason;

	private String status = "pending";
}
