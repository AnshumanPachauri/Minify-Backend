package com.ap.minify.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ap.minify.dto.ShortenUrlRequest;
import com.ap.minify.services.RateLimitService;
import com.ap.minify.services.UrlShortnerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class UrlShortnerController {

	private final UrlShortnerService urlShortnerService;
	private final RateLimitService rateLimitService;

	@PostMapping("/shorten")
	public ResponseEntity<?> shortenUrl(@Valid @RequestBody ShortenUrlRequest request, HttpServletRequest httpRequest) {

		String clientIp = getClientIp(httpRequest);

		return null;
	}

	private String getClientIp(HttpServletRequest httpRequest) {

		String xforwardedFor = httpRequest.getHeader("X-Forwarded-For");
		if (xforwardedFor != null && !xforwardedFor.isEmpty()) {
			return xforwardedFor.split(",")[0].trim();
		}
		String xRealIp = httpRequest.getHeader("X-Real-IP");
		if (xRealIp != null && !xRealIp.isEmpty()) {
			return xRealIp;
		}

		return httpRequest.getRemoteAddr();
	}

}
