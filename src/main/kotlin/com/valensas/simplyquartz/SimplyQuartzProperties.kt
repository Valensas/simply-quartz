package com.valensas.simplyquartz

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("simplyquartz")
class SimplyQuartzProperties {
    var enabled: Boolean = true
    var jobsSearchPathRootPackage : String? = null
    var defaultJobsGroupName : String = "main"
}

