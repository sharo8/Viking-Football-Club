package rw.vikingfc.club.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Object details;

    public BusinessException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public BusinessException(HttpStatus status, String message, String code) {
        this(status, message, code, null);
    }

    public BusinessException(HttpStatus status, String message, String code, Object details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, message);
    }

    public static BusinessException conflict(String message, String code, Object details) {
        return new BusinessException(HttpStatus.CONFLICT, message, code, details);
    }

    public static BusinessException unprocessable(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public static BusinessException unprocessable(String message, String code) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, message, code);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }

    public static BusinessException badRequest(String message, String code) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message, code);
    }
}
