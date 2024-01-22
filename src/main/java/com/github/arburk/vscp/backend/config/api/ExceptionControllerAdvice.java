package com.github.arburk.vscp.backend.config.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

@ControllerAdvice(basePackages = "com.github.arburk.vscp.backend.infra.api")
@Slf4j
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

  @ResponseBody
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleControllerException(HttpServletRequest request, Throwable ex) {
    log.error("request {} failed: {}", request.getRequestURL(), ex.getMessage());
    final HttpStatus internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
    return new ResponseEntity<>(getProblemDetail(request, ex, internalServerError), internalServerError);
  }

  @ResponseBody
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAccessDeniedException(HttpServletRequest request, Throwable ex) {
    log.error("request {} failed: {}", request.getRequestURL(), ex.getMessage());
    final HttpStatus forbidden = HttpStatus.FORBIDDEN;
    final ProblemDetail problemDetail = getProblemDetail(request, ex, forbidden);
    problemDetail.setTitle("AccessDeniedException");
    return new ResponseEntity<>(problemDetail, forbidden);
  }

  private static ProblemDetail getProblemDetail(final HttpServletRequest request, final Throwable ex, final HttpStatus status) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(status);
    problemDetail.setDetail(ex.getMessage());
    final String server = String.format("%s://%s", request.getScheme(), request.getServerName());
    final String port = String.format("://%s", request.getServerPort());
    final URI instance = URI.create(request.getServletPath().replace(server, "").replace(port, ""));
    problemDetail.setInstance(instance);
    problemDetail.setProperty("Method", request.getMethod());
    return problemDetail;
  }
}
