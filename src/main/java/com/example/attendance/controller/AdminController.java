package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendance.entity.Attendance;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.FixRequestService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

	private final AttendanceService attendanceService;
	private final FixRequestService fixRequestService;
	private final UserRepository userRepository;

	public AdminController(AttendanceService attendanceService,
			FixRequestService fixRequestService,
			UserRepository userRepository) {
		this.attendanceService = attendanceService;
		this.fixRequestService = fixRequestService;
		this.userRepository = userRepository;
	}

	@GetMapping("/dashboard")
	public String adminDashboard(Model model) {
		model.addAttribute("allAttendanceRecords", attendanceService.getAllAttendance());
		model.addAttribute("users", userRepository.findAll());
		return "admin_dashboard";
	}

	@GetMapping("/attendance")
	public String listAllAttendance(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "startDate", required = false) LocalDate startDate,
			@RequestParam(value = "endDate", required = false) LocalDate endDate,
			Model model) {

		List<Attendance> attendanceRecords;

		if (userId != null && startDate != null && endDate != null) {
			attendanceRecords = attendanceService.getAttendanceByUserIdAndDateRange(userId, startDate, endDate);
		} else if (startDate != null && endDate != null) {
			attendanceRecords = attendanceService.getAttendanceByDateRange(startDate, endDate);
		} else if (userId != null) {
			attendanceRecords = attendanceService.getAttendanceByUserId(userId);
		} else {
			attendanceRecords = attendanceService.getAllAttendance();
		}

		model.addAttribute("allAttendanceRecords", attendanceRecords);
		model.addAttribute("users", userRepository.findAll());

		return "admin_dashboard";
	}

	@GetMapping("/fix-requests")
	public String listFixRequests(Model model) {
		model.addAttribute("fixRequests", fixRequestService.getAllPendingFixRequests());
		return "fix_request_list";
	}

	@PostMapping("/fix-requests/{id}/approve")
	public String approveFixRequest(@PathVariable("id") Long requestId) {
		fixRequestService.approveFixRequest(requestId);
		return "redirect:/admin/fix-requests?success=approved";
	}

	@PostMapping("/fix-requests/{id}/reject")
	public String rejectFixRequest(@PathVariable("id") Long requestId) {
		fixRequestService.rejectFixRequest(requestId);
		return "redirect:/admin/fix-requests?success=rejected";
	}

	@GetMapping("/alerts")
	public String showAnomalyDetection(Model model) {
		model.addAttribute("anomalies", attendanceService.detectAnomalies());
		return "anomaly_detection";
	}

	@GetMapping("/attendance/csv")
	public void exportAttendanceCsv(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "startDate", required = false) LocalDate startDate,
			@RequestParam(value = "endDate", required = false) LocalDate endDate,
			HttpServletResponse response) throws IOException {

		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"attendance_records.csv\"");

		List<Attendance> records = attendanceService.getAllAttendance();

		if (userId != null) {
			records = records.stream()
					.filter(att -> att.getUser() != null && att.getUser().getId().equals(userId))
					.collect(Collectors.toList());
		}

		if (startDate != null) {
			records = records.stream()
					.filter(att -> att.getRecordDate() != null &&
							!att.getRecordDate().isBefore(startDate))
					.collect(Collectors.toList());
		}

		if (endDate != null) {
			records = records.stream()
					.filter(att -> att.getRecordDate() != null &&
							!att.getRecordDate().isAfter(endDate))
					.collect(Collectors.toList());
		}

		try (PrintWriter writer = response.getWriter()) {
			writer.append("ID,ユーザー名,日付,出勤,退勤,休憩開始,休憩終了,場所,ステータス\n");

			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

			for (Attendance record : records) {
				String recordDateStr = record.getRecordDate() != null
						? record.getRecordDate().toString()
						: "";
				String checkInStr = record.getCheckInTime() != null
						? record.getCheckInTime().format(dateTimeFormatter)
						: "";
				String checkOutStr = record.getCheckOutTime() != null
						? record.getCheckOutTime().format(dateTimeFormatter)
						: "";
				String breakStartStr = record.getBreakStartTime() != null
						? record.getBreakStartTime().format(dateTimeFormatter)
						: "";
				String breakEndStr = record.getBreakEndTime() != null
						? record.getBreakEndTime().format(dateTimeFormatter)
						: "";
				String locationStr = record.getLocation() != null
						? record.getLocation()
						: "";
				String statusStr = record.getStatus() != null
						? record.getStatus()
						: "";

				writer.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s\n",
						record.getId(),
						record.getUser().getUsername(),
						recordDateStr,
						checkInStr,
						checkOutStr,
						breakStartStr,
						breakEndStr,
						locationStr,
						statusStr));
			}
		}
	}
}
