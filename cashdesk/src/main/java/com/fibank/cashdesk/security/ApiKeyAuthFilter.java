//package com.fibank.cashdesk.security;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//public class ApiKeyAuthFilter extends OncePerRequestFilter {
//    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
//
//
//    @Value("${fib.x.auth.header}")
//    private String headerName;
//
//    @Value("${fib.x.auth.key}")
//    private String validApiKey;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException, IOException, ServletException {
//        String apiKey = request.getHeader(headerName);
//        if (apiKey == null || !apiKey.equals(validApiKey)) {
//            logger.warn("Unauthorized request: missing or invalid API key");
//            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
//            return;
//        }
//        filterChain.doFilter(request, response);
//    }
//}
