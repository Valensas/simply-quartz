package com.valensas.simplyquartz

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Scheduled
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Component
annotation class QuartzSchedule(

    val cron: String = "",

    val enabled: String = "true",

    val jobName: String = "",

    val jobGroup: String = ""
)
