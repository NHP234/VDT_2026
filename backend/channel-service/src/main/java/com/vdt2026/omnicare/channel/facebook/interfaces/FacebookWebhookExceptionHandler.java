package com.vdt2026.omnicare.channel.facebook.interfaces;

import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookVerificationException;
import com.vdt2026.omnicare.channel.facebook.application.FacebookWebhookSignatureException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = FacebookWebhookController.class)
class FacebookWebhookExceptionHandler {

    @ExceptionHandler(FacebookWebhookVerificationException.class)
    ProblemDetail handleVerificationFailure(FacebookWebhookVerificationException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Facebook webhook verification failed");
        problem.setDetail(exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(FacebookWebhookSignatureException.class)
    ProblemDetail handleSignatureFailure(FacebookWebhookSignatureException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Facebook webhook signature verification failed");
        problem.setDetail(exception.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Missing request parameter");
        problem.setDetail("Required parameter '" + exception.getParameterName() + "' is missing");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
