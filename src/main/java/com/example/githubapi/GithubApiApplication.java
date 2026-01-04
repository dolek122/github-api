package com.example.githubapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableConfigurationProperties(GithubProperties.class)
public class GithubApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubApiApplication.class, args);
    }

    @Bean
    RestClient githubRestClient(GithubProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "github-api-proxy")
                .build();
    }
}

@ConfigurationProperties(prefix = "github")
record GithubProperties(String baseUrl) { }


