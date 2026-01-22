package com.example.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.attendance.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests(authorize -> authorize
				.requestMatchers("/login", "/css/**", "/js/**").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/employee/**").hasRole("EMPLOYEE")
				.requestMatchers("/attendance/**").hasRole("EMPLOYEE")
				.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.successHandler((request, response, authentication) -> {

							boolean isAdmin = authentication.getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

							if (isAdmin) {
								response.sendRedirect("/admin/dashboard");
							} else {
								response.sendRedirect("/attendance/dashboard");
							}
						})
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.permitAll());

		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		return username -> userRepository.findByUsername(username)
				.map(user -> org.springframework.security.core.userdetails.User.builder()
						.username(user.getUsername())
						.password(user.getPassword())
						.roles(user.getRole())
						.disabled(!user.isEnabled())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
