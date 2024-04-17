package com.valensas.simplyquartz

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("simplyquartz")
class SimplyQuartzProperties {
    var enabled: Boolean = true
    var packageToScan: String? = null
    var defaultJobGroupName: String = "main"
}
