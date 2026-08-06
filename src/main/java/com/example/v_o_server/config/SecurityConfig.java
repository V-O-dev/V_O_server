package com.example.v_o_server.config;

import com.example.v_o_server.common.security.JwtAccessDeniedHandler;
import com.example.v_o_server.common.security.JwtAuthenticationEntryPoint;
import com.example.v_o_server.common.security.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 설정. JWT 기반 무상태 인증을 사용한다.
 */
@Configuration
@RequiredArgsConstructor
// @WebMvcTest 등 슬라이스 컨텍스트에는 @ConfigurationPropertiesScan이 적용되지 않으므로
// SecurityConfig를 import하는 곳이면 어디서든 CorsProperties가 바인딩되도록 명시한다.
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    /** Swagger / OpenAPI 문서 경로 (항상 공개). */
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/api/health/**"
    };

    /** 인증 없이 호출 가능한 auth 엔드포인트 (로그인/재발급). */
    private static final String[] AUTH_WHITELIST = {
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API: 세션/CSRF 미사용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // preflight(OPTIONS)는 Authorization 헤더 없이 오므로 인증 대상에서 제외
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers(HttpMethod.POST, AUTH_WHITELIST).permitAll()
                        // OAuth 로그인 진입점/콜백 (브라우저 리다이렉트라 인증 헤더가 없다)
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/oauth/**").permitAll()
                        // 테스트용 OAuth 로그인 진입점/콜백
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/dev/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // setAllowedOrigins가 아닌 Patterns를 써야 와일드카드(https://*.vercel.app)를 쓸 수 있다
        configuration.setAllowedOriginPatterns(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
