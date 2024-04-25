package com.valensas.simplyquartz

import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Scheduled()
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class QuartzSchedule(

    val cron: String = "",

    val fixedDelay: Long = -1L,

    val fixedDelayString: String = "",

    val initialDelay: Long = -1L,

    val initialDelayString: String = "",

    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,

    val CRON_DISABLED: String = "-",

    val jobName: String = "",

    val jobGroup: String = ""
)
