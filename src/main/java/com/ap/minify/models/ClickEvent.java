package com.ap.minify.models;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ClickEvent {

	private LocalDateTime timeStamp;
	private String ipAddress;
	private String userAgent;
	private String referrer;
	private String country;
	private String city;

}
