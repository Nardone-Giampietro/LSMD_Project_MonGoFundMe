package xyz.nardone.aide.largescale.exception;

import com.mongodb.MongoException;
import org.neo4j.driver.exceptions.Neo4jException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import xyz.nardone.aide.largescale.DTO.common.ApiResponseDTO;
import xyz.nardone.aide.largescale.DTO.common.ErrorDTO;

import java.util.List;

/**
 * Centralizes HTTP error responses for the REST API.
 *
 * Controllers and services throw validation, business, authentication, and
 * infrastructure exceptions. This handler converts those failures into the
 * common {@link ApiResponseDTO} envelope so clients receive a consistent JSON
 * structure regardless of where the error originated.
 */
@ControllerAdvice
public class CustomExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleFieldException(MethodArgumentNotValidException manv) {
        // Convert each Bean Validation field error into the API error DTO format.
        List<ErrorDTO> errorDTOList = manv.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    logger.error("Inside field validation: {} - {}", fe.getField(), fe.getDefaultMessage());
                    return ApplicationErrorFactory.error(fe.getCode(), fe.getDefaultMessage());
                })
                .toList();
        return new ResponseEntity<>(
                ApiResponseDTO.failure("Validation failed.", errorDTOList),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleBusinessException(BusinessException bex) {
        // Business exceptions already carry a domain-specific error code/message.
        ErrorDTO error = bex.getError();
        logger.error("BusinessException is thrown: {} - {}", error.getCode(), error.getMessage());

        return new ResponseEntity<>(
                ApiResponseDTO.failure("Business validation failed.", List.of(error)),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponseDTO<Void>> handleAuthenticationException(AuthenticationException exception) {
        // Hide credential details and return the same response for all login failures.
        logger.warn("Authentication failed: {}", exception.getMessage());
        return new ResponseEntity<>(
                ApiResponseDTO.failure(
                        "Authentication failed.",
                        List.of(ApplicationErrorFactory.error("AUTH_ERROR", "Invalid email or password."))
                ),
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler({
            DataAccessException.class,
            TransactionException.class,
            MongoException.class,
            Neo4jException.class
    })
    public ResponseEntity<ApiResponseDTO<Void>> handleInfrastructureException(Exception exception) {
        // Database and transaction failures are logged internally and exposed generically.
        logger.error("Infrastructure exception is thrown.", exception);
        return new ResponseEntity<>(
                ApiResponseDTO.failure(
                        "Internal server error.",
                        List.of(ApplicationErrorFactory.error(
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected infrastructure error occurred."
                        ))
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

}
