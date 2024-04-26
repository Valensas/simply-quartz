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
import java.time.Duration

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

                val enabled = resolveEnvironmentPlaceholders(scheduleAnnotation.enabled).toBoolean()

                if (!enabled) {
                    logger.info("Job $jobKey is disabled")
                    return@let
                }

                handleAnnotation(scheduleAnnotation, jobClass, jobKey)

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

    private fun handleAnnotation(
        scheduleAnnotation: QuartzSchedule,
        jobClass: Class<out Job>,
        jobKey: JobKey
    ) {
        val cronExpression = resolveEnvironmentPlaceholders(scheduleAnnotation.cron)

        val fixedDelayString = resolveEnvironmentPlaceholders(scheduleAnnotation.fixedDelay)

        val initialDelayString = resolveEnvironmentPlaceholders(scheduleAnnotation.initialDelay)

        if (fixedDelayString.isNotBlank() && cronExpression.isNotBlank()) {
            throw IllegalArgumentException("Both fixed delay and cron parameters are set for job $jobKey")
        }

        if (fixedDelayString.isNotBlank()) {
            val fixedDelay = Duration.parse(fixedDelayString).toMillis().runCatching { this }.getOrElse {
                throw IllegalArgumentException("Fixed delay must be a valid duration for job $jobKey , raw value $fixedDelayString")
            }

            val initialDelay = Duration.parse(initialDelayString).toMillis().runCatching { this }.getOrElse {
                throw IllegalArgumentException("Initial delay must be a valid duration for job $jobKey , raw value $initialDelayString")
            }

            if (fixedDelay < 0) {
                throw IllegalArgumentException("Fixed delay must be a positive number for job $jobKey , parsed value $fixedDelay")
            }

            scheduleFixedDelayJob(jobClass, jobKey, initialDelay, fixedDelay)
        } else if (cronExpression.isNotBlank()) {
            scheduleCronJob(jobClass, jobKey, cronExpression)
        } else {
            throw IllegalArgumentException("No scheduling parameters provided for job $jobKey")
        }
    }

    private fun scheduleFixedDelayJob(
        jobClass: Class<out Job>?,
        jobKey: JobKey,
        finalInitialDelay: Long,
        finalFixedDelay: Long
    ) {
        val newTrigger = TriggerBuilder.newTrigger()
            .withIdentity(jobKey.name, jobKey.group)
            .forJob(jobKey)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(finalFixedDelay).repeatForever()
            )
            .startAt(java.util.Date(System.currentTimeMillis() + finalInitialDelay))
            .build()
        val jobDetail = JobBuilder.newJob(jobClass)
            .requestRecovery(false)
            .withIdentity(jobKey)
            .storeDurably()
            .build()

        if (scheduler.checkExists(jobKey)) {
            scheduler.rescheduleJob(newTrigger.key, newTrigger)
        } else {
            scheduler.scheduleJob(jobDetail, newTrigger)
        }
    }

    private fun scheduleCronJob(clazz: Class<out Job>, jobKey: JobKey, cronExpression: String) {
        val newTrigger = TriggerBuilder.newTrigger()
            .withIdentity(jobKey.name, jobKey.group)
            .forJob(jobKey)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
            .build()
        val jobDetail = JobBuilder.newJob(clazz)
            .requestRecovery(false)
            .withIdentity(jobKey)
            .storeDurably()
            .build()

        if (scheduler.checkExists(jobKey)) {
            scheduler.rescheduleJob(newTrigger.key, newTrigger)
        } else {
            scheduler.scheduleJob(jobDetail, newTrigger)
        }
    }
}
