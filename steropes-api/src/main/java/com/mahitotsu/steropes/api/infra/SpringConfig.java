package com.mahitotsu.steropes.api.infra;

import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.SimpleThreadScope;

@Configuration
public class SpringConfig {
    
    @Bean
    public static CustomScopeConfigurer customScopeConfigurer() {

        final CustomScopeConfigurer scopeConfigurer = new CustomScopeConfigurer();
        scopeConfigurer.addScope("threadlocal", new SimpleThreadScope());
        return scopeConfigurer;
    }
}
