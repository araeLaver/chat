package com.beam;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @Value("${firebase.credentials.path:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void initialize() {
        if (!firebaseEnabled) {
            logger.info("Firebase is disabled. Push notifications will not be available.");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = buildFirebaseOptions();
                if (options != null) {
                    FirebaseApp.initializeApp(options);
                    logger.info("Firebase has been initialized successfully.");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    private FirebaseOptions buildFirebaseOptions() throws IOException {
        GoogleCredentials credentials = null;

        // Try JSON credentials from environment variable first
        if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isEmpty()) {
            InputStream credentialsStream = new ByteArrayInputStream(
                firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8)
            );
            credentials = GoogleCredentials.fromStream(credentialsStream);
            logger.info("Firebase credentials loaded from environment variable.");
        }
        // Try file path if JSON not provided
        else if (firebaseCredentialsPath != null && !firebaseCredentialsPath.isEmpty()) {
            try (InputStream credentialsStream = getClass().getClassLoader()
                    .getResourceAsStream(firebaseCredentialsPath)) {
                if (credentialsStream != null) {
                    credentials = GoogleCredentials.fromStream(credentialsStream);
                    logger.info("Firebase credentials loaded from file: {}", firebaseCredentialsPath);
                }
            }
        }

        if (credentials == null) {
            logger.warn("No Firebase credentials found. Push notifications will not be available.");
            return null;
        }

        return FirebaseOptions.builder()
            .setCredentials(credentials)
            .build();
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!firebaseEnabled || FirebaseApp.getApps().isEmpty()) {
            logger.warn("FirebaseMessaging bean not available - Firebase is not initialized.");
            return null;
        }
        return FirebaseMessaging.getInstance();
    }

    public boolean isEnabled() {
        return firebaseEnabled && !FirebaseApp.getApps().isEmpty();
    }
}
