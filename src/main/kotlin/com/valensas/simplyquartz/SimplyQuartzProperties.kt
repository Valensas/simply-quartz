package com.valensas.simplyquartz

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("simplyquartz")
class SimplyQuartzProperties {
    var enabled: Boolean = true
    var packagesToScan: List<String>? = null
}
