package de.ma.download.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus; /**
 * Exception thrown when an attempt is made to download a file that's not ready
 */
@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
public class FileNotReadyException extends RuntimeException {
    public FileNotReadyException(String jobId) {
        super("File is not ready for download. Job ID: " + jobId);
    }
}
