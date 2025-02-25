package de.ma.download.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when the service is overloaded
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServiceOverloadedException extends RuntimeException {
    public ServiceOverloadedException(String message) {
        super(message);
    }
}

