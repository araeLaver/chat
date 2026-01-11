package com.beam;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Performance Configuration
 *
 * <p>Configures performance-related features including:
 * <ul>
 *   <li>Database connection pooling (HikariCP)</li>
 *   <li>Async task execution</li>
 *   <li>Application-level caching (Caffeine)</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Configuration
@EnableAsync
@EnableCaching
public class PerformanceConfig {

    @Value("${spring.datasource.url:jdbc:h2:mem:beamdb}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:sa}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

    @Bean
    @Profile("prod")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceUrl);
        config.setUsername(datasourceUsername);
        config.setPassword(datasourcePassword);
        config.setDriverClassName(driverClassName);

        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        return new HikariDataSource(config);
    }

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("beam-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Cache Manager using Caffeine with fine-grained TTL settings
     *
     * <p>Configures application-level caching with the following caches:
     * <ul>
     *   <li><b>users</b>: User data (30 min TTL, 1000 max entries)</li>
     *   <li><b>chatRooms</b>: Chat room data (15 min TTL, 500 max entries)</li>
     *   <li><b>messages</b>: Recent messages (5 min TTL, 2000 max entries)</li>
     *   <li><b>friends</b>: Friend relationships (10 min TTL, 500 max entries)</li>
     *   <li><b>conversations</b>: DM conversation lists (10 min TTL)</li>
     *   <li><b>unreadCounts</b>: Unread message counts (3 min TTL)</li>
     *   <li><b>translations</b>: Translation results (1 hour TTL)</li>
     * </ul>
     *
     * @return Configured Caffeine cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager() {
            @Override
            protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name) {
                return getCacheBuilder(name).build();
            }
        };

        // Register cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "users",
                "chatRooms",
                "messages",
                "friends",
                "conversations",
                "unreadCounts",
                "translations"
        ));

        return cacheManager;
    }

    /**
     * 캐시별 세분화된 설정
     */
    private Caffeine<Object, Object> getCacheBuilder(String cacheName) {
        return switch (cacheName) {
            case "users" -> Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats();
            case "chatRooms" -> Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
            case "messages" -> Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(2000)
                .recordStats();
            case "friends" -> Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
            case "conversations" -> Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats();
            case "unreadCounts" -> Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats();
            case "translations" -> Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(5000)
                .recordStats();
            default -> Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats();
        };
    }
}