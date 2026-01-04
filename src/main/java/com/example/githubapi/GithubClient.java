package com.example.githubapi;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
class GithubClient {
    private static final ParameterizedTypeReference<List<GithubRepo>> REPO_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<GithubBranch>> BRANCH_LIST =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    GithubClient(RestClient restClient) {
        this.restClient = restClient;
    }

    List<GithubRepo> getUserRepos(String username) {
        return restClient.get()
                .uri("/users/{username}/repos?per_page=100", username)
                .retrieve()
                .onStatus(status -> status.value() == 404, (request, response) -> {
                    throw new GithubUserNotFoundException(username);
                })
                .body(REPO_LIST);
    }

    List<GithubBranch> getRepoBranches(String owner, String repoName) {
        return restClient.get()
                .uri("/repos/{owner}/{repo}/branches?per_page=100", owner, repoName)
                .retrieve()
                .body(BRANCH_LIST);
    }
}

record GithubRepo(String name, boolean fork, GithubOwner owner) { }
record GithubOwner(String login) { }

record GithubBranch(String name, GithubCommit commit) { }
record GithubCommit(String sha) { }


