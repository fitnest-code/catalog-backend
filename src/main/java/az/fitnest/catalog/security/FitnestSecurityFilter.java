/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.authority.SimpleGrantedAuthority
 *  org.springframework.security.core.context.SecurityContextHolder
 *  org.springframework.stereotype.Component
 *  org.springframework.web.filter.OncePerRequestFilter
 */
package az.fitnest.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FitnestSecurityFilter
extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr != null && !userIdStr.isBlank()) {
            try {
                String rolesToParse;
                Long userId = Long.parseLong(userIdStr);
                String scopes = request.getHeader("X-Scopes");
                String rolesHeader = request.getHeader("X-User-Roles");
                ArrayList<String> roles = new ArrayList<String>();
                String string = rolesToParse = scopes != null && !scopes.isBlank() ? scopes : rolesHeader;
                if (rolesToParse != null && !rolesToParse.isBlank()) {
                    roles.addAll(Arrays.stream(rolesToParse.split("[,\\s]+")).map(String::trim).filter(r -> !r.isEmpty()).map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r).toList());
                } else {
                    roles.add("ROLE_USER");
                }
                if (request.getRequestURI().startsWith("/api/v1/internal")) {
                    roles.add("ROLE_INTERNAL");
                }
                List authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken((Object)userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication((Authentication)auth);
            }
            catch (NumberFormatException numberFormatException) {
                // empty catch block
            }
        }
        filterChain.doFilter((ServletRequest)request, (ServletResponse)response);
    }
}

