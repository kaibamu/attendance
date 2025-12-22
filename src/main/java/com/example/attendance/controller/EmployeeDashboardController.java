package com.example.attendance.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {

	private final AttendanceService attendanceService;
	private final UserRepository userRepository;

	public EmployeeDashboardController(AttendanceService attendanceService, UserRepository userRepository) {
		this.attendanceService = attendanceService;
		this.userRepository = userRepository;
	}

	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		model.addAttribute("attendanceRecords", attendanceService.getUserAttendance(currentUser));
		return "employee_dashboard";
	}
}
