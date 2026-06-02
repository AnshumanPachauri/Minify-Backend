package com.ap.minify.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

		if (isWithinMinuteWindow(rateLimitData, now)) {
			if (rateLimitData.getMinCount() >= requestsPerMinute) {
				log.warn("Minute Limit exceeded for IP: {}", clientIp);
				return false; // Rate limit exceeded for minute window
			} else {
				rateLimitData.setMinCount(0);
				rateLimitData.setMinWindowStart(now);
			}
		}

		if (isWithinHourWindow(rateLimitData, now)) {
			if (rateLimitData.getHourCount() >= requestsPerHour) {
				log.warn("Hour Limit exceeded for IP: {}", clientIp);
				return false; // Rate limit exceeded for hour window
			} else {
				rateLimitData.setHourCount(0);
				rateLimitData.setHourWindowStart(now);
			}
		}

		rateLimitData.setMinCount(rateLimitData.getMinCount() + 1);
		rateLimitData.setHourCount(rateLimitData.getHourCount() + 1);

		saveRateLimitDataToRedis(redisKey, rateLimitData);
		return true;
	}

	private boolean isWithinHourWindow(RateLimitData rateLimitData, LocalDateTime now) {
		return rateLimitData.getHourWindowStart() != null
				&& ChronoUnit.HOURS.between(rateLimitData.getHourWindowStart(), now) < 1;
	}

	private boolean isWithinMinuteWindow(RateLimitData rateLimitData, LocalDateTime now) {
		return rateLimitData.getMinWindowStart() != null
				&& ChronoUnit.MINUTES.between(rateLimitData.getMinWindowStart(), now) < 1;
	}

	private void saveRateLimitDataToRedis(String redisKey, RateLimitData rateLimitData) {
		try {
			redisTemplate.opsForValue().set(redisKey, rateLimitData, 1, TimeUnit.HOURS); // Set expiration time to 1
																							// hour
		} catch (Exception e) {
			log.error("Error while saving rate limit data to Redis for key {}: {}", redisKey, e.getMessage());
		}

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
