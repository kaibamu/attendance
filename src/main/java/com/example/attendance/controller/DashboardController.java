package com.example.attendance.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;

@Controller

public class DashboardController {

	private final AttendanceService attendanceService;
	private final UserRepository userRepository;

	public DashboardController(AttendanceService attendanceService, UserRepository userRepository) {
		this.attendanceService = attendanceService;
		this.userRepository = userRepository;
	}

	@GetMapping("/dashboard")

	public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		User currentUser = userRepository.findByUsername(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
			model.addAttribute("allAttendanceRecords", attendanceService.getAllAttendanceView());
			return "admin_dashboard";
		} else {
			model.addAttribute("attendanceRecords", attendanceService.getUserAttendanceView(currentUser));
			return "employee_dashboard";
		}
	}

}
