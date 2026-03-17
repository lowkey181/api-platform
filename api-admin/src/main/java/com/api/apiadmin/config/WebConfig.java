//package com.api.apiadmin.config;
//
//import com.api.apiadmin.util.JwtUtil;
//import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    private final JwtUtil jwtUtil;
//
//    public WebConfig(JwtUtil jwtUtil) {
//        this.jwtUtil = jwtUtil;
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(new JwtInterceptor(jwtUtil))
//                .addPathPatterns("/admin/**")
//                .excludePathPatterns("/admin/login");
//    }
//
//    @Bean
//    public RestTemplate restTemplate(RestTemplateBuilder builder) {
//        return builder.build();
//    }
//}
