package io.github.erfangc.dataaccess

import com.zaxxer.hikari.util.DriverDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*
import javax.sql.DataSource

@Configuration
class DataAccessConfiguration {
    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

    @Bean
    fun dataSource(): DataSource {
        return DriverDataSource(
                "jdbc:postgresql://localhost:5432/postgres",
                "org.postgresql.Driver",
                Properties(),
                "postgres",
                ""
        )
    }
}