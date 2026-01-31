package com.example.attendance.controller;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendance.entity.Attendance; // ← 追加
import com.example.attendance.entity.User;
import com.example.attendance.repository.AttendanceRepository; // ← 追加
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.FixRequestService;

@Controller
@RequestMapping("/attendance")
public class AttendanceController {

	private final AttendanceService attendanceService;
	private final UserRepository userRepository;
	private final FixRequestService fixRequestService;
	private final AttendanceRepository attendanceRepository;

	public AttendanceController(AttendanceService attendanceService,
			UserRepository userRepository,
			FixRequestService fixRequestService,
			AttendanceRepository attendanceRepository) { // ← 追加
		this.attendanceService = attendanceService;
		this.userRepository = userRepository;
		this.fixRequestService = fixRequestService;
		this.attendanceRepository = attendanceRepository; // ← 追加
	}

	@PostMapping("/punch")
	public String punch(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("type") String type,
			@RequestParam(value = "latitude", required = false) Double latitude,
			@RequestParam(value = "longitude", required = false) Double longitude) {

		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		attendanceService.punch(currentUser, type, latitude, longitude);

		return "redirect:/attendance/dashboard";
	}

	@GetMapping("/dashboard")
	public String employeeDashboard(@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("attendanceRecords",
				attendanceService.getUserAttendanceView(currentUser));

		return "employee_dashboard";
	}

	@GetMapping("/history")
	public String showAttendanceHistory(@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		model.addAttribute("attendanceRecords",
				attendanceService.getUserAttendanceView(currentUser));

		return "attendance_history";
	}

	@GetMapping("/history/{id}/request-fix")
	public String showFixRequestForm(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("id") Long attendanceId,
			Model model) {

		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Attendance attendance = attendanceRepository.findById(attendanceId)
				.orElseThrow(() -> new RuntimeException("Attendance not found"));

		model.addAttribute("attendanceId", attendanceId);
		model.addAttribute("attendance", attendance); // ← ★これが超重要
		model.addAttribute("currentUserName", currentUser.getUsername());

		return "fix_request_form";
	}

	@PostMapping("/history/{id}/request-fix")
	public String submitFixRequest(@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("id") Long attendanceId,
			@RequestParam("newCheckInTime") LocalDateTime newCheckInTime,
			@RequestParam("newCheckOutTime") LocalDateTime newCheckOutTime,
			@RequestParam(value = "newBreakStartTime", required = false) LocalDateTime newBreakStartTime,
			@RequestParam(value = "newBreakEndTime", required = false) LocalDateTime newBreakEndTime,
			@RequestParam("reason") String reason) {

		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		fixRequestService.createFixRequest(
				currentUser,
				attendanceId,
				newCheckInTime,
				newCheckOutTime,
				newBreakStartTime,
				newBreakEndTime,
				reason);

		return "redirect:/attendance/history?success=fixRequestSubmitted";
	}
}