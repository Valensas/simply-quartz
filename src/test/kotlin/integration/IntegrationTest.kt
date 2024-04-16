package integration

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.valensas.simplyquartz.QuartzSchedule

@EnableConfigurationProperties
@SpringBootApplication
class IntegrationTest {






}


@QuartzSchedule(cron = "0/5 * * * * ?")
class SampleJob : Job {
    override fun execute(context: JobExecutionContext) {
        println("Hello World!")
    }
}


fun main(args: Array<String>) {
    runApplication<IntegrationTest>(*args)

}