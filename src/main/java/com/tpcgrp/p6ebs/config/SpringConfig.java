// Spring Configuration
package com.tpcgrp.p6ebs.config;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import com.tpcgrp.p6ebs.service.EbsProjectService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import com.tpcgrp.p6ebs.service.integration.*;
import org.springframework.context.annotation.Bean;

@Configuration
@ComponentScan(basePackages = "com.tpcgrp.p6ebs")
public class SpringConfig {
    /**
     * Integration service beans
     */
    @Bean
    public EbsProjectService ebsProjectService(DatabaseService databaseService) {
        return new EbsProjectService(databaseService);
    }
}