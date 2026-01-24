package com.example.attendance.controller.employee;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;

@Controller
@RequestMapping("/employee/password")
public class EmployeePasswordController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public EmployeePasswordController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public String showForm() {
		return "employee/password_change";
	}

	@PostMapping
	public String changePassword(@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String newPasswordConfirm,
			Authentication authentication,
			Model model) {

		String username = authentication.getName();
		User user = userRepository.findByUsername(username).orElseThrow();

		// 現在パス確認
		if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
			model.addAttribute("error", "現在のパスワードが正しくありません");
			return "employee/password_change";
		}

		// 新パスチェック
		if (newPassword == null || newPassword.length() < 8) {
			model.addAttribute("error", "新しいパスワードは8文字以上にしてください");
			return "employee/password_change";
		}
		if (!newPassword.equals(newPasswordConfirm)) {
			model.addAttribute("error", "新しいパスワード（確認）が一致しません");
			return "employee/password_change";
		}

		// 更新
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setMustChangePassword(false);
		userRepository.save(user);

		model.addAttribute("success", "パスワードを変更しました");
		return "employee/password_change";
	}
}