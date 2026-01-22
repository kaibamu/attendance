package com.example.attendance.controller.admin;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.attendance.entity.Attendance;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;

@Controller
@RequestMapping("/admin/attendances")
public class AdminAttendanceController {

	private final AttendanceService attendanceService;
	private final UserRepository userRepository;

	public AdminAttendanceController(
			AttendanceService attendanceService,
			UserRepository userRepository) {
		this.attendanceService = attendanceService;
		this.userRepository = userRepository;
	}

	@GetMapping
	public String list(
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(defaultValue = "0") int page,
			Model model) {

		if (from == null && to == null) {
			LocalDate today = LocalDate.now();
			from = today.withDayOfMonth(1);
			to = today;
		} else {
			if (from == null)
				from = LocalDate.now().withDayOfMonth(1);
			if (to == null)
				to = LocalDate.now();
		}

		PageRequest pageable = PageRequest.of(page, 10, Sort.by("recordDate").descending());

		Page<Attendance> pageObj = (userId != null)
				? attendanceService.findPageByUserIdAndDateRange(userId, from, to, pageable)
				: attendanceService.findPageByDateRange(from, to, pageable);

		Map<Long, String> workTimeMap = new HashMap<>();
		Map<Long, String> overtimeMap = new HashMap<>();
		Map<Long, String> nightMap = new HashMap<>();
		Map<Long, String> holidayMap = new HashMap<>();
		Map<Long, String> premiumMap = new HashMap<>();

		Map<Long, Boolean> weeklyOverMap = new HashMap<>();
		Map<Long, String> weeklyTotalMap = new HashMap<>();
		Map<Long, String> weeklyOverMinutesMap = new HashMap<>();
		Map<Long, Boolean> monthHeadMap = new HashMap<>();

		Map<Long, Boolean> weekHeadMap = new HashMap<>();
		Map<Long, String> premiumPayMap = new HashMap<>();
		Map<Long, String> nightOtMap = new HashMap<>();
		Map<Long, Boolean> monthlyOverMap = new HashMap<>();
		Map<Long, String> monthlyTotalOtMap = new HashMap<>();
		Map<Long, String> monthlyOverMinutesMap = new HashMap<>();

		for (Attendance a : pageObj.getContent()) {

			long workMins = attendanceService.calcWorkingMinutes(a);
			workTimeMap.put(a.getId(), String.format("%d:%02d", workMins / 60, workMins % 60));

			long otMins = attendanceService.calcOvertimeMinutes(a);
			overtimeMap.put(a.getId(), String.format("%d:%02d", otMins / 60, otMins % 60));

			long nightMins = attendanceService.calcNightMinutes(a);
			nightMap.put(a.getId(), nightMins > 0 ? String.format("%d:%02d", nightMins / 60, nightMins % 60) : "-");

			long nightOtMins = attendanceService.calcNightOvertimeMinutes(a);
			nightOtMap.put(a.getId(),
					nightOtMins > 0 ? String.format("%d:%02d", nightOtMins / 60, nightOtMins % 60) : "-");

			long holidayMins = attendanceService.calcHolidayMinutes(a);
			holidayMap.put(a.getId(),
					holidayMins > 0 ? String.format("%d:%02d", holidayMins / 60, holidayMins % 60) : "-");

			var pm = attendanceService.calcPremiumMinutes(a);
			long premiumMinutes = pm.get("night") + pm.get("holiday") + pm.get("holiday_night");
			premiumMap.put(a.getId(),
					premiumMinutes > 0 ? String.format("%d:%02d", premiumMinutes / 60, premiumMinutes % 60) : "-");

			long hourlyWage = 1000; // 仮の時給
			long pay = attendanceService.calcPremiumPay(a, hourlyWage);
			premiumPayMap.put(a.getId(), pay > 0 ? String.format("%,d円", pay) : "-");

			// 週40h（ユーザーごとに初回のみ計算）
			if (a.getUser() != null && a.getUser().getId() != null && a.getRecordDate() != null) {
				Long uid = a.getUser().getId();

				if (!weeklyTotalMap.containsKey(uid)) {
					long weeklyMins = attendanceService.calcWeeklyWorkingMinutes(uid, a.getRecordDate());
					long weeklyOver = attendanceService.calcWeeklyLegalOverMinutes(uid, a.getRecordDate());

					weeklyTotalMap.put(uid, String.format("%d:%02d", weeklyMins / 60, weeklyMins % 60));
					weeklyOverMinutesMap.put(uid,
							weeklyOver > 0 ? String.format("+%d:%02d", weeklyOver / 60, weeklyOver % 60) : "-");
					weeklyOverMap.put(uid, weeklyOver > 0);
				}
			}

			// 月45h（ユーザーごとに初回のみ計算）
			if (a.getUser() != null && a.getUser().getId() != null && a.getRecordDate() != null) {
				Long uid = a.getUser().getId();

				if (!monthlyTotalOtMap.containsKey(uid)) {
					long monthlyOtMins = attendanceService.calcMonthlyOvertimeMinutes(uid, a.getRecordDate());
					long monthlyOver = attendanceService.calcMonthlyOver45Minutes(uid, a.getRecordDate());

					monthlyTotalOtMap.put(uid,
							String.format("%d:%02d", monthlyOtMins / 60, monthlyOtMins % 60));

					monthlyOverMinutesMap.put(uid,
							monthlyOver > 0
									? String.format("+%d:%02d", monthlyOver / 60, monthlyOver % 60)
									: "-");

					monthlyOverMap.put(uid, attendanceService.isMonthlyOver45Hours(uid, a.getRecordDate()));
				}
			}

			boolean isWeekHead = false;
			if (a.getRecordDate() != null) {
				DayOfWeek dow = a.getRecordDate().getDayOfWeek();
				isWeekHead = (dow == DayOfWeek.MONDAY);
			}
			weekHeadMap.put(a.getId(), isWeekHead);

			boolean isMonthHead = false;
			if (a.getRecordDate() != null) {
				isMonthHead = (a.getRecordDate().getDayOfMonth() == 1);
			}
			monthHeadMap.put(a.getId(), isMonthHead);
		}

		model.addAttribute("from", from);
		model.addAttribute("to", to);
		model.addAttribute("userId", userId);

		model.addAttribute("pageObj", pageObj);
		model.addAttribute("attendances", pageObj.getContent());
		model.addAttribute("users", userRepository.findAll());

		model.addAttribute("workTimeMap", workTimeMap);
		model.addAttribute("overtimeMap", overtimeMap);
		model.addAttribute("nightMap", nightMap);
		model.addAttribute("holidayMap", holidayMap);
		model.addAttribute("premiumMap", premiumMap);

		model.addAttribute("weeklyOverMap", weeklyOverMap);
		model.addAttribute("weeklyTotalMap", weeklyTotalMap);
		model.addAttribute("weeklyOverMinutesMap", weeklyOverMinutesMap);

		model.addAttribute("weekHeadMap", weekHeadMap);
		model.addAttribute("premiumPayMap", premiumPayMap);
		model.addAttribute("monthlyOverMap", monthlyOverMap);
		model.addAttribute("monthlyTotalOtMap", monthlyTotalOtMap);
		model.addAttribute("monthlyOverMinutesMap", monthlyOverMinutesMap);
		model.addAttribute("nightOtMap", nightOtMap);
		model.addAttribute("monthHeadMap", monthHeadMap);

		return "admin/attendance_list";
	}

	// ---------------------------
	// 日次CSV（明細）
	// ---------------------------
	@GetMapping("/csv")
	public void exportCsv(
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			HttpServletResponse response) throws IOException {

		if (from == null && to == null) {
			LocalDate today = LocalDate.now();
			from = today.withDayOfMonth(1);
			to = today;
		} else {
			if (from == null)
				from = LocalDate.now().withDayOfMonth(1);
			if (to == null)
				to = LocalDate.now();
		}

		Charset sjis = Charset.forName("MS932");
		response.setContentType("text/csv; charset=MS932");
		response.setHeader("Content-Disposition", "attachment; filename=attendances.csv");

		PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), sjis), true);

		writer.println("sep=,");

		writer.println(
				"recordDate,username,checkInTime,checkOutTime,workMinutes,overtimeMinutes,nightMinutes,nightOvertimeMinutes");

		var list = (userId != null)
				? attendanceService.findByUserIdAndDateRange(userId, from, to)
				: attendanceService.findByDateRange(from, to);

		for (Attendance a : list) {
			long work = attendanceService.calcWorkingMinutes(a);
			long ot = attendanceService.calcOvertimeMinutes(a);
			long night = attendanceService.calcNightMinutes(a);
			long nightOt = attendanceService.calcNightOvertimeMinutes(a);

			writer.println("sep=,");

			writer.println(
					safe(a.getRecordDate()) + "," +
							safe(a.getUser() != null ? a.getUser().getUsername() : "") + "," +
							safe(a.getCheckInTime()) + "," +
							safe(a.getCheckOutTime()) + "," +
							hm(work) + "," +
							hm(ot) + "," +
							hm(night) + "," +
							hm(nightOt));
		}

		writer.flush();
	}

	// ---------------------------
	// 月次CSV（1人1行）
	// ---------------------------
	@GetMapping("/csv/monthly")
	public void exportMonthlyCsv(
			@RequestParam(required = false) String month, // "yyyy-MM" or "yyyy-MM-dd"
			HttpServletResponse response) throws IOException {

		YearMonth ym = parseYearMonth(month);
		LocalDate anyDateInMonth = ym.atDay(1);

		response.setContentType("text/csv; charset=MS932");

		String ymStr = ym.toString(); // "yyyy-MM"
		response.setHeader("Content-Disposition",
				"attachment; filename=attendance_monthly_" + ymStr + ".csv");

		Charset sjis = Charset.forName("MS932");
		var writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), sjis), true);

		writer.println("sep=,");

		writer.println("年月,ユーザー名,総実働,残業,深夜,深夜残業,45h超過,超過時間");

		var users = userRepository.findAll();
		for (var u : users) {
			var m = attendanceService.calcMonthlySummary(u.getId(), anyDateInMonth);

			writer.println("sep=,");

			writer.println(
					ymStr + "," +
							u.getUsername() + "," +
							hm((long) m.get("work")) + "," +
							hm((long) m.get("ot")) + "," +
							hm((long) m.get("night")) + "," +
							hm((long) m.get("nightOt")) + "," +
							((boolean) m.get("over45Flg") ? "超過" : "") + "," +
							hm((long) m.get("over45")));
		}
		writer.flush();
	}

	// ---------------------------
	// 週アラートCSV（週40h）
	// ---------------------------
	@GetMapping("/csv/alert/weekly")
	public void exportWeeklyAlertCsv(
			@RequestParam(required = false) String date,
			HttpServletResponse response) throws IOException {

		LocalDate base = parseLocalDate(date);

		response.setContentType("text/csv; charset=MS932");
		String yw = base.getYear() + "-W" + String.format("%02d",
				base.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));
		response.setHeader("Content-Disposition",
				"attachment; filename=attendance_alert_weekly_" + yw + ".csv");

		var writer = new PrintWriter(
				new OutputStreamWriter(response.getOutputStream(),
						Charset.forName("MS932")),
				true);

		writer.println("sep=,");

		writer.println("年週,ユーザー名,週実働,週40h超過,超過時間,対象週開始,対象週終了");

		var users = userRepository.findAll();
		for (var u : users) {
			var m = attendanceService.calcWeeklyAlert(u.getId(), base);

			// ★超過してない人は出さない
			if (!(boolean) m.get("overFlg")) {
				continue;
			}

			String yearWeek = base.getYear() + "-W" + String.format("%02d",
					base.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR));

			writer.println("sep=,");

			writer.println(
					yearWeek + "," +
							u.getUsername() + "," +
							hm((long) m.get("work")) + "," +
							"超過" + "," +
							hm((long) m.get("over")) + "," +
							m.get("weekStart") + "," +
							m.get("weekEnd"));
		}
		writer.flush();
	}

	// ---------------------------
	// 月アラートCSV（月45h）
	// ---------------------------
	@GetMapping("/csv/alert/monthly")
	public void exportMonthlyAlertCsv(
			@RequestParam(required = false) String month, // "yyyy-MM" or "yyyy-MM-dd"
			HttpServletResponse response) throws IOException {

		YearMonth ym = parseYearMonth(month);
		LocalDate anyDateInMonth = ym.atDay(1);

		response.setContentType("text/csv; charset=MS932");

		String ymStr = ym.toString(); // "yyyy-MM"
		response.setHeader("Content-Disposition",
				"attachment; filename=attendance_alert_monthly_" + ymStr + ".csv");

		var writer = new PrintWriter(
				new OutputStreamWriter(response.getOutputStream(),
						Charset.forName("MS932")),
				true);

		writer.println("sep=,");

		writer.println("年月,ユーザー名,月残業,月45h超過,超過時間,対象月開始,対象月終了");

		LocalDate monthStart = ym.atDay(1);
		LocalDate monthEnd = ym.atEndOfMonth();

		var users = userRepository.findAll();
		for (var u : users) {
			var m = attendanceService.calcMonthlySummary(u.getId(), anyDateInMonth);

			// ★45h超過してない人は出さない
			if (!(boolean) m.get("over45Flg")) {
				continue;
			}

			writer.println("sep=,");

			writer.println(
					ymStr + "," +
							u.getUsername() + "," +
							hm((long) m.get("ot")) + "," +
							"超過" + "," +
							hm((long) m.get("over45")) + "," +
							monthStart + "," +
							monthEnd);
		}
		writer.flush();
	}

	// ---------------------------
	// helpers
	// ---------------------------
	private String safe(Object v) {
		return v == null ? "" : String.valueOf(v);
	}

	private String hm(long minutes) {
		if (minutes <= 0)
			return "0:00";
		return String.format("%d:%02d", minutes / 60, minutes % 60);
	}

	// "yyyy-MM" でも "yyyy-MM-dd" でも受けられる安全版
	private YearMonth parseYearMonth(String month) {
		if (month == null || month.isBlank())
			return YearMonth.now();

		String s = month.trim();
		if (s.length() >= 7)
			s = s.substring(0, 7); // yyyy-MM だけ使う

		try {
			return YearMonth.parse(s);
		} catch (Exception e) {
			return YearMonth.now();
		}
	}

	private LocalDate parseLocalDate(String date) {
		if (date == null || date.isBlank())
			return LocalDate.now();

		String s = date.trim();

		// "yyyy-MM" で来たら 1日扱いにする（週計算の基準日としてOK）
		if (s.length() == 7)
			s = s + "-01";

		// "yyyy-MM-dd..." でも先頭10文字だけ使う
		if (s.length() >= 10)
			s = s.substring(0, 10);

		try {
			return LocalDate.parse(s); // ISO yyyy-MM-dd
		} catch (Exception e) {
			return LocalDate.now();
		}
	}
}