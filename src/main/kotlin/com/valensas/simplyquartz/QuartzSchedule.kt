package com.valensas.simplyquartz

import org.springframework.scheduling.annotation.Scheduled

@Scheduled
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class QuartzSchedule(

    val cron: String = "",

    val enabled: String = "true",

    val jobName: String = "",

    val jobGroup: String = ""
)
