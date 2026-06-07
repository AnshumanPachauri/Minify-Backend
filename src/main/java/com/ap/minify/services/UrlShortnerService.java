package com.ap.minify.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ap.minify.dto.ShortenUrlRequest;
import com.ap.minify.dto.ShortenUrlResponse;
import com.ap.minify.dto.UrlStatsResponse;
import com.ap.minify.dto.urlAnalyticsResponse;
import com.ap.minify.models.ClickEvent;
import com.ap.minify.models.UrlData;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortnerService {

	private final RedisTemplate<String, Object> redisTemplate;

	private final Map<String, UrlData> urlMappings = new ConcurrentHashMap<>();

	private final Map<String, List<ClickEvent>> clickAnalytics = new ConcurrentHashMap<>();

	@Value("${minify.base-url}")
	private String baseUrl;
	@Value("${minify.short-code.length}")
	private int shortCodeLength;
	@Value("${minify.short-code.max-attempts}")
	private int maxGenerationAttempts;
	@Value("${minify.cache.ttl-minutes}")
	private int cacheTtlMinutes;

	private static final String BASE_62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public ShortenUrlResponse shortenUrl(@Valid ShortenUrlRequest request, String clientIp) {

		String shortCode = request.getCustomAlias();

		if (shortCode == null || shortCode.trim().isEmpty()) {
			shortCode = generateUniqueShortCode();
		} else {
			shortCode = shortCode.trim();
			if (shortCodeExists(shortCode)) {
				throw new IllegalArgumentException("Custom Alias " + shortCode + " already in use...Try a new one");
			}
		}

		UrlData urlData = UrlData.builder().originalUrl(request.getOriginalUrl()).shortCode(shortCode)
				.expiresAt(request.getExpiresAt()).createdAt(LocalDateTime.now()).createdBy(clientIp).clickCount(0)
				.isActive(true).clickEvent(new ArrayList<>()).build();

		urlMappings.put(shortCode, urlData);
		clickAnalytics.put(shortCode, new ArrayList<>());

		cacheUrl(shortCode, request.getOriginalUrl());
		log.info("Created short URL: {} -> {}", shortCode, request.getOriginalUrl());
		return ShortenUrlResponse.builder().originalUrl(request.getOriginalUrl()).createdAt(urlData.getCreatedAt())
				.expiresAt(urlData.getExpiresAt()).shortCode(shortCode).shortUrl(buildShortUrl(shortCode)).build();
	}

	private String buildShortUrl(String shortCode) {
		String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return normalizedBaseUrl + "/api/" + shortCode;
	}

	private void cacheUrl(String shortCode, String originalUrl) {
		try {
			redisTemplate.opsForValue().set("url: " + shortCode, originalUrl, cacheTtlMinutes, TimeUnit.MINUTES);
		} catch (Exception e) {
			log.warn("Failed to cache url for {} : {}", shortCode, e.getMessage());
		}
	}

	private String generateUniqueShortCode() {

		for (int attempts = 0; attempts < maxGenerationAttempts; attempts++) {
			String code = generateRandomBase62();
			if (!shortCodeExists(code)) {
				return code;
			}
		}
		throw new RuntimeException(
				"Failed to generate unique short code after:- " + maxGenerationAttempts + " attempts.");
	}

	private boolean shortCodeExists(String code) {
		return urlMappings.containsKey(code);
	}

	private String generateRandomBase62() {
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; i < shortCodeLength; i++) {
			int index = ThreadLocalRandom.current().nextInt(BASE_62_CHARS.length());

			stringBuilder.append(BASE_62_CHARS.charAt(index));
		}
		return stringBuilder.toString();
	}

	public Optional<String> getOriginalUrl(String shortCode) {

		String cachedUrl = getCachedUrl(shortCode);

		if (cachedUrl != null) {
			return Optional.of(cachedUrl);
		}

		UrlData urlData = urlMappings.get(shortCode);
		if (urlData != null && urlData.isActive()) {
			if (isExpired(urlData)) {
				urlData.setActive(false);
				return Optional.empty();
			}

			cacheUrl(shortCode, urlData.getOriginalUrl());
			return Optional.of(urlData.getOriginalUrl());
		}

		return Optional.empty();
	}

	private boolean isExpired(UrlData urlData) {
		return urlData.getExpiresAt() != null && urlData.getExpiresAt().isBefore(LocalDateTime.now());
	}

	private String getCachedUrl(String shortCode) {

		try {
			return redisTemplate.opsForValue().get("Url:" + shortCode).toString();
		} catch (Exception e) {
			log.warn("Failed to Read Cached URL for {}:{}", shortCode, e.getMessage());
			return null;
		}
	}

	public void recordClick(String shortCode, String clientIp, String userAgrnt, String referrer) {
		UrlData urlData = urlMappings.get(shortCode);

		if (urlData != null && urlData.isActive()) {
			urlData.setClickCount(urlData.getClickCount() + 1);

			ClickEvent clickEvent = ClickEvent.builder().timeStamp(LocalDateTime.now()).ipAddress(clientIp)
					.userAgent(userAgrnt).referrer(referrer).build();
			clickAnalytics.get(shortCode).add(clickEvent);
			log.debug("Recorder click for shortCode: {}", shortCode);
		}

	}

	public Optional<UrlStatsResponse> getUrlStats(String shortCode) {
		UrlData urlData = urlMappings.get(shortCode);

		if (urlData == null) {
			return Optional.empty();
		}

		return Optional.of(UrlStatsResponse.builder().shortCode(shortCode).originalUrl(urlData.getOriginalUrl())
				.clickCount(urlData.getClickCount()).createdAt(urlData.getCreatedAt()).expiresAt(urlData.getExpiresAt())
				.isActive(urlData.isActive()).createdBy(urlData.getCreatedBy()).build());
	}

	public Optional<urlAnalyticsResponse> getUrlAnalytics(String shortCode) {
		UrlData urlData = urlMappings.get(shortCode);

		if (urlData == null) {
			return Optional.empty();
		}
		List<ClickEvent> clicks = clickAnalytics.getOrDefault(shortCode, new ArrayList<>());
		Map<String, Integer> clicksByReferrer = clicks.stream().filter(c -> c.getReferrer() != null)
				.collect(Collectors.groupingBy(ClickEvent::getReferrer, Collectors.summingInt(e -> 1)));
		Map<String, Integer> clicksByHour = clicks.stream()
				.collect(Collectors.groupingBy(c -> c.getTimeStamp().getHour() + ":00", Collectors.summingInt(e -> 1)));
		return null;
	}

}
