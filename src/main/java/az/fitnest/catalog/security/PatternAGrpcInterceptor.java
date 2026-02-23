/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.grpc.CallOptions
 *  io.grpc.Channel
 *  io.grpc.ClientCall
 *  io.grpc.ClientCall$Listener
 *  io.grpc.ClientInterceptor
 *  io.grpc.ForwardingClientCall$SimpleForwardingClientCall
 *  io.grpc.Metadata
 *  io.grpc.Metadata$AsciiMarshaller
 *  io.grpc.Metadata$Key
 *  io.grpc.MethodDescriptor
 *  io.grpc.ServerCall
 *  io.grpc.ServerCall$Listener
 *  io.grpc.ServerCallHandler
 *  io.grpc.ServerInterceptor
 *  jakarta.servlet.http.HttpServletRequest
 *  net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor
 *  net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.security.authentication.UsernamePasswordAuthenticationToken
 *  org.springframework.security.core.Authentication
 *  org.springframework.security.core.authority.SimpleGrantedAuthority
 *  org.springframework.security.core.context.SecurityContextHolder
 *  org.springframework.stereotype.Component
 *  org.springframework.web.context.request.RequestContextHolder
 *  org.springframework.web.context.request.ServletRequestAttributes
 */
package az.fitnest.catalog.security;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@GrpcGlobalServerInterceptor
@GrpcGlobalClientInterceptor
public class PatternAGrpcInterceptor
implements ServerInterceptor,
ClientInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PatternAGrpcInterceptor.class);
    private static final Metadata.Key<String> X_USER_ID = Metadata.Key.of("x-user-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_TENANT_ID = Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_SCOPES = Metadata.Key.of("x-scopes", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_REQUEST_ID = Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_FROM_GATEWAY = Metadata.Key.of("x-from-gateway", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_SERVICE_NAME = Metadata.Key.of("x-service-name", Metadata.ASCII_STRING_MARSHALLER);

    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String gatewayFlag = (String)headers.get(X_FROM_GATEWAY);
        String userIdStr = (String)headers.get(X_USER_ID);
        String requestId = (String)headers.get(X_REQUEST_ID);
        String caller = (String)headers.get(X_SERVICE_NAME);
        if ("1".equals(gatewayFlag) && userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                String scopes = (String)headers.get(X_SCOPES);
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (scopes != null && !scopes.isBlank()) {
                    authorities = Arrays.stream(scopes.split(" ")).map(s -> s.startsWith("ROLE_") ? s : "ROLE_" + s).map(SimpleGrantedAuthority::new).collect(Collectors.toList());
                } else {
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                auth.setDetails("PatternA:gRPC:" + caller + ":" + requestId);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authenticated gRPC user {} via Pattern A from {}", userId, caller);
            }
            catch (Exception e) {
                log.warn("Failed to parse Pattern A headers in gRPC call", e);
            }
        }
        return next.startCall(call, headers);
    }

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)){

            public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    PatternAGrpcInterceptor.this.propagate(request, headers, "X-User-Id", X_USER_ID);
                    PatternAGrpcInterceptor.this.propagate(request, headers, "X-Tenant-Id", X_TENANT_ID);
                    PatternAGrpcInterceptor.this.propagate(request, headers, "X-Scopes", X_SCOPES);
                    PatternAGrpcInterceptor.this.propagate(request, headers, "X-Request-Id", X_REQUEST_ID);
                    PatternAGrpcInterceptor.this.propagate(request, headers, "X-From-Gateway", X_FROM_GATEWAY);
                    headers.put(X_SERVICE_NAME, "catalog-service");
                }
                super.start(responseListener, headers);
            }
        };
    }

    private void propagate(HttpServletRequest request, Metadata headers, String headerName, Metadata.Key<String> key) {
        String val = request.getHeader(headerName);
        if (val != null && !val.isBlank()) {
            headers.put(key, val);
        }
    }
}

