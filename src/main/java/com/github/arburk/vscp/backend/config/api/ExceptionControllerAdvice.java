package com.github.arburk.vscp.backend.config.api;

import com.github.arburk.vscp.backend.infra.api.config.ConfigController;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice(basePackages = "com.github.arburk.vscp.backend.infra.api")
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

  private final Logger log = LoggerFactory.getLogger(ConfigController.class.getName());

  @ResponseBody
  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
    log.error("request {} failed: {}", request.getRequestURL(), ex.getMessage());
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problemDetail.setDetail(ex.getMessage());
    return new ResponseEntity<>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
