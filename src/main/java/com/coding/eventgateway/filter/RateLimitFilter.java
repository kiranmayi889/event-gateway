package com.coding.eventgateway.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

	@Value("${gateway.rate-limit.capacity}")
	private int capacity;

	@Value("${gateway.rate-limit.refill}")
	private int refill;

	private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		Bucket bucket = resolveBucket(request.getRemoteAddr());
		System.out.println("tokens "+bucket.getAvailableTokens());
		
		if (bucket.tryConsume(1)) {
			filterChain.doFilter(request, response);
		} else {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setContentType("application/json");

			response.getWriter().write("""
					{
					  "status":429,
					  "error":"Too Many Requests",
					  "message":"Rate limit exceeded. Please try again later."
					}
					""");
		}
	}

	private Bucket resolveBucket(String ip) {
		return cache.computeIfAbsent(ip,
				k -> Bucket.builder().addLimit(
						Bandwidth.builder().capacity(capacity).refillGreedy(refill, Duration.ofMinutes(1)).build())
						.build());
	}
}