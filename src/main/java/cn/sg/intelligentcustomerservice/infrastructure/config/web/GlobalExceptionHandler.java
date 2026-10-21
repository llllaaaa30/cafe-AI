package cn.sg.intelligentcustomerservice.infrastructure.config.web;

import cn.sg.intelligentcustomerservice.common.lang.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理业务异常
     * 
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public R<Void> handleRuntimeException(RuntimeException ex) {
        log.error("handleRuntimeException: {}", ex.getMessage(), ex);
        return R.failed(500, ex.getMessage());
    }
    
    /**
     * 处理参数验证异常
     * 
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(Exception ex) {
        BindingResult bindingResult;
        if (ex instanceof MethodArgumentNotValidException) {
            bindingResult = ((MethodArgumentNotValidException) ex).getBindingResult();
        } else {
            bindingResult = ((BindException) ex).getBindingResult();
        }
        
        StringBuilder sb = new StringBuilder("参数校验失败:");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append(" [").append(fieldError.getField()).append("]").append(fieldError.getDefaultMessage()).append(",");
        }
        String msg = sb.deleteCharAt(sb.length() - 1).toString();
        log.warn(msg);
        return R.validateFailed(msg);
    }
    
    /**
     * 处理IllegalArgumentException异常
     * 
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("handleIllegalArgumentException: {}", ex.getMessage(), ex);
        return R.validateFailed(ex.getMessage());
    }
    
    /**
     * 处理通用异常
     * 
     * @param ex 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleGenericException(Exception ex) {
        log.error("handleGenericException: {}", ex.getMessage(), ex);
        return R.failed(500, "系统内部错误，请联系管理员");
    }
}