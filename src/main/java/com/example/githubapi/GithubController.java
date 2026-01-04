package com.example.githubapi;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/github", produces = MediaType.APPLICATION_JSON_VALUE)
class GithubController {
    private final GithubService service;

    GithubController(GithubService service) {
        this.service = service;
    }

    @GetMapping("/{username}/repositories")
    List<RepositoryResponse> listRepositories(@PathVariable String username) {
        return service.listNonForkReposWithBranches(username);
    }
}


