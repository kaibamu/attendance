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
@Table(name = "attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "record_date", nullable = false)
	private LocalDate recordDate;

	@Column(name = "check_in_time")
	private LocalDateTime checkInTime;

	@Column(name = "check_out_time")
	private LocalDateTime checkOutTime;

	@Column(name = "break_start_time")
	private LocalDateTime breakStartTime;

	@Column(name = "break_end_time")
	private LocalDateTime breakEndTime;

	private String location;

	private String status = "normal";

	@Column(name = "work_type", length = 20)
	private String workType;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;
}
