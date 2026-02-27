/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.Filter
 *  org.springframework.context.annotation.Bean
 *  org.springframework.context.annotation.Configuration
 *  org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
 *  org.springframework.security.config.annotation.web.builders.HttpSecurity
 *  org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 *  org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
 *  org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer$AuthorizedUrl
 *  org.springframework.security.config.annotation.web.configurers.HeadersConfigurer$CacheControlConfig
 *  org.springframework.security.config.http.SessionCreationPolicy
 *  org.springframework.security.web.SecurityFilterChain
 *  org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
 *  org.springframework.web.cors.CorsConfiguration
 *  org.springframework.web.cors.CorsConfigurationSource
 *  org.springframework.web.cors.UrlBasedCorsConfigurationSource
 */
package az.fitnest.catalog.configuration;

import az.fitnest.catalog.security.FitnestSecurityFilter;
import jakarta.servlet.Filter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final FitnestSecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return (SecurityFilterChain)http.csrf(AbstractHttpConfigurer::disable).cors(AbstractHttpConfigurer::disable).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).headers(headers -> headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable)).authorizeHttpRequests(auth -> ((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)((AuthorizeHttpRequestsConfigurer.AuthorizedUrl)auth.requestMatchers(new String[]{"/api/v1/internal/**"})).hasRole("INTERNAL").requestMatchers(new String[]{"/api/v1/stores/admin/**"})).hasRole("ADMIN").requestMatchers(new String[]{"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"})).permitAll().requestMatchers(new String[]{"/actuator/**", "/health/**"})).permitAll().requestMatchers(new String[]{"/error"})).permitAll().anyRequest()).authenticated()).addFilterBefore((Filter)this.securityFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN\nROLE_ADMIN > ROLE_USER");
        return hierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    public SecurityConfig(FitnestSecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }
}

