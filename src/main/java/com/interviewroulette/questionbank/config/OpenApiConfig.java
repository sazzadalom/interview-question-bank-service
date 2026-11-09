package com.interviewroulette.questionbank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI questionBankOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Interview Question Bank Service API")
                        .description("REST APIs for topics, interview questions, and Excel question uploads.")
                        .version("1.0.0"));
    }
}
