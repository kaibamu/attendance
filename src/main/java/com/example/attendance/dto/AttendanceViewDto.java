package com.example.attendance.dto;

import lombok.Data;

@Data
public class AttendanceViewDto {
	private Long id;

	private String userName;

	private String recordDate;
	private String checkInTime;
	private String checkOutTime;
	private String breakStartTime;
	private String breakEndTime;

	private String workType;
	private String location;
	private String status;
	private String statusLabel;
}
