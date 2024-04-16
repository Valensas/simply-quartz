package org.valensas.simplyquartz

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class QuartzSchedule(
    val cron: String,
    val jobName: String = "", // Defaults to the class name
    val jobGroup: String = "" // Defaults to the defaultJobGroupName in SchedulerProperties
)