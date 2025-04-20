package ru.practicum.shareit.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.shareit.filter.ResponseModifierFilter;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<ResponseModifierFilter> responseModifierFilterRegistration(ResponseModifierFilter filter) {
        FilterRegistrationBean<ResponseModifierFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/items/*");
        return registrationBean;
    }
}