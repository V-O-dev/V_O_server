package com.example.v_o_server.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    public static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock applicationClock() {
        return Clock.system(KOREA_ZONE_ID);
    }
}
