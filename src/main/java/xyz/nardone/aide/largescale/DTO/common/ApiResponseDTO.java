package xyz.nardone.aide.largescale.DTO.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDTO<T> {

    private boolean success;
    private String message;
    private T data;
    private List<ErrorDTO> errors;
    private LocalDateTime timestamp;

    public static <T> ApiResponseDTO<T> success(String message, T data) {
        return new ApiResponseDTO<>(true, message, data, null, LocalDateTime.now());
    }

    public static <T> ApiResponseDTO<T> failure(String message, List<ErrorDTO> errors) {
        return new ApiResponseDTO<>(false, message, null, errors, LocalDateTime.now());
    }
}
