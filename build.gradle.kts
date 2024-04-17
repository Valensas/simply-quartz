import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.4"
    id("org.jmailen.kotlinter") version "4.2.0"
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
    implementation("org.reflections:reflections:0.10.2")

    // Job Store
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.mchange:c3p0")

    // Test
    testImplementation("org.flywaydb:flyway-core")
    testRuntimeOnly("org.postgresql:postgresql")

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
