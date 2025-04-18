package com.medisync.authservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisync.authservice.dto.ApiResponseDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

//JwtAuthenticationFilter
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if(
                request.getServletPath().equals("/api/v1/users/signup") ||
                        request.getServletPath().equals("/api/v1/users/login")

        ){
            chain.doFilter(request,response);
        }
        else{
            request.getHeaderNames();
            System.out.println(request.getHeaderNames());
            final String authHeader = request.getHeader("Authorization");
            logger.info(authHeader);
            String email = null;
            String jwt = null;
            String role = null;

            // Extract JWT token from Authorization header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
                // Extract email from JWT token
                try{
                    email = jwtUtil.extractEmail(jwt);
                    role = jwtUtil.extractRole(jwt);
                    // Check if email is not null and there is no existing authentication
                    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // Validate the JWT token
                        if (jwtUtil.isTokenValid(jwt, email, role)) {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(email, null, new ArrayList<>());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            // Set authentication in security context
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }else {
                            sendUnauthorizedResponse(response, "03", "Not a valid token");
                            return;
                        }
                    }
                }catch (Exception e){
                    sendUnauthorizedResponse(response, "03", "Exception in authorization");
                    return;
                }
            }else {
                sendUnauthorizedResponse(response, "03", "No header");
                return;
            }
            chain.doFilter(request, response);
        }
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String responseCode, String responseMsg)
            throws IOException {
        ApiResponseDTO apiResponseDTO = ApiResponseDTO.builder()
                .responseCode(responseCode)
                .responseMsg(responseMsg)
                .content(null)
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403 status code
        response.setContentType("application/json");

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(apiResponseDTO));
    }
}
