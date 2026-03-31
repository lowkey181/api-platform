package com.api.apiadmin.config;

import cn.hutool.core.util.StrUtil;
import com.api.apiadmin.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 获取请求头 token
        String token_Bearer  = request.getHeader("Authorization");
        System.out.println("token:"+token_Bearer);
        // 没有 token 直接放行，走游客模式
        if (StrUtil.isBlank(token_Bearer)) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = token_Bearer;
        if (token_Bearer.startsWith("Bearer ")){
            token = token_Bearer.substring(7);
        }

        // 解析 token
        System.out.println("token开始解析:"+token);
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            response.setStatus(401);
            System.out.println("token 解析失败401");
            return;
        }
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);
        System.out.println("userId"+userId+"role"+role+"username"+claims.get("username",String.class));

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if ("ADMIN".equals(role)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        // 把角色交给 Spring Security
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities); // 👈 加权限

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 放行
        filterChain.doFilter(request, response);
    }
}