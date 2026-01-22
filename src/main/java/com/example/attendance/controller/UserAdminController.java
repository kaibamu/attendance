package com.example.attendance.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendance.service.UserService;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/users-old")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserAdminController {

	private final UserService userService;

	@GetMapping("/new")
	public String newUserForm(Model model) {
		model.addAttribute("form", new UserForm());
		return "user_register";
	}

	@PostMapping
	public String create(@ModelAttribute("form") UserForm form, Model model) {
		try {
			userService.register(
					form.getUsername(),
					form.getPassword(),
					form.getRole(),
					form.getEmail(),
					form.getSlackWebhook());
			return "redirect:/admin/dashboard";
		} catch (IllegalArgumentException e) {
			model.addAttribute("error", e.getMessage());
			return "user_register";
		}
	}

	@Data
	public static class UserForm {
		private String username;
		private String password;
		private String role;
		private String email;
		private String slackWebhook;
	}
}
