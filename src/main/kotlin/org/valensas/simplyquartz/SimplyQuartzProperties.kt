package org.valensas.simplyquartz

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties("simplyquartz")
class SimplyQuartzProperties {
    var enabled: Boolean = true
    var jobsSearchPathRootPackage : String? = null
    var defaultJobsGroupName : String = "main"
}

