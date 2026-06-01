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
public class ShortenUrlResponse {

	private String shortUrl;
	private String shortCode;
	private String originalUrl;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;

}
