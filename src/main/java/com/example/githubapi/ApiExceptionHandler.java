package com.example.githubapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(GithubUserNotFoundException.class)
    ResponseEntity<ErrorResponse> handleUserNotFound(GithubUserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "GitHub user '%s' not found".formatted(ex.username())));
    }
}

record ErrorResponse(int status, String message) { }

class GithubUserNotFoundException extends RuntimeException {
    private final String username;

    GithubUserNotFoundException(String username) {
        super("GitHub user not found: " + username);
        this.username = username;
    }

    String username() {
        return username;
    }
}


