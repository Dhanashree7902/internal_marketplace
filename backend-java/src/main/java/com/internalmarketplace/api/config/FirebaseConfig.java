package com.internalmarketplace.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import com.google.cloud.firestore.Firestore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Bootstraps the Firebase Admin SDK using the same environment variables and
 * credential-resolution order as the Node backend's lib/firebase-admin.js:
 * an explicit service-account JSON blob (FIREBASE_SERVICE_ACCOUNT_JSON) takes
 * priority, otherwise Application Default Credentials are used (the Cloud Run
 * attached service account in production, or GOOGLE_APPLICATION_CREDENTIALS
 * locally). FIRESTORE_EMULATOR_HOST / FIREBASE_AUTH_EMULATOR_HOST are honored
 * automatically by the underlying client libraries, exactly as in Node.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    private volatile String resolvedProjectId;

    @Bean
    public GoogleCredentials googleCredentials(
            @Value("${firebase.project-id:}") String projectId,
            @Value("${firebase.service-account-json:}") String serviceAccountJson) {
        try {
            if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
                GoogleCredentials credentials;
                try (InputStream in = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
                    credentials = GoogleCredentials.fromStream(in);
                }
                JsonNode serviceAccount = new ObjectMapper().readTree(serviceAccountJson);
                resolvedProjectId = serviceAccount.hasNonNull("project_id")
                        ? serviceAccount.get("project_id").asText()
                        : projectId;
                return credentials;
            }

            resolvedProjectId = projectId;
            return GoogleCredentials.getApplicationDefault();
        } catch (IOException err) {
            log.error("Firebase Admin SDK failed to initialize. Possible causes:");
            log.error("- Missing or invalid service account (set GOOGLE_APPLICATION_CREDENTIALS or FIREBASE_SERVICE_ACCOUNT_JSON)");
            log.error("- Using emulator hosts but the Firebase emulator is not running "
                    + "(check FIREBASE_AUTH_EMULATOR_HOST, FIRESTORE_EMULATOR_HOST)");
            throw new IllegalStateException("Firebase Admin SDK initialization failed", err);
        }
    }

    @Bean
    public FirebaseApp firebaseApp(
            GoogleCredentials googleCredentials,
            @Value("${firebase.storage-bucket:}") String storageBucket) {

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder().setCredentials(googleCredentials);
        if (resolvedProjectId != null && !resolvedProjectId.isBlank()) {
            optionsBuilder.setProjectId(resolvedProjectId);
        }
        if (storageBucket != null && !storageBucket.isBlank()) {
            optionsBuilder.setStorageBucket(storageBucket);
        }

        return FirebaseApp.initializeApp(optionsBuilder.build());
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    /**
     * A raw {@link Storage} client built directly from the same credentials as
     * the FirebaseApp, rather than via {@code StorageClient.getInstance(app).bucket()}.
     * The latter eagerly issues a GET against the bucket at bean-creation time
     * (i.e. application startup) to verify it exists — unlike the Node app's
     * {@code getStorage()}, which returns a lazy local handle with no network
     * call until an operation actually runs. Building {@link Storage} directly
     * preserves that lazy behavior: a transient Storage/network outage no
     * longer fails application startup, matching Node's semantics exactly.
     */
    @Bean
    public Storage storage(GoogleCredentials googleCredentials) {
        StorageOptions.Builder builder = StorageOptions.newBuilder().setCredentials(googleCredentials);
        if (resolvedProjectId != null && !resolvedProjectId.isBlank()) {
            builder.setProjectId(resolvedProjectId);
        }
        return builder.build().getService();
    }
}
