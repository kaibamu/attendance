package com.example.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String role;

	@Column(unique = true)
	private String email;

	@Column(name = "slack_webhook")
	private String slackWebhook;

	@Column(nullable = false)
	private boolean enabled = true;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword = false;

	@Column(name = "employee_no", unique = true, length = 20)
	private String employeeNo;
}
