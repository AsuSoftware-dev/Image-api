package com.asusoftware.image_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serving images from a directory located at /var/www/images/
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/");
    }
}