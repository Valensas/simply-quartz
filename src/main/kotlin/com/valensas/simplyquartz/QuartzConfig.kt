package com.valensas.simplyquartz

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.quartz.QuartzDataSource
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource


@Configuration
class QuartzConfig {

    var logger = LoggerFactory.getLogger(javaClass)

    @Bean(name = ["quartz_ds"])
    @QuartzDataSource
    fun quartzDataSource(): DataSource {
        logger.info("Creating quartz data source")
        return DataSourceBuilder.create().build()
    }
}