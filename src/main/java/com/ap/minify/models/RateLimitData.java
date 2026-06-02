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
public class RateLimitData {

	private int minCount;
	private int hourCount;
	private LocalDateTime minWindowStart;
	private LocalDateTime hourWindowStart;

}
