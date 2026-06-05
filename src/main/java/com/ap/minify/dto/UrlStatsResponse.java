package com.ap.minify.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UrlStatsResponse {

	private String shortCode;
	private String originalUrl;
	private int clickCount;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	private Boolean isActive;
	private String createdBy;

}
