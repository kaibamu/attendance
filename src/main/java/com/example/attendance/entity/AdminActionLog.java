package com.example.attendance.entity;

import java.time.LocalDateTime;

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
@Table(name = "admin_action_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminActionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 実行者（最小：username文字列で保存）
	@Column(name = "admin_username", nullable = false, length = 100)
	private String adminUsername;

	// 操作種別（例: TOGGLE_ENABLED / RESET_PASSWORD）
	@Column(nullable = false, length = 50)
	private String action;

	// 対象ユーザー
	@Column(name = "target_user_id", nullable = false)
	private Long targetUserId;

	@Column(name = "target_username", length = 100)
	private String targetUsername;

	@Column(name = "target_employee_no", length = 20)
	private String targetEmployeeNo;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}