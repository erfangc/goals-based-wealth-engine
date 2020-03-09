package io.github.erfangc.dataaccess

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
        val url = "jdbc:postgresql://localhost:5432/erfangchen"
        val username = "postgres"
        val password = ""
        return DriverDataSource(
                url,
                "org.postgresql.Driver",
                Properties(),
                username,
                password
        )

    }
}