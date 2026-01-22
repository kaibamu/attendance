package com.example.attendance.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.attendance.entity.User;
import com.example.attendance.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public User register(String username, String rawPassword, String role, String email, String slackWebhook) {

		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("そのユーザーIDは既に存在します");
		}

		User user = new User();
		user.setUsername(username);
		user.setPassword(passwordEncoder.encode(rawPassword));
		user.setRole(role);
		user.setEmail(email);
		user.setSlackWebhook(slackWebhook);
		user.setEnabled(true);

		return userRepository.save(user);
	}
}
