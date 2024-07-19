package com.valensas.simplyquartz

import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.matchers.GroupMatcher
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener


@Configuration
@ConditionalOnProperty("simplyquartz.enabled", havingValue = "true")
@EnableConfigurationProperties(SimplyQuartzProperties::class)
class JobScheduler(
    private val scheduler: Scheduler,
    private val applicationContext: ApplicationContext,
    private val jobs: List<Job>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("Scheduling jobs")

        val existingJobKeysSet = scheduler.getJobKeys(GroupMatcher.anyGroup()).toSet()
        val newJobKeysSet = mutableSetOf<JobKey>()

        jobs.forEach {
            val jobClass = it.javaClass
            jobClass.getAnnotation(QuartzSchedule::class.java)?.let { scheduleAnnotation ->
                val jobName = resolveEnvironmentPlaceholders(scheduleAnnotation.jobName)
                    .ifBlank { jobClass.simpleName }
                val jobGroup = resolveEnvironmentPlaceholders(scheduleAnnotation.jobGroup)
                    .ifBlank { jobClass.`package`.name }

                val jobKey = JobKey.jobKey(jobName, jobGroup)

                val enabled = resolveEnvironmentPlaceholders(scheduleAnnotation.enabled).toBoolean()

                if (!enabled) {
                    logger.info("Job $jobKey is disabled")
                    return@let
                }

                val cronExpression = resolveEnvironmentPlaceholders(scheduleAnnotation.cron)

                cronExpression.ifBlank { throw IllegalArgumentException("No cron expression provided for job $jobKey") }

                scheduleCronJob(jobClass, jobKey, cronExpression)

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
            logger.info("Rescheduling job ${jobKey.name} with schedule $cronExpression")
            scheduler.rescheduleJob(newTrigger.key, newTrigger)
        } else {
            logger.info("Scheduling job  ${jobKey.name} with schedule $cronExpression")
            scheduler.scheduleJob(jobDetail, newTrigger)
        }
    }
}
