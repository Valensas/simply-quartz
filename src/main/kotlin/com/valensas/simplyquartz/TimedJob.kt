package com.valensas.simplyquartz

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.PersistJobDataAfterExecution
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

abstract class TimedJob : Job {

    private val timers = mutableMapOf<String, Timer>()

    private var logger = LoggerFactory.getLogger(this.javaClass.name)

    override fun execute(context: JobExecutionContext) {
        val result = measureTimedValue { kotlin.runCatching { executeTimed(context) } }

        val jobName = this.javaClass.simpleName
        val jobGroup = this.javaClass.`package`.name
        val exceptionClass = result.value.exceptionOrNull()?.javaClass?.simpleName ?: "none"
        val key = "$jobName-$jobGroup-$exceptionClass"

        val timer = timers.getOrPut(key) {
            Metrics.globalRegistry.timer(
                "quartz_scheduled_job",
                "job_name",
                jobName,
                "job_group",
                jobGroup,
                "exception",
                exceptionClass
            )
        }

        timer.record(result.duration.toJavaDuration())
        
        logger.info("Job $jobName-$jobGroup executed in ${result.duration}")
    }

    protected abstract fun executeTimed(context: JobExecutionContext)
}
