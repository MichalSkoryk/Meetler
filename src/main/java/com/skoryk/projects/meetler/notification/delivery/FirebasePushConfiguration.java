package com.skoryk.projects.meetler.notification.delivery;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "notification.firebase", name = "enabled", havingValue = "true")
public class FirebasePushConfiguration {

  private static final String APP_NAME = "meetler-push";

  @Bean
  FirebaseApp meetlerFirebaseApp(
      @Value("${notification.firebase.service-account-base64:}") String encodedCredentials)
      throws IOException {
    if (encodedCredentials == null || encodedCredentials.isBlank()) {
      throw new IllegalStateException(
          "FIREBASE_SERVICE_ACCOUNT_BASE64 is required when Firebase push is enabled");
    }

    byte[] json;
    try {
      json = Base64.getDecoder().decode(encodedCredentials.trim());
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Firebase service account is not valid base64", exception);
    }

    GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json));
    FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
    return FirebaseApp.getApps().stream()
        .filter(app -> APP_NAME.equals(app.getName()))
        .findFirst()
        .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
  }

  @Bean
  FirebaseMessaging firebaseMessaging(FirebaseApp meetlerFirebaseApp) {
    return FirebaseMessaging.getInstance(meetlerFirebaseApp);
  }
}
