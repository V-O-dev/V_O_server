package com.example.v_o_server.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 로컬 개발 저장소에 업로드된 파일을 인증된 API 클라이언트가 조회할 수 있게 매핑한다.
 * S3 저장소 적용 후에는 제거할 수 있다.
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    private static final Path UPLOAD_DIRECTORY = Paths.get("uploads").toAbsolutePath().normalize();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(UPLOAD_DIRECTORY.toUri().toString());
    }
}
