package com.ap.minify.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortenUrlRequest {

	@NotBlank(message = "Original URL cannot be blank")
	@Pattern(regexp = "^https?://.*", message = "Invalid URL format")
	private String originalUrl;
	private String customAlias;
	private LocalDateTime expiresAt;

}
