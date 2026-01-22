package com.example.attendance.service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.attendance.dto.AttendanceViewDto;
import com.example.attendance.entity.Attendance;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository;

@Service
public class AttendanceService {

	private final AttendanceRepository attendanceRepository;
	private final GeocodingService geocodingService;

	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

	public AttendanceService(AttendanceRepository attendanceRepository,
			GeocodingService geocodingService) {
		this.attendanceRepository = attendanceRepository;
		this.geocodingService = geocodingService;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Page<Attendance> findPageByDateRange(LocalDate from, LocalDate to, Pageable pageable) {
		return attendanceRepository.findByRecordDateBetween(from, to, pageable);
	}

	public List<Attendance> findByDateRange(LocalDate from, LocalDate to) {
		return attendanceRepository.findByRecordDateBetweenOrderByRecordDateDesc(from, to);
	}

	public List<Attendance> findByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to) {
		return attendanceRepository
				.findByUserIdAndRecordDateBetween(userId, from, to);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Page<Attendance> findPageByUserIdAndDateRange(Long userId, LocalDate from, LocalDate to, Pageable pageable) {
		return attendanceRepository.findByUserIdAndRecordDateBetween(userId, from, to, pageable);
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
		return type != null && (type.startsWith("check_in") || type.startsWith("check_out"));
	}

	private String resolveWorkType(String type) {
		return switch (type) {
		case "check_in_direct" -> "DIRECT_IN";
		case "check_out_direct" -> "DIRECT_OUT";
		case "check_in", "check_out" -> "NORMAL";
		default -> null;
		};
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> getUserAttendance(User user) {
		return attendanceRepository.findByUserOrderByRecordDateDesc(user);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<AttendanceViewDto> getUserAttendanceView(User user) {
		return attendanceRepository.findByUserOrderByRecordDateDesc(user)
				.stream()
				.map(this::toViewDto)
				.toList();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> getAllAttendance() {
		return attendanceRepository.findAll();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<AttendanceViewDto> getAllAttendanceView() {
		return attendanceRepository.findAll().stream()
				.sorted((a, b) -> {
					if (a.getRecordDate() == null && b.getRecordDate() == null)
						return 0;
					if (a.getRecordDate() == null)
						return 1;
					if (b.getRecordDate() == null)
						return -1;
					int cmp = b.getRecordDate().compareTo(a.getRecordDate());
					if (cmp != 0)
						return cmp;

					if (a.getId() == null && b.getId() == null)
						return 0;
					if (a.getId() == null)
						return 1;
					if (b.getId() == null)
						return -1;
					return b.getId().compareTo(a.getId());
				})
				.map(this::toViewDto)
				.toList();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<AttendanceViewDto> toViewList(List<Attendance> list) {
		return list.stream()
				.map(this::toViewDto)
				.toList();
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public Optional<Attendance> getAttendanceById(Long id) {
		return attendanceRepository.findById(id);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> getAttendanceByUserId(Long userId) {
		return attendanceRepository.findByUserId(userId);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> getAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
		return attendanceRepository.findByRecordDateBetween(startDate, endDate);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> getAttendanceByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
		return attendanceRepository.findByUserIdAndRecordDateBetween(userId, startDate, endDate);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> findAllBetween(LocalDate from, LocalDate to) {
		return attendanceRepository.findByRecordDateBetweenOrderByRecordDateDesc(from, to);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public List<Attendance> detectAnomalies() {
		return attendanceRepository.findAll().stream()
				.filter(att -> att.getCheckOutTime() == null ||
						(att.getCheckInTime() != null &&
								att.getCheckOutTime() != null &&
								ChronoUnit.HOURS.between(att.getCheckInTime(), att.getCheckOutTime()) > 12))
				.toList();
	}

	private AttendanceViewDto toViewDto(Attendance a) {
		AttendanceViewDto dto = new AttendanceViewDto();

		dto.setId(a.getId());
		dto.setUserName(a.getUser() != null ? a.getUser().getUsername() : "-");

		dto.setRecordDate(a.getRecordDate() != null ? a.getRecordDate().format(DATE_FMT) : "-");
		dto.setCheckInTime(a.getCheckInTime() != null ? a.getCheckInTime().format(TIME_FMT) : "-");
		dto.setCheckOutTime(a.getCheckOutTime() != null ? a.getCheckOutTime().format(TIME_FMT) : "-");
		dto.setBreakStartTime(a.getBreakStartTime() != null ? a.getBreakStartTime().format(TIME_FMT) : "-");
		dto.setBreakEndTime(a.getBreakEndTime() != null ? a.getBreakEndTime().format(TIME_FMT) : "-");

		dto.setWorkType(workTypeLabel(a.getWorkType()));
		dto.setLocation(a.getLocation() != null && !a.getLocation().isBlank() ? a.getLocation() : "-");

		dto.setStatus(a.getStatus());
		dto.setStatusLabel(statusLabel(a.getStatus()));

		return dto;
	}

	private String workTypeLabel(String workType) {
		if (workType == null || workType.isBlank())
			return "-";

		return switch (workType) {
		case "NORMAL" -> "通常";
		case "DIRECT_IN" -> "直行出勤";
		case "DIRECT_OUT" -> "直帰退勤";
		case "DIRECT_RETURN" -> "直行・直帰";
		default -> workType;
		};
	}

	private String statusLabel(String status) {
		if (status == null || status.isBlank())
			return "-";

		return switch (status) {
		case "normal" -> "通常";
		case "pending" -> "申請中";
		case "approved" -> "承認";
		case "rejected" -> "却下";
		default -> status;
		};
	}

	@Transactional
	public Attendance saveAttendance(Attendance attendance) {
		return attendanceRepository.save(attendance);
	}

	public long calcWorkingMinutes(Attendance a) {
		if (a.getCheckInTime() == null || a.getCheckOutTime() == null) {
			return 0;
		}

		long totalMinutes = Duration.between(a.getCheckInTime(), a.getCheckOutTime()).toMinutes();

		if (a.getBreakStartTime() != null && a.getBreakEndTime() != null) {
			long breakMinutes = Duration.between(a.getBreakStartTime(), a.getBreakEndTime()).toMinutes();
			totalMinutes -= breakMinutes;
		}

		return Math.max(totalMinutes, 0);
	}

	public long calcOvertimeMinutes(Attendance a) {
		long work = calcWorkingMinutes(a);
		long base = 8 * 60;
		return Math.max(work - base, 0);
	}

	public LocalDate getWeekStart(LocalDate date) {
		return date.with(DayOfWeek.MONDAY);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public long calcWeeklyWorkingMinutes(Long userId, LocalDate anyDateInWeek) {
		LocalDate weekStart = getWeekStart(anyDateInWeek);
		LocalDate weekEnd = weekStart.plusDays(6);

		List<Attendance> weekAttendances = attendanceRepository.findByUserIdAndRecordDateBetween(userId, weekStart,
				weekEnd);

		long totalMinutes = 0;
		for (Attendance a : weekAttendances) {
			totalMinutes += calcWorkingMinutes(a);
		}
		return totalMinutes;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public boolean isWeeklyOver40Hours(Long userId, LocalDate anyDateInWeek) {
		return calcWeeklyWorkingMinutes(userId, anyDateInWeek) > 40 * 60;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public long calcWeeklyLegalOverMinutes(Long userId, LocalDate anyDateInWeek) {
		long weekly = calcWeeklyWorkingMinutes(userId, anyDateInWeek);
		long legal = 40L * 60;
		return Math.max(weekly - legal, 0);
	}

	public boolean isHoliday(LocalDate date) {
		if (date == null)
			return false;
		DayOfWeek dow = date.getDayOfWeek();
		return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
	}

	public long calcHolidayMinutes(Attendance a) {
		if (a == null || a.getRecordDate() == null)
			return 0;
		if (!isHoliday(a.getRecordDate()))
			return 0;
		return calcWorkingMinutes(a);
	}

	public long calcNightMinutes(Attendance a) {
		if (a == null || a.getCheckInTime() == null || a.getCheckOutTime() == null) {
			return 0;
		}

		LocalDateTime start = a.getCheckInTime();
		LocalDateTime end = a.getCheckOutTime();

		if (end.isBefore(start)) {
			return 0;
		}

		long nightMinutes = 0;
		LocalDateTime cursor = start;

		while (cursor.isBefore(end)) {
			LocalDateTime nightStart = cursor.toLocalDate().atTime(22, 0);
			LocalDateTime nightEnd = cursor.toLocalDate().plusDays(1).atTime(5, 0);

			LocalDateTime overlapStart = cursor.isAfter(nightStart) ? cursor : nightStart;
			LocalDateTime overlapEnd = end.isBefore(nightEnd) ? end : nightEnd;

			if (overlapStart.isBefore(overlapEnd)) {
				nightMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
			}

			cursor = cursor.toLocalDate().plusDays(1).atStartOfDay();
		}

		return Math.max(nightMinutes, 0);
	}

	public long calcNightOvertimeMinutes(Attendance a) {

		if (a == null || a.getCheckInTime() == null || a.getCheckOutTime() == null) {
			return 0;
		}

		if (isHoliday(a.getRecordDate())) {
			return 0;
		}

		LocalDateTime start = a.getCheckInTime();
		LocalDateTime end = a.getCheckOutTime();

		if (end.isBefore(start)) {
			return 0;
		}

		LocalDateTime breakStart = a.getBreakStartTime();
		LocalDateTime breakEnd = a.getBreakEndTime();

		long breakMinutes = 0;
		if (breakStart != null && breakEnd != null && breakEnd.isAfter(breakStart)) {
			breakMinutes = Duration.between(breakStart, breakEnd).toMinutes();
		}

		LocalDateTime overtimeStart = start.plusMinutes(480 + breakMinutes);

		if (!end.isAfter(overtimeStart)) {
			return 0;
		}

		long nightOverMinutes = 0;
		LocalDateTime cursor = overtimeStart;

		while (cursor.isBefore(end)) {

			LocalDateTime nightStart = cursor.toLocalDate().atTime(22, 0);
			LocalDateTime nightEnd = cursor.toLocalDate().plusDays(1).atTime(5, 0);

			LocalDateTime overlapStart = cursor.isAfter(nightStart) ? cursor : nightStart;
			LocalDateTime overlapEnd = end.isBefore(nightEnd) ? end : nightEnd;

			if (overlapStart.isBefore(overlapEnd)) {
				nightOverMinutes += Duration.between(overlapStart, overlapEnd).toMinutes();
			}

			cursor = cursor.toLocalDate().plusDays(1).atStartOfDay();
		}

		return Math.max(nightOverMinutes, 0);
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public long calcMonthlyOvertimeMinutes(Long userId, LocalDate anyDateInMonth) {

		if (userId == null || anyDateInMonth == null) {
			return 0;
		}

		LocalDate monthStart = anyDateInMonth.withDayOfMonth(1);
		LocalDate monthEnd = anyDateInMonth.withDayOfMonth(anyDateInMonth.lengthOfMonth());

		List<Attendance> list = attendanceRepository.findByUserIdAndRecordDateBetween(userId, monthStart, monthEnd);

		long total = 0;
		for (Attendance a : list) {
			total += calcOvertimeMinutes(a);
		}
		return total;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public boolean isMonthlyOver45Hours(Long userId, LocalDate anyDateInMonth) {
		return calcMonthlyOvertimeMinutes(userId, anyDateInMonth) > 45L * 60;
	}

	@Transactional(Transactional.TxType.SUPPORTS)
	public long calcMonthlyOver45Minutes(Long userId, LocalDate anyDateInMonth) {
		long monthly = calcMonthlyOvertimeMinutes(userId, anyDateInMonth);
		long legal = 45L * 60;
		return Math.max(monthly - legal, 0);
	}

	public Map<String, Long> calcPremiumMinutes(Attendance a) {

		Map<String, Long> result = new HashMap<>();
		result.put("night", 0L);
		result.put("holiday", 0L);
		result.put("holiday_night", 0L);

		if (a == null || a.getCheckInTime() == null || a.getCheckOutTime() == null) {
			return result;
		}

		boolean holiday = isHoliday(a.getRecordDate());

		long nightMinutes = calcNightMinutes(a);
		long workMinutes = calcWorkingMinutes(a);

		if (holiday) {
			result.put("holiday", workMinutes);

			if (nightMinutes > 0) {
				result.put("holiday_night", nightMinutes);
				result.put("holiday", Math.max(workMinutes - nightMinutes, 0));
			}
		} else {
			result.put("night", nightMinutes);
		}

		return result;
	}

	public long calcPremiumPay(Attendance a, long hourlyWage) {

		Map<String, Long> m = calcPremiumMinutes(a);

		double total = 0;
		total += m.get("night") * hourlyWage / 60.0 * 0.25;
		total += m.get("holiday") * hourlyWage / 60.0 * 0.35;
		total += m.get("holiday_night") * hourlyWage / 60.0 * 0.60;

		return Math.round(total);
	}

	public Map<String, Object> calcMonthlySummary(Long userId, LocalDate anyDateInMonth) {
		Map<String, Object> r = new HashMap<>();

		LocalDate from = anyDateInMonth.withDayOfMonth(1);
		LocalDate to = anyDateInMonth.withDayOfMonth(anyDateInMonth.lengthOfMonth());

		List<Attendance> list = attendanceRepository.findByUserIdAndRecordDateBetween(userId, from, to);

		long work = 0, ot = 0, night = 0, nightOt = 0;

		for (Attendance a : list) {
			work += calcWorkingMinutes(a);
			ot += calcOvertimeMinutes(a);
			night += calcNightMinutes(a);
			nightOt += calcNightOvertimeMinutes(a);
		}

		long over45 = Math.max(ot - 45L * 60, 0);

		r.put("work", work);
		r.put("ot", ot);
		r.put("night", night);
		r.put("nightOt", nightOt);
		r.put("over45", over45);
		r.put("over45Flg", over45 > 0);

		return r;
	}

	public Map<String, Object> calcWeeklyAlert(Long userId, LocalDate anyDateInWeek) {
		Map<String, Object> r = new HashMap<>();

		LocalDate weekStart = getWeekStart(anyDateInWeek); // 月曜開始
		LocalDate weekEnd = weekStart.plusDays(6);

		List<Attendance> list = attendanceRepository.findByUserIdAndRecordDateBetween(userId, weekStart, weekEnd);

		long work = 0;
		for (Attendance a : list) {
			work += calcWorkingMinutes(a);
		}

		long over = Math.max(work - 40L * 60, 0);

		r.put("weekStart", weekStart);
		r.put("weekEnd", weekEnd);
		r.put("work", work);
		r.put("over", over);
		r.put("overFlg", over > 0);

		return r;
	}
}