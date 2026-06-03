package com.ap.minify.models;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlData {

	private String originalUrl;
	private String shortCode;
	private LocalDateTime createdAt;
	private LocalDateTime expiresAt;
	private int clickCount;
	private String createdBy;
	private boolean isActive;
	private java.util.List<ClickEvent> clickEvent;
}
