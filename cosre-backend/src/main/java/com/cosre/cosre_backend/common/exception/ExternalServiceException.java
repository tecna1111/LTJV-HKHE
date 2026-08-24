package com.cosre.cosre_backend.common.exception;
import org.springframework.http.HttpStatus;
public class ExternalServiceException extends RuntimeException {

    private final HttpStatus status;

    public ExternalServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() {
        return status;
    }
}