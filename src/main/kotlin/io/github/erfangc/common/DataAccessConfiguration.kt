package io.github.erfangc.common

import com.zaxxer.hikari.util.DriverDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.*
import javax.sql.DataSource

@Configuration
class DataAccessConfiguration {
    @Bean
    fun jdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(dataSource)
    }

    @Bean
    fun dataSource(): DataSource {
        val url = System.getenv("JDBC_DATABASE_URL")
        val username = System.getenv("DATABASE_USERNAME")
        val password = System.getenv("DATABASE_PASSWORD")
        return DriverDataSource(
                url,
                "org.postgresql.Driver",
                Properties(),
                username,
                password
        )

    }
}