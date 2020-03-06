package org.onedatashare.server.service;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.onedatashare.server.model.core.ODSConstants.TOKEN_COOKIE_NAME;

@Service
public class ODSSecurityConfigRepository implements ServerSecurityContextRepository {

    @Autowired
    private ODSAuthenticationManager odsAuthenticationManager;

    @Autowired
    private JWTUtil jwtUtil;

    @Override
    public Mono<Void> save(ServerWebExchange serverWebExchange, SecurityContext securityContext) {
        return null;
    }

    public String fetchAuthToken(ServerWebExchange serverWebExchange){
        ServerHttpRequest request = serverWebExchange.getRequest();

        String token = null;
        String endpoint = request.getPath().pathWithinApplication().value().toString();
        //Check for token only when the request needs to be authenticated
        if(endpoint.startsWith("/api/")) {
            try {
                token = request.getCookies().getFirst(TOKEN_COOKIE_NAME).getValue();
            } catch (NullPointerException npe) {
                ODSLoggerService.logError("No token Found for request at " + endpoint);
            }
        }
        return token;
    }

    @Override
    public Mono<SecurityContext> load(ServerWebExchange serverWebExchange) {
        String authToken = this.fetchAuthToken(serverWebExchange);
        try {
            if (authToken != null) {
                String email = jwtUtil.getEmailFromToken(authToken);
                Authentication auth = new UsernamePasswordAuthenticationToken(email, authToken);
                return this.odsAuthenticationManager.authenticate(auth).map(SecurityContextImpl::new);
            }
        }
        catch(ExpiredJwtException e){
            ODSLoggerService.logError("Token Expired");
        }
        return Mono.empty();
    }
}
