package com.example.attendance.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.attendance.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmployeeNo(String employeeNo);

	List<User> findByUsernameContainingIgnoreCaseOrEmployeeNoContainingIgnoreCase(String username, String employeeNo);

	Page<User> findByUsernameContainingIgnoreCaseOrEmployeeNoContainingIgnoreCase(
			String username, String employeeNo, Pageable pageable);

	long countByRoleAndEnabled(String role, boolean enabled);

}
