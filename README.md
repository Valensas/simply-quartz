# Simply Quartz
=============
Simply Quartz is a quality of life library for scheduling jobs with cron schedules in spring boot applications. It
provides a simple way to schedule tasks using Quartz Scheduler. It tries to mock the simple usage of @Scheduled
annotation of spring boot.

# Example Usage

Please refer to [Quartz Documentation](https://www.quartz-scheduler.org/documentation/quartz-2.3.0/configuration/) for
configurations and default values.

This example shows how to use simply quartz with postgresql database for clustered mode using hikaricp connection pool.

1) Add simply-quartz and spring boot quartz dependencies to your project

```kts
implementation("org.springframework.boot:spring-boot-starter-quartz:3.2.0")
implementation("com.valensas:simply-quartz:0.2.1")
```

2) Add quartz configuration in your application.properties or application.yml file

```yml
spring:
  quartz:
    auto-startup: false
    job-store-type: jdbc
    jdbc:
      initialize-schema: never
    properties:
      org.quartz.scheduler.instanceId: AUTO
      org.quartz.jobStore.dataSource: quartz_ds
      org.quartz.jobStore.driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
      org.quartz.jobStore.isClustered: true
      org.quartz.jobStore.tablePrefix: qrtz_
      org.quartz.dataSource.quartz_ds.provider: hikaricp
      org.quartz.dataSource.quartz_ds.driver: org.postgresql.Driver
      org.quartz.dataSource.quartz_ds.URL: ${spring.datasource.url}
      org.quartz.dataSource.quartz_ds.user: ${spring.datasource.username}
      org.quartz.dataSource.quartz_ds.password: ${spring.datasource.password}

simplyquartz:
  enabled: true
  packagesToScan: com.example
```

Note: Both simplyquartz properties are optional. Simply Quartz is enabled by default. If you don't provide packagesToScan, simply quartz will scan all the packages
within the same package as your main class, or any subpackages to find classes implementing Job or extending TimedJob. You can provide a list of packages to scan.

**Warning:** If you are using migrations for creating quartz tables, you should set `jdbc.initialize-schema` to `never`
to prevent quartz from creating or possibly overwriting tables itself.

**Warning:** If you are using clustered mode you must set a unique `org.quartz.scheduler.instanceId` for each instance.
You can use `AUTO` to let quartz generate a unique id for you. Otherwise, concurrent executions will happen even if you
use `@DisallowConcurrentExecution`.

3) Add migration for quartz tables

You can use the following sample migration to create quartz tables in your
database: [quartz_migration.sql](https://github.com/Valensas/simply-quartz/blob/main/sample_config/quartz_migration.sql)
Don't forget to change the table prefixes in the migration file according to your configuration. Existing table names
are in accordance to the given configurations.

4) Create a job class

- Normal Job

```kotlin
@QuartzSchedule(cron = "0/5 * * * * ?")
@DisallowConcurrentExecution
class SampleJob : Job {
    override fun execute(context: JobExecutionContext?) {
        println("Hello World!")
    }
}
```

- Timed Job With Micrometer Metrics

```kotlin
@QuartzSchedule(cron = "0/5 * * * * ?")
@DisallowConcurrentExecution
class SampleJob : TimedJob() {
    override fun executeTimed(context: JobExecutionContext?) {
        println("Hello World!")
    }
}
```

Note: `@DisallowConcurrentExecution` belongs to Quartz library, and `@QuartzSchedule` belongs to SimplyQuartz library.
`@DisallowConcurrentExecution` is used to prevent the job from running concurrently. If you don't want to prevent
concurrent execution, you can remove it. This annotation supports clustered mod too and will prevent concurrent
execution across the cluster, if clustered mod is enabled and instances have unique ids.

Jobs that extend `TimedJob` will automatically record the execution time and count metrics using Micrometer.
Simply Quartz will create metrics like this:

name: "quartz_scheduled_job"\
Label 1: "job_name" : jobName\
Label 2: "job_group" : jobGroup\
Label 3: "exception" : exceptionClass

Simply Quartz will use `io.micrometer.core.instrument.Metrics.globalRegistry` to create timers.

5) Extra configurations for jobs

```kotlin
annotation class QuartzSchedule(
    val cron: String = "",
    val enabled: String = "true",
    val jobName: String = "",
    val jobGroup: String = ""
)
```

enabled: If you want to disable the job without removing the annotation, you can set this to false.

jobName: If you want to give a custom name to the job, you can set this. Default is the class name. Affects metrics'
labels.

jobGroup: If you want to give a custom group to the job, you can set this. Default is the full package name. Affects
metrics' labels.

You can use dependency injection and property placeholders in your job classes.

```kotlin
@QuartzSchedule(cron = "\${sample.cron}")
class SampleJob(
    private val sampleService: SampleService,
    @Value("\${sample.value}")
    private val sampleValue: Int?
) : Job {
    override fun execute(context: JobExecutionContext?) {
        println("Hello World!")
        sampleService.doSomething(sampleValue)
    }
}
```