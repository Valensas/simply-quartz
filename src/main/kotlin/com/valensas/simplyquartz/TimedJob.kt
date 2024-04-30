package com.valensas.simplyquartz

import io.micrometer.core.instrument.ImmutableTag
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
        val exceptionClass = result.value.exceptionOrNull()?.javaClass?.simpleName
        val key = "$jobName-$jobGroup-$exceptionClass"

        val timer = timers.getOrPut(key) {
            val tags = listOfNotNull(
                ImmutableTag("job", jobName),
                ImmutableTag("group", jobGroup),
                exceptionClass?.let { ImmutableTag("exception", it) }
            )

            val timerName = if (exceptionClass == null) "qrtz_scheduled_job" else "qrtz_scheduled_job_exception"

            Metrics.globalRegistry.timer(
                timerName,
                tags
            )
        }

        timer.record(result.duration.toJavaDuration())
    }

    protected abstract fun executeTimed(context: JobExecutionContext?)
}
