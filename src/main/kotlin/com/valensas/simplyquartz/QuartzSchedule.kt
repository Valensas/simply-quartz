package com.valensas.simplyquartz

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class QuartzSchedule(

    val cron: String = "",

    val fixedDelay: String = "",

    val initialDelay: String = "",

    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,

    val enabled: String = "",

    val jobName: String = "",

    val jobGroup: String = ""
)
