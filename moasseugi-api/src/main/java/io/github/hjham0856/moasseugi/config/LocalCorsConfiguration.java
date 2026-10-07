package io.github.hjham0856.moasseugi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 로컬 프론트 개발 서버에서 API를 호출할 수 있도록 CORS를 엽니다. */
@Configuration
@Profile("local")
public class LocalCorsConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT")
                .allowedHeaders("Content-Type", "X-Participant-Token");
    }
}
