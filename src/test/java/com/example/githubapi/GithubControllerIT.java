package com.example.githubapi;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.web.client.RestClient;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GithubControllerIT {

    static WireMockServer wireMock;

    @LocalServerPort
    int port;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("github.base-url", () -> wireMock.baseUrl());
    }

    @Test
    void shouldReturnOnlyNonForkReposWithBranches() {
        wireMock.stubFor(WireMock.get("/users/john/repos?per_page=100")
                .willReturn(okJson("""
                        [
                          {"name":"forked-repo","fork":true,"owner":{"login":"john"}},
                          {"name":"repo-1","fork":false,"owner":{"login":"john"}}
                        ]
                        """)));

        wireMock.stubFor(WireMock.get("/repos/john/repo-1/branches?per_page=100")
                .willReturn(okJson("""
                        [
                          {"name":"main","commit":{"sha":"abc123"}},
                          {"name":"dev","commit":{"sha":"def456"}}
                        ]
                        """)));

        RestClient client = RestClient.builder().baseUrl("http://localhost:" + port).build();

        String response = client.get()
                .uri("/api/github/john/repositories")
                .retrieve()
                .body(String.class);

        assertThat(response).contains("repo-1");
        assertThat(response).doesNotContain("forked-repo");
        assertThat(response).contains("\"repositoryName\":\"repo-1\"");
        assertThat(response).contains("\"ownerLogin\":\"john\"");
        assertThat(response).contains("\"name\":\"main\"");
        assertThat(response).contains("\"lastCommitSha\":\"abc123\"");
        assertThat(response).contains("\"name\":\"dev\"");
        assertThat(response).contains("\"lastCommitSha\":\"def456\"");
    }

    @Test
    void shouldReturn404WhenGithubUserDoesNotExist() {
        wireMock.stubFor(WireMock.get("/users/unknown/repos?per_page=100")
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"message":"Not Found","documentation_url":"https://developer.github.com/v3"}
                                """)));

        RestClient client = RestClient.builder().baseUrl("http://localhost:" + port).build();

        try {
            client.get()
                    .uri("/api/github/unknown/repositories")
                    .retrieve()
                    .body(String.class);
            assertThat(false).as("Expected exception").isTrue();
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            String body = ex.getResponseBodyAsString();
            assertThat(body).contains("\"status\":404");
            assertThat(body).contains("\"message\":\"GitHub user 'unknown' not found\"");
        }
    }

    @Test
    void shouldFetchBranchesInParallel() {
        int delayMs = 1000;

        wireMock.stubFor(WireMock.get("/users/testuser/repos?per_page=100")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"name":"repo-1","fork":false,"owner":{"login":"testuser"}},
                                  {"name":"repo-2","fork":false,"owner":{"login":"testuser"}},
                                  {"name":"fork-repo","fork":true,"owner":{"login":"testuser"}}
                                ]
                                """)
                        .withFixedDelay(delayMs)));

        wireMock.stubFor(WireMock.get("/repos/testuser/repo-1/branches?per_page=100")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"name":"main","commit":{"sha":"sha1"}},
                                  {"name":"develop","commit":{"sha":"sha2"}},
                                  {"name":"feature/one","commit":{"sha":"sha3"}}
                                ]
                                """)
                        .withFixedDelay(delayMs)));

        wireMock.stubFor(WireMock.get("/repos/testuser/repo-2/branches?per_page=100")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"name":"main","commit":{"sha":"sha4"}},
                                  {"name":"release","commit":{"sha":"sha5"}},
                                  {"name":"hotfix","commit":{"sha":"sha6"}}
                                ]
                                """)
                        .withFixedDelay(delayMs)));

        RestClient client = RestClient.builder().baseUrl("http://localhost:" + port).build();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String response = client.get()
                .uri("/api/github/testuser/repositories")
                .retrieve()
                .body(String.class);

        stopWatch.stop();
        long totalTimeMs = stopWatch.getTime();

        assertThat(response).contains("repo-1");
        assertThat(response).contains("repo-2");
        assertThat(response).doesNotContain("fork-repo");

        wireMock.verify(3, getRequestedFor(urlMatching(".*")));

        assertThat(totalTimeMs)
                .as("Total time should be between 2000ms and 3000ms (parallel branch fetching)")
                .isGreaterThanOrEqualTo(2000)
                .isLessThanOrEqualTo(3000);
    }
}
