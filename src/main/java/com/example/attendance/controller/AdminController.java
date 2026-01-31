package com.example.attendance.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendance.dto.AdminAttendanceSearchCondition;
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

	@ModelAttribute("cond")
	public AdminAttendanceSearchCondition prepareCondition() {
		return new AdminAttendanceSearchCondition();
	}

	/** ★追加：どの画面でも使える未処理件数（共通で model に入れる） */
	private void putPendingFixCount(Model model) {
		long pending = fixRequestService.countPendingFixRequests();
		model.addAttribute("pendingFixCount", pending);
	}

	@GetMapping("/dashboard")
	public String adminDashboard(
			@org.springframework.web.bind.annotation.ModelAttribute("cond") AdminAttendanceSearchCondition cond,
			Model model) {

		if (cond == null) {
			cond = new AdminAttendanceSearchCondition();
		}

		if (cond.getFrom() == null && cond.getTo() == null) {
			LocalDate today = LocalDate.now();
			cond.setFrom(today.withDayOfMonth(1));
			cond.setTo(today);
		}

		Long userId = cond.getUserId();
		LocalDate from = cond.getFrom();
		LocalDate to = cond.getTo();

		List<Attendance> attendanceRecords;

		if (userId != null && from != null && to != null) {
			attendanceRecords = attendanceService.getAttendanceByUserIdAndDateRange(userId, from, to);
		} else if (from != null && to != null) {
			attendanceRecords = attendanceService.getAttendanceByDateRange(from, to);
		} else if (userId != null) {
			attendanceRecords = attendanceService.getAttendanceByUserId(userId);
		} else {
			attendanceRecords = attendanceService.getAllAttendance();
		}

		model.addAttribute("allAttendanceRecords", attendanceService.toViewList(attendanceRecords));
		model.addAttribute("users", userRepository.findAll());

		// ★追加
		putPendingFixCount(model);

		return "admin_dashboard";
	}

	@GetMapping("/attendance")
	public String listAllAttendance(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
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

		model.addAttribute("allAttendanceRecords", attendanceService.toViewList(attendanceRecords));
		model.addAttribute("users", userRepository.findAll());

		// ★追加（/attendance からも admin_dashboard を返してるので必要）
		putPendingFixCount(model);

		return "admin_dashboard";
	}

	@GetMapping("/fix-requests")
	public String listFixRequests(Model model) {
		model.addAttribute("fixRequests", fixRequestService.getAllPendingFixRequests());

		// ★追加
		putPendingFixCount(model);

		// コントローラはこう返してるので、実ファイルは下のどちらかにする
		// templates/admin/fix_request_list.html
		return "admin/fix_request_list";
	}

	@GetMapping("/fix-requests/all")
	public String listAllFixRequests(Model model) {
		model.addAttribute("fixRequests", fixRequestService.getAllFixRequests());

		// ★追加
		putPendingFixCount(model);

		// templates/admin/fix_request_all.html
		return "admin/fix_request_all";
	}

	@PostMapping("/fix-requests/{id}/approve")
	public String approveFixRequest(@PathVariable("id") Long requestId) {
		try {
			fixRequestService.approveFixRequest(requestId);
			return "redirect:/admin/fix-requests?success=approved";
		} catch (IllegalStateException e) {
			return "redirect:/admin/fix-requests?error=alreadyProcessed";
		}
	}

	@PostMapping("/fix-requests/{id}/reject")
	public String rejectFixRequest(@PathVariable("id") Long requestId) {
		try {
			fixRequestService.rejectFixRequest(requestId);
			return "redirect:/admin/fix-requests?success=rejected";
		} catch (IllegalStateException e) {
			return "redirect:/admin/fix-requests?error=alreadyProcessed";
		}
	}

	@GetMapping("/attendance/csv")
	public void exportAttendanceCsv(
			@RequestParam(value = "userId", required = false) Long userId,
			@RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			HttpServletResponse response) throws IOException {

		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"attendance_records.csv\"");
		response.getOutputStream().write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });

		List<Attendance> records;

		if (userId != null && from != null && to != null) {
			records = attendanceService.getAttendanceByUserIdAndDateRange(userId, from, to);
		} else if (from != null && to != null) {
			records = attendanceService.getAttendanceByDateRange(from, to);
		} else if (userId != null) {
			records = attendanceService.getAttendanceByUserId(userId);
		} else {
			records = attendanceService.getAllAttendance();
		}

		try (PrintWriter writer = new PrintWriter(response.getOutputStream(), true, StandardCharsets.UTF_8)) {

			writer.append("ID,ユーザー名,日付,出勤,退勤,実働(数値),休憩開始,休憩終了,場所,ステータス\n");

			DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

			for (Attendance record : records) {

				String username = (record.getUser() != null && record.getUser().getUsername() != null)
						? record.getUser().getUsername()
						: "-";

				String recordDateStr = record.getRecordDate() != null ? record.getRecordDate().toString() : "";
				String checkInStr = record.getCheckInTime() != null ? record.getCheckInTime().format(timeFmt) : "";
				String checkOutStr = record.getCheckOutTime() != null ? record.getCheckOutTime().format(timeFmt) : "";
				String breakStartStr = record.getBreakStartTime() != null ? record.getBreakStartTime().format(timeFmt)
						: "";
				String breakEndStr = record.getBreakEndTime() != null ? record.getBreakEndTime().format(timeFmt) : "";

				long mins = attendanceService.calcWorkingMinutes(record);
				double workAsDay = mins / 1440.0;
				String workAsDayStr = String.format(Locale.US, "%.6f", workAsDay);

				String workTypeStr = workTypeLabel(record.getWorkType());
				String locationStr = record.getLocation() != null ? record.getLocation() : "";
				String statusStr = statusLabel(record.getStatus());

				writer.append(csv(record.getId())).append(",")
						.append(csv(username)).append(",")
						.append(csv(recordDateStr)).append(",")
						.append(csv(checkInStr)).append(",")
						.append(csv(checkOutStr)).append(",")
						.append(csv(workAsDayStr)).append(",")
						.append(csv(breakStartStr)).append(",")
						.append(csv(breakEndStr)).append(",")
						.append(csv(workTypeStr)).append(",")
						.append(csv(locationStr)).append(",")
						.append(csv(statusStr))
						.append("\n");
			}
		}
	}

	private String csv(Object value) {
		if (value == null)
			return "";
		String s = String.valueOf(value);

		boolean needQuote = s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"");
		if (needQuote) {
			s = s.replace("\"", "\"\"");
			return "\"" + s + "\"";
		}
		return s;
	}

	private String workTypeLabel(String workType) {
		if (workType == null || workType.isBlank())
			return "";
		return switch (workType) {
		case "NORMAL" -> "通常";
		case "DIRECT_IN" -> "直行出勤";
		case "DIRECT_OUT" -> "直帰退勤";
		case "DIRECT_RETURN" -> "直行・直帰";
		default -> workType;
		};
	}

	private String statusLabel(String status) {
		if (status == null || status.isBlank())
			return "";
		return switch (status) {
		case "normal" -> "通常";
		case "late" -> "遅刻";
		case "early" -> "早退";
		case "absence" -> "欠勤";
		case "pending" -> "申請中";
		case "approved" -> "承認";
		case "rejected" -> "却下";
		default -> status;
		};
	}
}