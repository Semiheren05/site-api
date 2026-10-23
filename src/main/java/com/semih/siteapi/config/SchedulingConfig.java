package com.semih.siteapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled (nightly clean-ups). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
