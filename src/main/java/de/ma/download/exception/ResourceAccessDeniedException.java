package de.ma.download.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Exception thrown when access to a resource is denied
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ResourceAccessDeniedException extends RuntimeException {
    public ResourceAccessDeniedException(String message) {
        super(message);
    }
}
