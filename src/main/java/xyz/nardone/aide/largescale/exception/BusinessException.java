package xyz.nardone.aide.largescale.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.nardone.aide.largescale.DTO.common.ErrorDTO;

@Getter
@Setter
@NoArgsConstructor
public class BusinessException extends RuntimeException {
    private ErrorDTO error;

    public BusinessException(ErrorDTO error) {
        this.error = error;
    }
}
