package com.vdt2026.omnicare.channel.simulator.interfaces;

import com.vdt2026.omnicare.channel.facebook.application.InvalidFacebookInboundEventException;
import com.vdt2026.omnicare.channel.email.application.InvalidEmailInboundEventException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = { FacebookSimulatorController.class, EmailSimulatorController.class })
class SimulatorExceptionHandler {

    @ExceptionHandler(InvalidFacebookInboundEventException.class)
    ProblemDetail handleInvalidEvent(InvalidFacebookInboundEventException exception, HttpServletRequest request) {
        return problem("Invalid simulator event", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidEmailInboundEventException.class)
    ProblemDetail handleInvalidEmailEvent(InvalidEmailInboundEventException exception, HttpServletRequest request) {
        return problem("Invalid simulator event", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return problem("Validation failed", "Request body contains invalid fields", request);
    }

    private ProblemDetail problem(String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
