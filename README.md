# github-api

Spring Boot application that acts as a simple proxy for the GitHub REST API (v3).

## Requirements

- Java 25
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
```

Application starts on port `8080`.

## API

### List user repositories (non-forks) with branches and last commit sha

`GET /api/github/{username}/repositories`

Example:

```bash
curl http://localhost:8080/api/github/octocat/repositories
```

Response (example shape):

```json
[
  {
    "repositoryName": "Hello-World",
    "ownerLogin": "octocat",
    "branches": [
      { "name": "main", "lastCommitSha": "..." }
    ]
  }
]
```

### User not found

If the given GitHub user does not exist, the API returns `404`:

```json
{
  "status": 404,
  "message": "GitHub user 'some-user' not found"
}
```

## Tests

Only integration tests are provided. External GitHub API calls are emulated with WireMock.

```bash
mvn test
```

## Notes

- Pagination is not implemented (neither in the exposed endpoint nor when consuming GitHub API).


