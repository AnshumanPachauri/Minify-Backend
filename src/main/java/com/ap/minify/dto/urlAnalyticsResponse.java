package com.ap.minify.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.ap.minify.models.ClickEvent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class urlAnalyticsResponse {

	private String shortCode;
	private String origialUrl;
	private int totalClicks;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	private List<ClickEvent> recentClicks;
	private Map<String, Integer> clicksByReferrer;
	private Map<String, Integer> clicksByHour;
	private Map<String, Integer> clicksByDay;

}
