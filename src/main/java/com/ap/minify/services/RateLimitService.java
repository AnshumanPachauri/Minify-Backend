package com.ap.minify.services;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.ap.minify.models.RateLimitData;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

	private final RedisTemplate<String, Object> redisTemplate;

	@Value("${minify.rate-limit.requests-per-minute}")
	private int requestsPerMinute;

	@Value("${minify.rate-limit.requests-per-hour}")
	private int requestsPerHour;

	private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:";

	private final ConcurrentHashMap<String, RateLimitData> rateLimitDataMap = new ConcurrentHashMap<>();

	public boolean isAllowed(String clientIp) {
		String redisKey = RATE_LIMIT_KEY_PREFIX + clientIp;
		// ip = 192.168.1.10
		// redisKey = ratelimit:192.168.1.10

		// Get the current date time.
		LocalDateTime now = LocalDateTime.now();

		// Get Data from redis cache.

		RateLimitData rateLimitData = getRateLimitDataFromRedis(redisKey);

		if (rateLimitData == null) {
			rateLimitData = rateLimitDataMap.computeIfAbsent(clientIp, (String k) -> RateLimitData.builder().minCount(0)
					.hourCount(0).minWindowStart(now).hourWindowStart(now).build());
		}

		// Check if the client IP is already in Redis
		return false;
	}

	private RateLimitData getRateLimitDataFromRedis(String redisKey) {
		try {
			return (RateLimitData) redisTemplate.opsForValue().get(redisKey);
		} catch (Exception e) {
			log.error("Error while getting rate limit data from Redis for key {}: {}", redisKey, e.getMessage());
			return null;
		}
	}

}
