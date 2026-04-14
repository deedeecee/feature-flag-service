package com.debankar.featureflags.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ErrorResponse {

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final OffsetDateTime timestamp;
    private final List<String> details;

    private ErrorResponse(Builder builder) {
        this.status = builder.status;
        this.error = builder.error;
        this.message = builder.message;
        this.path = builder.path;
        this.timestamp = OffsetDateTime.now();
        this.details = builder.details;
    }

    public static Builder builder() { return new Builder(); }

    public int getStatus() { return status; }

    public String getError() { return error; }

    public String getMessage() { return message; }

    public String getPath() { return path; }

    public OffsetDateTime getTimestamp() { return timestamp; }

    public List<String> getDetails() { return details; }

    public static class Builder {
        private int status;
        private String error;
        private String message;
        private String path;
        private List<String> details;

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder details(List<String> details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}