package com.valensas.simplyquartz

import io.micrometer.core.instrument.Metrics
import org.quartz.Job
import org.quartz.JobExecutionContext
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

abstract class TimedJob : Job {

    private val timers = mutableMapOf<String, io.micrometer.core.instrument.Timer>()

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
    }

    protected abstract fun executeTimed(context: JobExecutionContext?)
}
