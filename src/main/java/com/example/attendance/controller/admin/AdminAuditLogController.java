package com.example.attendance.controller.admin;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.attendance.entity.AdminActionLog;
import com.example.attendance.repository.AdminActionLogRepository;

@Controller
@RequestMapping("/admin/audit-logs")
public class AdminAuditLogController {

	private final AdminActionLogRepository adminActionLogRepository;

	public AdminAuditLogController(AdminActionLogRepository adminActionLogRepository) {
		this.adminActionLogRepository = adminActionLogRepository;
	}

	@GetMapping
	public String list(Model model) {
		List<AdminActionLog> logs = adminActionLogRepository.findTop20ByOrderByCreatedAtDesc();
		model.addAttribute("logs", logs);
		return "admin/audit_log_list";
	}
}