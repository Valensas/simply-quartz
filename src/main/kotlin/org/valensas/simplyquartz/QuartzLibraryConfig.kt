package org.valensas.simplyquartz

import javax.sql.DataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(SimplyQuartzProperties::class)
class QuartzLibraryConfig {
    @Bean(name = ["quartz_ds"])
    @QuartzDataSource
    @ConfigurationProperties(prefix = "spring.datasource")
    fun quartzDataSource(): DataSource = DataSourceBuilder.create().build()
}