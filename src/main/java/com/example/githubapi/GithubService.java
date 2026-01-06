package com.example.githubapi;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
class GithubService {
    private final GithubClient client;

    GithubService(GithubClient client) {
        this.client = client;
    }

    List<RepositoryResponse> listNonForkReposWithBranches(String username) {
        List<GithubRepo> nonForkRepos = client.getUserRepos(username).stream()
                .filter(repo -> !repo.fork())
                .toList();

        return nonForkRepos.parallelStream()
                .map(repo -> new RepositoryResponse(
                        repo.name(),
                        repo.owner().login(),
                        client.getRepoBranches(repo.owner().login(), repo.name()).stream()
                                .map(b -> new BranchResponse(b.name(), b.commit().sha()))
                                .toList()
                ))
                .toList();
    }
}

record RepositoryResponse(String repositoryName, String ownerLogin, List<BranchResponse> branches) { }
record BranchResponse(String name, String lastCommitSha) { }


