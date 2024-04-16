package org.valensas.simplyquartz

import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.JobBuilder
import org.quartz.JobKey
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.reflections.Reflections
import org.springframework.context.ApplicationContext

import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

// Need to get the main application class to determine the root package for job scanning at runtime
@Component
class MainClassHolder : ApplicationListener<ApplicationStartedEvent> {
    var mainApplicationClass: Class<*>? = null

    override fun onApplicationEvent(event: ApplicationStartedEvent) {
        mainApplicationClass = event.springApplication.mainApplicationClass
    }
}

@ConditionalOnProperty("simplyquartz.enabled", havingValue = "true")
class JobScheduler(
    private val scheduler: Scheduler,
    private val applicationContext: ApplicationContext,
    private val simplyQuartzProperties: SimplyQuartzProperties,
    private val mainClassHolder: MainClassHolder
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("${applicationContext.environment.getProperty("simplyquartz.defaultJobsGroupName")}")
        logger.info("Scheduling jobs")
        scanAndScheduleJobs()
    }

    private fun scanAndScheduleJobs() {
        val jobsSearchRootPackage = determineJobsSearchPathRootPackage()
        val reflections = Reflections(jobsSearchRootPackage)
        reflections.getSubTypesOf(Job::class.java).forEach { jobClass ->
            jobClass.getAnnotation(QuartzSchedule::class.java)?.let { scheduleAnnotation ->
                val jobName =
                    resolveEnvironmentPlaceholders(scheduleAnnotation.jobName)
                        .ifBlank { jobClass.simpleName }
                val jobGroup =
                    resolveEnvironmentPlaceholders(scheduleAnnotation.jobGroup)
                        .ifBlank { resolveEnvironmentPlaceholders(simplyQuartzProperties.defaultJobsGroupName) }
                val cronExpression = resolveEnvironmentPlaceholders(scheduleAnnotation.cron)
                scheduleJob(jobClass, jobName, jobGroup, cronExpression)
            }
        }
    }

    private fun resolveEnvironmentPlaceholders(value: String): String {
        return applicationContext.environment.resolvePlaceholders(value)
    }

    // Get the root package for job scanning, defaulting to the package of the main application class
    private fun determineJobsSearchPathRootPackage(): String {
        return simplyQuartzProperties.jobsSearchPathRootPackage
            ?: mainClassHolder.mainApplicationClass?.`package`?.name
            ?: throw IllegalStateException("Unable to determine base package for job scanning")
    }

    private fun scheduleJob(clazz: Class<out Job>, jobName: String, jobGroup: String, cronExpression: String) {
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
