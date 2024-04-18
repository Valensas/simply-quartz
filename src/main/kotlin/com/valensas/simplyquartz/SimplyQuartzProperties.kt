package com.valensas.simplyquartz

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("simplyquartz")
class SimplyQuartzProperties {
    var enabled: Boolean = true
    var packagesToScan: List<String>? = null
}
