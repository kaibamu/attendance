package com.example.attendance.controller.admin;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.attendance.entity.AdminActionLog;
import com.example.attendance.entity.User;
import com.example.attendance.repository.AdminActionLogRepository;
import com.example.attendance.repository.UserRepository;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AdminActionLogRepository adminActionLogRepository;
	// パスワードルール（将来変更しやすいよう定数化）
	private static final String PASSWORD_PATTERN = "^[A-Za-z0-9]+$";
	private static final String PASSWORD_ERROR_MESSAGE = "パスワードは半角英数字のみ使用できます";

	public AdminUserController(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			AdminActionLogRepository adminActionLogRepository) {

		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminActionLogRepository = adminActionLogRepository;
	}

	// ===============================
	// 一覧（検索・並び替え・ページング）
	// ===============================

	@GetMapping
	public String listUsers(
			@RequestParam(defaultValue = "id") String sort,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		String keyword = q == null ? "" : q.trim();

		Sort s = "emp".equalsIgnoreCase(sort)
				? Sort.by("employeeNo").and(Sort.by("id"))
				: Sort.by("id");

		Pageable pageable = PageRequest.of(page, size, s);

		Page<User> userPage;

		if (StringUtils.hasText(keyword)) {
			userPage = userRepository
					.findByUsernameContainingIgnoreCaseOrEmployeeNoContainingIgnoreCase(
							keyword, keyword, pageable);
		} else {
			userPage = userRepository.findAll(pageable);
		}

		model.addAttribute("users", userPage.getContent());
		model.addAttribute("page", userPage);
		model.addAttribute("sort", sort);
		model.addAttribute("q", keyword);

		return "admin/user_list";
	}

	// ===============================
	// 新規登録（画面表示）
	// ===============================
	@GetMapping("/new")
	public String newUserForm(Model model) {
		model.addAttribute("user", new User());
		return "admin/user_form";
	}

	// ===============================
	// 新規登録（登録処理）
	// ===============================
	@PostMapping
	public String createUser(
			@RequestParam String employeeNo,
			@RequestParam String username,
			@RequestParam String password,
			@RequestParam(required = false, defaultValue = "EMPLOYEE") String role,
			RedirectAttributes ra) {

		String emp = employeeNo == null ? "" : employeeNo.trim();
		String u = username == null ? "" : username.trim();

		if (emp.isEmpty()) {
			ra.addFlashAttribute("error", "社員番号を入力してください");
			return "redirect:/admin/users/new";
		}
		if (u.isEmpty()) {
			ra.addFlashAttribute("error", "ユーザー名を入力してください");
			return "redirect:/admin/users/new";
		}
		if (password == null || password.isBlank()) {
			ra.addFlashAttribute("error", "パスワードを入力してください");
			return "redirect:/admin/users/new";
		}
		if (!password.matches(PASSWORD_PATTERN)) {
			ra.addFlashAttribute("error", PASSWORD_ERROR_MESSAGE);
			return "redirect:/admin/users/new";
		}

		// 社員番号 重複チェック
		if (userRepository.findByEmployeeNo(emp).isPresent()) {
			ra.addFlashAttribute("error", "その社員番号は既に使われています");
			return "redirect:/admin/users/new";
		}

		// username 重複チェック
		if (userRepository.findByUsername(u).isPresent()) {
			ra.addFlashAttribute("error", "そのユーザー名は既に使われています");
			return "redirect:/admin/users/new";
		}

		User user = new User();
		user.setEmployeeNo(emp);
		user.setUsername(u);
		user.setPassword(passwordEncoder.encode(password));
		user.setRole(role);
		user.setEnabled(true);

		userRepository.save(user);
		saveAdminLog("CREATE_USER", user);

		ra.addFlashAttribute("success", "ユーザーを登録しました");
		return "redirect:/admin/users/new";
	}

	// ===============================
	// 停止 / 再開（安全設計込み）
	// ===============================

	@PostMapping("/{id}/toggle")
	public String toggleEnabled(@PathVariable Long id,
			@RequestParam(defaultValue = "id") String sort,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			RedirectAttributes ra) {

		User user = userRepository.findById(id).orElse(null);
		if (user == null) {
			ra.addFlashAttribute("error", "対象ユーザーが見つかりません");
			return redirectBack(sort, q, page, size);
		}

		// -------------------------------
		// ① 自分自身は停止不可
		// -------------------------------
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String loginUser = (auth != null) ? auth.getName() : "";

		if (user.getUsername() != null && user.getUsername().equals(loginUser)) {
			ra.addFlashAttribute("error", "自分自身は停止できません");
			return redirectBack(sort, q, page, size);
		}

		// -------------------------------
		// ② 最後のADMINは停止不可
		// -------------------------------
		if (user.isEnabled() && "ADMIN".equals(user.getRole())) {
			long enabledAdmins = userRepository.countByRoleAndEnabled("ADMIN", true);
			if (enabledAdmins <= 1) {
				ra.addFlashAttribute("error", "最後の管理者は停止できません");
				return redirectBack(sort, q, page, size);
			}
		}

		user.setEnabled(!user.isEnabled());
		userRepository.save(user);

		saveAdminLog("TOGGLE_ENABLED", user);

		ra.addFlashAttribute("success",
				user.isEnabled() ? "ユーザーを有効化しました" : "ユーザーを停止しました");

		return redirectBack(sort, q, page, size);
	}

	// ===============================
	// パスワード初期化
	// ===============================

	@PostMapping("/{id}/reset-password")
	public String resetPassword(@PathVariable Long id,
			@RequestParam(defaultValue = "id") String sort,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			RedirectAttributes ra) {

		User user = userRepository.findById(id).orElse(null);
		if (user == null) {
			ra.addFlashAttribute("error", "対象ユーザーが見つかりません");
			return redirectBack(sort, q, page, size);
		}

		String tempPassword = "Temp1234!";
		user.setPassword(passwordEncoder.encode(tempPassword));
		userRepository.save(user);

		saveAdminLog("RESET_PASSWORD", user);

		ra.addFlashAttribute("success",
				"パスワードを初期化しました（仮パス: " + tempPassword + "）");

		return redirectBack(sort, q, page, size);
	}

	// ===============================
	// 監査ログ保存
	// ===============================

	private void saveAdminLog(String action, User target) {

		String adminName = "unknown";
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && StringUtils.hasText(auth.getName())) {
			adminName = auth.getName();
		}

		AdminActionLog log = new AdminActionLog();
		log.setAdminUsername(adminName);
		log.setAction(action);
		log.setTargetUserId(target.getId());
		log.setTargetEmployeeNo(target.getEmployeeNo());
		log.setTargetUsername(target.getUsername());
		log.setCreatedAt(LocalDateTime.now());

		adminActionLogRepository.save(log);
	}

	private String redirectBack(String sort, String q, int page, int size) {
		String keyword = q == null ? "" : q.trim();
		return "redirect:/admin/users?page=" + page
				+ "&size=" + size
				+ "&sort=" + sort
				+ "&q=" + keyword;
	}

	// ===============================
	// 編集画面（表示）
	// ===============================
	@GetMapping("/{id}/edit")
	public String editUserForm(@PathVariable Long id,
			@RequestParam(defaultValue = "id") String sort,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model,
			RedirectAttributes ra) {

		User user = userRepository.findById(id).orElse(null);
		if (user == null) {
			ra.addFlashAttribute("error", "対象ユーザーが見つかりません");
			return redirectBack(sort, q, page, size);
		}

		model.addAttribute("user", user);
		model.addAttribute("sort", sort);
		model.addAttribute("q", q == null ? "" : q.trim());
		model.addAttribute("pageNo", page);
		model.addAttribute("size", size);

		return "admin/user_edit";
	}

	// ===============================
	// 編集（更新）
	// ===============================
	@PostMapping("/{id}/edit")
	public String updateUser(@PathVariable Long id,
			@RequestParam String employeeNo,
			@RequestParam String username,
			@RequestParam(defaultValue = "id") String sort,
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			RedirectAttributes ra) {

		User user = userRepository.findById(id).orElse(null);
		if (user == null) {
			ra.addFlashAttribute("error", "対象ユーザーが見つかりません");
			return redirectBack(sort, q, page, size);
		}

		String emp = employeeNo == null ? "" : employeeNo.trim();
		String u = username == null ? "" : username.trim();

		if (emp.isEmpty()) {
			ra.addFlashAttribute("error", "社員番号を入力してください");
			return "redirect:/admin/users/" + id + "/edit?sort=" + sort + "&q=" + (q == null ? "" : q.trim())
					+ "&page=" + page + "&size=" + size;
		}
		if (u.isEmpty()) {
			ra.addFlashAttribute("error", "ユーザー名を入力してください");
			return "redirect:/admin/users/" + id + "/edit?sort=" + sort + "&q=" + (q == null ? "" : q.trim())
					+ "&page=" + page + "&size=" + size;
		}

		// 社員番号 重複チェック（自分以外）
		userRepository.findByEmployeeNo(emp).ifPresent(other -> {
			if (!other.getId().equals(user.getId())) {
				throw new IllegalStateException("DUP_EMP");
			}
		});

		// username 重複チェック（自分以外）
		userRepository.findByUsername(u).ifPresent(other -> {
			if (!other.getId().equals(user.getId())) {
				throw new IllegalStateException("DUP_USER");
			}
		});

		try {
			user.setEmployeeNo(emp);
			user.setUsername(u);

			userRepository.save(user);
			saveAdminLog("UPDATE_USER", user);

			ra.addFlashAttribute("success", "ユーザー情報を更新しました");
			return redirectBack(sort, q, page, size);

		} catch (IllegalStateException ex) {
			if ("DUP_EMP".equals(ex.getMessage())) {
				ra.addFlashAttribute("error", "その社員番号は既に使われています");
			} else if ("DUP_USER".equals(ex.getMessage())) {
				ra.addFlashAttribute("error", "そのユーザー名は既に使われています");
			} else {
				ra.addFlashAttribute("error", "更新に失敗しました");
			}
			return "redirect:/admin/users/" + id + "/edit?sort=" + sort + "&q=" + (q == null ? "" : q.trim())
					+ "&page=" + page + "&size=" + size;
		}
	}
}
