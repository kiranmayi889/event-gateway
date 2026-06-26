package com.coding.eventgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI eventGatewayOpenAPI() {

		return new OpenAPI().info(new Info().title("Event Gateway API").description("API for Event Ledger System")
				.version("1.0").license(new License().name("Apache 2.0")));
	}
}