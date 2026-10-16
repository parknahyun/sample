package com.example.commerce.config;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.example.commerce.common.ConflictException;
import com.example.commerce.common.FieldProblem;
import com.example.commerce.common.NotFoundException;
import com.example.commerce.common.ValidationFailedException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 오류를 Problem Details(RFC 9457) 형식으로 통일한다. 응답에는 요청 ID와 조치 가능한 한글 안내를 담고, 예상하지 못한 오류의 내부 정보는 노출하지
 * 않는다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  ResponseEntity<Object> notFound(NotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, "찾을 수 없음", ex.getMessage(), List.of());
  }

  @ExceptionHandler(ConflictException.class)
  ResponseEntity<Object> conflict(ConflictException ex) {
    return problem(HttpStatus.CONFLICT, "충돌", ex.getMessage(), List.of());
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ResponseEntity<Object> optimisticLock(OptimisticLockingFailureException ex) {
    log.warn("동시 수정 충돌", kv("event", "version_conflict"));
    return problem(
        HttpStatus.CONFLICT, "동시 수정 충돌", "다른 사용자가 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 다시 시도하세요.", List.of());
  }

  @ExceptionHandler(ValidationFailedException.class)
  ResponseEntity<Object> validationFailed(ValidationFailedException ex) {
    return problem(HttpStatus.BAD_REQUEST, "입력 검증 실패", ex.getMessage(), ex.getErrors());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Object> constraintViolation(ConstraintViolationException ex) {
    List<FieldProblem> errors =
        ex.getConstraintViolations().stream()
            .map(v -> new FieldProblem(v.getPropertyPath().toString(), v.getMessage()))
            .toList();
    return problem(HttpStatus.BAD_REQUEST, "입력 검증 실패", "입력값을 확인하세요.", errors);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<Object> typeMismatch(MethodArgumentTypeMismatchException ex) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "입력 검증 실패",
        "'" + ex.getName() + "' 값의 형식이 올바르지 않습니다.",
        List.of(new FieldProblem(ex.getName(), "형식이 올바르지 않습니다")));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Object> unexpected(Exception ex) {
    log.error("처리되지 않은 오류", kv("event", "unhandled_exception"), ex);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "서버 오류",
        "서버에서 오류가 발생했습니다. 잠시 후 다시 시도하고, 계속되면 요청 ID와 함께 관리자에게 문의하세요.",
        List.of());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<FieldProblem> errors =
        ex.getBindingResult().getAllErrors().stream()
            .map(
                e ->
                    new FieldProblem(
                        e instanceof org.springframework.validation.FieldError fe
                            ? fe.getField()
                            : e.getObjectName(),
                        e.getDefaultMessage()))
            .toList();
    return problem(HttpStatus.BAD_REQUEST, "입력 검증 실패", "입력값을 확인하세요.", errors);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST, "요청 형식 오류", "요청 형식이 올바르지 않습니다. JSON 구조와 값의 형식을 확인하세요.", List.of());
  }

  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return problem(
        HttpStatus.PAYLOAD_TOO_LARGE,
        "파일이 너무 큽니다",
        "이미지는 장당 5MB 이하만 올릴 수 있습니다. 파일 크기를 줄여 다시 올려 주세요.",
        List.of());
  }

  /** 스프링 MVC가 처리하는 나머지 오류(405, 415 등)에도 요청 ID를 붙인다. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    if (body instanceof ProblemDetail detail) {
      detail.setProperty("requestId", MDC.get(RequestIdFilter.MDC_KEY));
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  private static ResponseEntity<Object> problem(
      HttpStatus status, String title, String detail, List<FieldProblem> errors) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    problem.setProperty("requestId", MDC.get(RequestIdFilter.MDC_KEY));
    if (!errors.isEmpty()) {
      problem.setProperty("errors", errors);
    }
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
