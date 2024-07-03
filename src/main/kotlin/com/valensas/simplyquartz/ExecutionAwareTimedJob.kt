package com.valensas.simplyquartz

import org.quartz.JobExecutionContext
import org.quartz.PersistJobDataAfterExecution
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

@PersistJobDataAfterExecution
abstract class ExecutionAwareTimedJob : TimedJob() {


    private val logger = LoggerFactory.getLogger(javaClass)

    override fun executeTimed(context: JobExecutionContext) {

        var success = true
        var lastErrorMessage = ""

        try {
            executeAwareTimed(context)
        } catch (e: Exception) {

            logger.error("Error in job", e)

            success = false
            lastErrorMessage = e.message ?: "Unknown error"
        }

        val lastFireTime = context.fireTime
        val lastFinishTime = Date.from(Instant.now())
        val lastDuration = lastFinishTime.time - lastFireTime?.time!!

        context.jobDetail.jobDataMap["lastFireTime"] = lastFireTime
        context.jobDetail.jobDataMap["lastFinishTime"] = lastFinishTime
        context.jobDetail.jobDataMap["lastDuration"] = lastDuration
        context.jobDetail.jobDataMap["lastSuccess"] = success
        context.jobDetail.jobDataMap["lastErrorMessage"] = lastErrorMessage
        context.jobDetail.jobDataMap["executionId"] = UUID.randomUUID().toString()

    }

    abstract fun executeAwareTimed(context: JobExecutionContext)

}