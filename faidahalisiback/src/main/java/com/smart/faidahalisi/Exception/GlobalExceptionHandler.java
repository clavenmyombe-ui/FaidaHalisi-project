package com.smart.faidahalisi.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice 
public class GlobalExceptionHandler {
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<String> handleResourceNotFoundException(ResourceNotFoundException exception){
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
}
@ExceptionHandler(value = {IllegalArgumentException.class})
public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException exception){
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
}
// Makosa ya @Valid (mfano jina tupu): tunarudisha ujumbe wa kwanza wa kueleweka, si maelezo ya ndani ya Spring.
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<String> handleValidation(MethodArgumentNotValidException exception){
    String message = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .findFirst()
            .orElse("Taarifa ulizotuma si sahihi.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
}
}
