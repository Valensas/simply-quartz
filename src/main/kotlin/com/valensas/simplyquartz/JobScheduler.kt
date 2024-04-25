package com.valensas.simplyquartz

import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder
import org.quartz.TriggerBuilder
import org.quartz.impl.matchers.GroupMatcher
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import java.util.concurrent.TimeUnit

// Need to get the main application class to determine the root package for job scanning at runtime
@Configuration
class MainClassHolder : ApplicationListener<ApplicationStartedEvent> {
    var mainApplicationClass: Class<*>? = null

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        mainApplicationClass = event.springApplication.mainApplicationClass
    }
}

@Configuration
@ConditionalOnProperty("simplyquartz.enabled", havingValue = "true")
@EnableConfigurationProperties(SimplyQuartzProperties::class)
class JobScheduler(
    private val scheduler: Scheduler,
    private val applicationContext: ApplicationContext,
    private val simplyQuartzProperties: SimplyQuartzProperties,
    private val mainClassHolder: MainClassHolder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("Scheduling jobs")
        scanAndScheduleJobs()
    }

    private fun scanAndScheduleJobs() {
        val existingJobKeysSet = scheduler.getJobKeys(GroupMatcher.anyGroup()).toSet()
        val newJobKeysSet = mutableSetOf<JobKey>()

        val jobsSearchPackages = determineJobsSearchPathRootPackage()
        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackages(*jobsSearchPackages.toTypedArray())
        )

        reflections.getSubTypesOf(Job::class.java).forEach { jobClass ->
            jobClass.getAnnotation(QuartzSchedule::class.java)?.let { scheduleAnnotation ->
                val jobName =
                    resolveEnvironmentPlaceholders(scheduleAnnotation.jobName)
                        .ifBlank { jobClass.simpleName }
                val jobGroup =
                    resolveEnvironmentPlaceholders(scheduleAnnotation.jobGroup)
                        .ifBlank { jobClass.`package`.name }

                val jobKey = JobKey.jobKey(jobName, jobGroup)

                val cronExpression = resolveEnvironmentPlaceholders(scheduleAnnotation.cron)

                val fixedDelayString = resolveEnvironmentPlaceholders(scheduleAnnotation.fixedDelayString)

                val initialDelayString = resolveEnvironmentPlaceholders(scheduleAnnotation.initialDelayString)

                val fixedDelay = scheduleAnnotation.fixedDelay

                val initialDelay = scheduleAnnotation.initialDelay

                val fixedDelayParameterSet = fixedDelayString != "" || fixedDelay != -1L
                val initialDelayParameterSet = initialDelayString != "" || initialDelay != -1L
                val cronParameterSet = cronExpression != ""

                if (fixedDelayParameterSet && cronParameterSet) {
                    throw IllegalArgumentException("Both fixed delay and cron parameters are set for job $jobName")
                }

                if (initialDelayParameterSet && cronParameterSet) {
                    throw IllegalArgumentException("Both initial delay and cron parameters are set for job $jobName")
                }

                if (fixedDelayString != "" || fixedDelay != -1L) {
                    var finalFixedDelay = if (fixedDelayString != "") {
                        fixedDelayString.toLongOrNull()
                    } else {
                        fixedDelay
                    }

                    if (finalFixedDelay == null) {
                        throw IllegalArgumentException("Invalid fixed delay string value for job $jobName")
                    }

                    if (finalFixedDelay == -1L) {
                        logger.info("Skipping disabled fixed delay job $jobName")
                        return@let
                    }

                    if (scheduleAnnotation.timeUnit != TimeUnit.MILLISECONDS) {
                        finalFixedDelay = scheduleAnnotation.timeUnit.toMillis(finalFixedDelay)
                    }

                    var finalInitialDelay = if (initialDelayString != "") {
                        initialDelayString.toLongOrNull()
                    } else {
                        initialDelay
                    }

                    if (finalInitialDelay == null) {
                        throw IllegalArgumentException("Invalid initial delay string value for job $jobName")
                    }

                    if (finalInitialDelay == -1L) {
                        finalInitialDelay = 0L
                    }

                    scheduleFixedDelayJob(jobClass, jobName, jobGroup, finalInitialDelay, finalFixedDelay)
                } else if (cronExpression != "") {
                    if (cronExpression == scheduleAnnotation.CRON_DISABLED) {
                        logger.info("Skipping disabled cron job $jobName")
                        return@let
                    }

                    scheduleCronJob(jobClass, jobName, jobGroup, cronExpression)
                } else {
                    throw IllegalArgumentException("No scheduling parameters provided for job $jobName")
                }

                newJobKeysSet.add(jobKey)
            }
        }

        val jobKeysToBeRemoved = existingJobKeysSet.minus(newJobKeysSet)

        jobKeysToBeRemoved.forEach { jobKey ->
            logger.info("Removing undeclared job ${jobKey.name}")
            scheduler.deleteJob(jobKey)
        }

        logger.info("Starting scheduler")

        scheduler.start()
    }

    private fun resolveEnvironmentPlaceholders(value: String): String {
        return applicationContext.environment.resolvePlaceholders(value)
    }

    // Get the root package for job scanning, defaulting to the package of the main application class
    private fun determineJobsSearchPathRootPackage(): List<String> {
        return simplyQuartzProperties.packagesToScan
            ?: mainClassHolder.mainApplicationClass?.`package`?.name?.let { listOf(it) }
            ?: throw IllegalStateException("Unable to determine base package for job scanning")
    }

    private fun scheduleFixedDelayJob(
        jobClass: Class<out Job>?,
        jobName: String?,
        jobGroup: String?,
        finalInitialDelay: Long,
        finalFixedDelay: Long
    ) {
        val jobKey = JobKey.jobKey(jobName, jobGroup)
        val newTrigger = TriggerBuilder.newTrigger()
            .withIdentity(jobName, jobGroup)
            .forJob(jobName, jobGroup)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(finalFixedDelay).repeatForever())
            .startAt(java.util.Date(System.currentTimeMillis() + finalInitialDelay))
            .build()
        val jobDetail = JobBuilder.newJob(jobClass)
            .requestRecovery(false)
            .withIdentity(jobName, jobGroup)
            .storeDurably()
            .build()

        if (scheduler.checkExists(jobKey)) {
            scheduler.rescheduleJob(newTrigger.key, newTrigger)
        } else {
            scheduler.scheduleJob(jobDetail, newTrigger)
        }
    }

    private fun scheduleCronJob(clazz: Class<out Job>, jobName: String, jobGroup: String, cronExpression: String) {
        val jobKey = JobKey.jobKey(jobName, jobGroup)
        val newTrigger = TriggerBuilder.newTrigger()
            .withIdentity(jobName, jobGroup)
            .forJob(jobName, jobGroup)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()
        val jobDetail = JobBuilder.newJob(clazz)
            .requestRecovery(false)
            .withIdentity(jobName, jobGroup)
            .storeDurably()
            .build()

        if (scheduler.checkExists(jobKey)) {
            scheduler.rescheduleJob(newTrigger.key, newTrigger)
        } else {
            scheduler.scheduleJob(jobDetail, newTrigger)
        }
    }
}
