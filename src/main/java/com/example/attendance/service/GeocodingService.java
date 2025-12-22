package com.example.attendance.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class GeocodingService {

	private final WebClient webClient;

	public GeocodingService(WebClient.Builder builder) {
		this.webClient = builder
				.baseUrl("https://nominatim.openstreetmap.org")
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.USER_AGENT, "attendance-app/1.0 (test-use)")
				.build();
	}

	public String reverseGeocode(Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			System.out.println("[Geo] latitude or longitude is null");
			return null;
		}

		try {
			String lat = URLEncoder.encode(latitude.toString(), StandardCharsets.UTF_8);
			String lon = URLEncoder.encode(longitude.toString(), StandardCharsets.UTF_8);

			String json = webClient.get()
					.uri("/reverse?format=jsonv2&lat=" + lat + "&lon=" + lon)
					.retrieve()
					.bodyToMono(String.class)
					.timeout(Duration.ofSeconds(5))
					.block();

			System.out.println("[Geo] raw json = " + json);

			String address = extractDisplayName(json);
			System.out.println("[Geo] extracted address = " + address);

			return address;

		} catch (Exception e) {
			System.out.println("[Geo] exception occurred");
			e.printStackTrace();
			return null;
		}
	}

	private String extractDisplayName(String json) {
		if (json == null)
			return null;

		String key = "\"display_name\":\"";
		int start = json.indexOf(key);
		if (start < 0)
			return null;
		start += key.length();

		int end = json.indexOf("\"", start);
		if (end < 0)
			return null;

		return json.substring(start, end).replace("\\u0026", "&").replace("\\\"", "\"");
	}
}
