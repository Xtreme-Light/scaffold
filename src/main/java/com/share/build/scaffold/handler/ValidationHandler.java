package com.share.build.scaffold.handler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ValidationHandler {
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<List<BindErrorVO>> handleException(WebExchangeBindException e) {
        final List<BindErrorVO> collect = e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(v->{
                    if (v instanceof FieldError) {
                        return new BindErrorVO(((FieldError) v).getField(), v.getDefaultMessage());
                    }else {
                        return new BindErrorVO(v.getObjectName(), v.getDefaultMessage());
                    }
                })
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(collect);
    }
    @Data
    @AllArgsConstructor
    public static class BindErrorVO{
        private String fieldName;
        private String errorMessage;
    }
}
