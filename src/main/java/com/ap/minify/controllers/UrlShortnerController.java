package com.ap.minify.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ap.minify.services.RateLimitService;
import com.ap.minify.services.UrlShortnerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class UrlShortnerController {

	private final UrlShortnerService urlShortnerService;
	private final RateLimitService rateLimitService;

}
