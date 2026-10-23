package com.library.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证过滤器: 解析 Authorization: Bearer <token>
 * - token 非法/过期/已拉黑 -> 401 直接踢出
 * - 合法 -> 写入 SecurityContext
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                // token 已拉黑(登出/异常) -> 踢出
                if (blacklistService.isBlacklisted(claims.getId())) {
                    writeUnauthorized(response, "登录状态已失效");
                    return;
                }
                UserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                // 将 token 声明放入 details, 供登出时拉黑 jti 使用
                authentication.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // token 非法 / 过期 / 用户被删 -> 踢出
                writeUnauthorized(response, "未登录或登录已过期");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8).length > 0
                ? "{\"code\":401,\"message\":\"" + message + "\",\"data\":null}"
                : "{}");
    }
}
