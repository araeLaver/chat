package com.beam;

/**
 * Interface for handling email sending failures
 * Implementations can provide notifications, retry queuing, or other recovery mechanisms
 */
public interface EmailFailureHandler {

    /**
     * Handle an email sending failure
     *
     * @param toEmail The recipient email address
     * @param emailType The type of email (e.g., "VERIFICATION", "WELCOME")
     * @param errorMessage The error message describing the failure
     */
    void handleFailure(String toEmail, String emailType, String errorMessage);
}
