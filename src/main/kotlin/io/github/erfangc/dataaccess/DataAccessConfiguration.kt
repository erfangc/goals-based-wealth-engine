package io.github.erfangc.dataaccess

import com.zaxxer.hikari.util.DriverDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
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
        return DriverDataSource(
                "jdbc:postgresql://localhost:5432/erfangchen",
                "org.postgresql.Driver",
                Properties(),
                "postgres",
                ""
        )
    }
}