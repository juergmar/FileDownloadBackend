package de.ma.download.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class JobAlreadyExistsException extends RuntimeException {
    public JobAlreadyExistsException(String message) {
        super(message);
    }
}
