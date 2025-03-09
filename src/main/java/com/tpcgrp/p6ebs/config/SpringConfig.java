// Spring Configuration
package com.tpcgrp.p6ebs.config;

import com.tpcgrp.p6ebs.service.ConfigurationService;
import com.tpcgrp.p6ebs.service.DatabaseService;
import com.tpcgrp.p6ebs.service.EbsProjectService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import com.tpcgrp.p6ebs.service.integration.*;
import org.springframework.context.annotation.Bean;
import com.tpcgrp.p6ebs.service.P6ProjectService;

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

    @Bean
    public P6ProjectService p6ProjectService(DatabaseService databaseService) {
        return new P6ProjectService(databaseService);
    }
}