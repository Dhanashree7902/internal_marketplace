package com.internalmarketplace.api.config;

import com.internalmarketplace.api.common.json.FirestoreTimestampModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer firestoreTimestampCustomizer() {
        return builder -> builder.modulesToInstall(new FirestoreTimestampModule());
    }
}
