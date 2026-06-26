package com.coding.eventgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class EventGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventGatewayApplication.class, args);
	}

}
