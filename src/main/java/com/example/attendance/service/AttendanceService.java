package com.example.attendance.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final GeocodingService geocodingService;

	public AttendanceService(AttendanceRepository attendanceRepository,
			GeocodingService geocodingService) {
		this.attendanceRepository = attendanceRepository;
		this.geocodingService = geocodingService;
	}

	@Transactional
	public Attendance punch(User user, String type, Double latitude, Double longitude) {

		LocalDate today = LocalDate.now();

		Attendance attendance = attendanceRepository
				.findByUserAndRecordDate(user, today)
				.orElseGet(() -> {
					Attendance a = new Attendance();
					a.setUser(user);
					a.setRecordDate(today);
					a.setStatus("normal");
					return a;
				});

		if (isCheckInOrOut(type)) {
			attendance.setWorkType(resolveWorkType(type));
		}

		switch (type) {
		case "check_in", "check_in_direct" -> {
			if (attendance.getCheckInTime() == null) {
				attendance.setCheckInTime(LocalDateTime.now());
			}
		}
		case "check_out", "check_out_direct" -> {
			if (attendance.getCheckOutTime() == null) {
				attendance.setCheckOutTime(LocalDateTime.now());
			}
		}
		case "break_start" -> {
			if (attendance.getBreakStartTime() == null) {
				attendance.setBreakStartTime(LocalDateTime.now());
			}
		}
		case "break_end" -> {
			if (attendance.getBreakEndTime() == null) {
				attendance.setBreakEndTime(LocalDateTime.now());
			}
		}
		default -> throw new IllegalArgumentException("Invalid punch type: " + type);
		}

		if (isDirect(type)) {
			attendance.setLatitude(latitude);
			attendance.setLongitude(longitude);

			if (latitude != null && longitude != null) {
				String location = geocodingService.reverseGeocode(latitude, longitude);
				if (location != null && !location.isBlank()) {
					attendance.setLocation(location);
				}
			}
		}

		return attendanceRepository.save(attendance);
	}

	private boolean isDirect(String type) {
		return "check_in_direct".equals(type) || "check_out_direct".equals(type);
	}

	private boolean isCheckInOrOut(String type) {
		return type.startsWith("check_in") || type.startsWith("check_out");
	}

	private String resolveWorkType(String type) {
		return switch (type) {
		case "check_in_direct" -> "DIRECT_IN";
		case "check_out_direct" -> "DIRECT_OUT";
		case "check_in", "check_out" -> "NORMAL";
		default -> null;
		};
	}

	public List<Attendance> getUserAttendance(User user) {
		return attendanceRepository.findByUserOrderByRecordDateDesc(user);
	}

	public List<Attendance> getAllAttendance() {
		return attendanceRepository.findAll();
	}

	public Optional<Attendance> getAttendanceById(Long id) {
		return attendanceRepository.findById(id);
	}

	public List<Attendance> getAttendanceByUserId(Long userId) {
		return attendanceRepository.findByUserId(userId);
	}

	public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
		return attendanceRepository.findByRecordDateBetween(startDate, endDate);
	}

	public List<Attendance> getAttendanceByUserIdAndDateRange(
			Long userId, LocalDate startDate, LocalDate endDate) {
		return attendanceRepository.findByUserIdAndRecordDateBetween(
				userId, startDate, endDate);
	}

	public List<Attendance> detectAnomalies() {
		return attendanceRepository.findAll().stream()
				.filter(att -> att.getCheckOutTime() == null ||
						(att.getCheckInTime() != null &&
								att.getCheckOutTime() != null &&
								ChronoUnit.HOURS.between(
										att.getCheckInTime(),
										att.getCheckOutTime()) > 12))
				.toList();
	}

	@Transactional
	public Attendance saveAttendance(Attendance attendance) {
		return attendanceRepository.save(attendance);
	}
}
