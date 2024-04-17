package org.valensas.simplyquartz

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import javax.sql.DataSource


@Component
class QuartzConfig {

    var logger = LoggerFactory.getLogger(javaClass)

    @Bean(name = ["quartz_ds"])
    @QuartzDataSource
    @ConfigurationProperties(prefix = "spring.datasource")
    fun quartzDataSource(): DataSource {
        logger.info("Creating quartz data source")
        return DataSourceBuilder.create().build()
    }
}