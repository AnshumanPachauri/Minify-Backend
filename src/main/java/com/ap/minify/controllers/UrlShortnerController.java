package com.ap.minify.controllers;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ap.minify.dto.ShortenUrlRequest;
import com.ap.minify.dto.ShortenUrlResponse;
import com.ap.minify.dto.UrlStatsResponse;
import com.ap.minify.services.RateLimitService;
import com.ap.minify.services.UrlShortnerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

		if (!rateLimitService.isAllowed(clientIp)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.body(Map.of("error", "Rate limit exceeded", "remainingRequests",
							rateLimitService.getRemainingRequests(clientIp), "timeUntilReset",
							rateLimitService.getTimeUntilReset(clientIp)));
		}

		try {

			ShortenUrlResponse shortenUrlResponse = urlShortnerService.shortenUrl(request, clientIp);
			return ResponseEntity.ok(shortenUrlResponse);

		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void> redirectToUrl(@PathVariable String shortCode, HttpServletRequest request,
			HttpServletResponse response) {

		String clientIp = getClientIp(request);
		String userAgrnt = request.getHeader("User-Agent");
		String referrer = request.getHeader("Referrer");

		Optional<String> originalUrl = urlShortnerService.getOriginalUrl(shortCode);

		if (originalUrl.isPresent()) {
			urlShortnerService.recordClick(shortCode, clientIp, userAgrnt, referrer);
			response.setHeader("Location", originalUrl.get());
			return ResponseEntity.status(HttpStatus.FOUND).build();
		} else {
			return ResponseEntity.notFound().build();
		}

	}

	@GetMapping("/stats/{shortCode}")
	public ResponseEntity<?> getUrlStatus(@PathVariable String shortCode) {

		Optional<UrlStatsResponse> stats = urlShortnerService.getUrlStats(shortCode);

		if (stats.isPresent()) {
			return ResponseEntity.ok(stats.get());
		}

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("Error", "ShortCode not found"));
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
