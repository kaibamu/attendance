package com.example.attendance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.attendance.entity.FixRequest;
import com.example.attendance.repository.FixRequestRepository;

@Service
public class FixRequestService {

	private final FixRequestRepository fixRequestRepository;

	public FixRequestService(FixRequestRepository fixRequestRepository) {
		this.fixRequestRepository = fixRequestRepository;
	}

	public List<FixRequest> getAllPendingFixRequests() {
		// 既存実装に合わせてOK
		// return fixRequestRepository.findByStatusOrderByIdDesc("pending");
		return fixRequestRepository.findAll().stream()
				.filter(fr -> "pending".equals(fr.getStatus()))
				.toList();
	}

	public List<FixRequest> getAllFixRequests() {
		return fixRequestRepository.findAll();
	}

	/** ★追加：未処理件数 */
	public long countPendingFixRequests() {
		return fixRequestRepository.countByStatus("pending");
	}

	@Transactional
	public void approveFixRequest(Long id) {
		FixRequest req = fixRequestRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Fix request not found: " + id));

		if (!"pending".equals(req.getStatus())) {
			throw new IllegalStateException("Fix request is not pending.");
		}

		req.setStatus("approved");
		fixRequestRepository.save(req);
	}

	@Transactional
	public void rejectFixRequest(Long id) {
		FixRequest req = fixRequestRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Fix request not found: " + id));

		if (!"pending".equals(req.getStatus())) {
			throw new IllegalStateException("Fix request is not pending.");
		}

		req.setStatus("rejected");
		fixRequestRepository.save(req);
	}
}