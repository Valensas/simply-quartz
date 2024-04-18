package com.valensas.simplyquartz

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class QuartzSchedule(
    val cron: String,
    val jobName: String = "",
    val jobGroup: String = ""
)
