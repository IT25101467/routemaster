package com.routemaster.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${redisson.single-server-config.address}")
    private String address;

    @Value("${redisson.single-server-config.password:}")
    private String password;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        var serverConfig = config.useSingleServer()
                .setAddress(address)
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8)
                .setConnectTimeout(5000)
                .setTimeout(3000);

        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }

        return Redisson.create(config);
    }
}