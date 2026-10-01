package com.example.hotel.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.example.hotel.exception.GlobalExceptionHandler;
import com.example.hotel.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/customer/auth/login").permitAll()
                        .requestMatchers("/api/v1/customer/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").hasAnyRole("ADMIN", "FRONT_DESK", "CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/auth/change-password").hasAnyRole("ADMIN", "FRONT_DESK", "CUSTOMER")
                        .requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.GET, "/api/v1/operations/**").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.GET, "/api/v1/rooms/**").hasAnyRole("ADMIN", "FRONT_DESK", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/room-types/**").hasAnyRole("ADMIN", "FRONT_DESK", "CUSTOMER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/guests/**").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reservations/**").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.POST, "/api/v1/guests").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/guests/*").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservations").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*/status").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*/extend").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservations/*/change-room").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/operations/notifications/*/read").hasAnyRole("ADMIN", "FRONT_DESK")
                        .requestMatchers("/api/v1/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                GlobalExceptionHandler.writeJsonError(response, 401, "登录状态已失效，请重新登录"))
                        .accessDeniedHandler((request, response, ex) ->
                                GlobalExceptionHandler.writeJsonError(response, 403, "当前账号没有权限执行这个操作"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
