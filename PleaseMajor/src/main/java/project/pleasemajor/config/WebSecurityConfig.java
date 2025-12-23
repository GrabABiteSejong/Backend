package project.pleasemajor.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/api/**").permitAll()// "/api/" 경로는 아무나 출입 가능 (방문증 필요 없음)
            .requestMatchers("/docs/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN") // "/admin/" 경로는 관리자만 출입 가능
            .anyRequest().authenticated() // 나머지 모든 경로는 일단 로그인해야 출입 가능
        )
        .formLogin(form -> form.disable()); // 기본 로그인 페이지 제공
    return http.build();
  }
}