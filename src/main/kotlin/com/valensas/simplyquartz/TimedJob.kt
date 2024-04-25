package com.valensas.simplyquartz

import io.micrometer.core.instrument.Metrics
import org.quartz.Job
import org.quartz.JobExecutionContext

abstract class TimedJob : Job {
    private val timer = Metrics.globalRegistry.timer(this.javaClass.name, "job", "name")

    override fun execute(context: JobExecutionContext) {
        timer.record<Unit> { executeTimed(context) }
    }

    protected abstract fun executeTimed(context: JobExecutionContext?)
}
