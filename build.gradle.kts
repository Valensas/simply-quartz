import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.graalvm.buildtools.native") version "0.9.28"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "org.valensas"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    if (project.hasProperty("GITLAB_REPO_URL")) {
        maven {
            name = "Gitlab"
            url = uri(project.property("GITLAB_REPO_URL").toString())
            credentials(HttpHeaderCredentials::class.java) {
                name = project.findProperty("GITLAB_TOKEN_NAME")?.toString()
                value = project.findProperty("GITLAB_TOKEN")?.toString()
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-quartz:3.2.0")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.reflections:reflections:0.10.2")

    implementation("com.mchange:c3p0")

    implementation("org.flywaydb:flyway-core")


    runtimeOnly("org.postgresql:postgresql")

}



tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootRun {
    doFirst {
        systemProperty("database.user", System.getenv("DATABASE_USER") ?: "db_user")
        systemProperty("database.password", System.getenv("DATABASE_PASSWORD") ?: "db_pass")
        systemProperty("database.endpoint", System.getenv("DATABASE_ENDPOINT") ?: "localhost:5432")
        systemProperty("database.name", System.getenv("DATABASE_NAME") ?: "simplyquartz_test")
    }
}
