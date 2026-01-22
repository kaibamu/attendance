package com.example.attendance.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public class AdminAttendanceSearchCondition {

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate from;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate to;

	private Long userId;

	private String status;

	public LocalDate getFrom() {
		return from;
	}

	public void setFrom(LocalDate from) {
		this.from = from;
	}

	public LocalDate getTo() {
		return to;
	}

	public void setTo(LocalDate to) {
		this.to = to;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
