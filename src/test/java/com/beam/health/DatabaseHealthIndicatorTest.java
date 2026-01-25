package com.beam.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DatabaseHealthIndicator Tests")
class DatabaseHealthIndicatorTest {

    private DatabaseHealthIndicator healthIndicator;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @BeforeEach
    void setUp() throws SQLException {
        healthIndicator = new DatabaseHealthIndicator(dataSource);
        ReflectionTestUtils.setField(healthIndicator, "slowQueryThresholdMs", 100L);
        ReflectionTestUtils.setField(healthIndicator, "criticalQueryThresholdMs", 500L);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT 1")).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");
        when(databaseMetaData.getDatabaseProductVersion()).thenReturn("2.1.214");
        when(databaseMetaData.getDriverName()).thenReturn("H2 JDBC Driver");
    }

    @Nested
    @DisplayName("health() Tests")
    class HealthTests {

        @Test
        @DisplayName("Should return UP when database is healthy")
        void shouldReturnUpWhenDatabaseIsHealthy() {
            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("responseTimeMs");
            assertThat(health.getDetails().get("database")).isEqualTo("H2");
        }

        @Test
        @DisplayName("Should include database details in health response")
        void shouldIncludeDatabaseDetailsInHealthResponse() {
            Health health = healthIndicator.health();

            assertThat(health.getDetails().get("database")).isEqualTo("H2");
            assertThat(health.getDetails().get("databaseVersion")).isEqualTo("2.1.214");
            assertThat(health.getDetails().get("driverName")).isEqualTo("H2 JDBC Driver");
        }

        @Test
        @DisplayName("Should return DOWN when connection fails")
        void shouldReturnDownWhenConnectionFails() throws SQLException {
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails().get("error")).isEqualTo("Database connection failed");
            assertThat(health.getDetails().get("message")).isEqualTo("Connection refused");
        }

        @Test
        @DisplayName("Should return DOWN when query fails")
        void shouldReturnDownWhenQueryFails() throws SQLException {
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("Query timeout"));

            Health health = healthIndicator.health();

            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails().get("error")).isEqualTo("Database connection failed");
        }
    }
}
