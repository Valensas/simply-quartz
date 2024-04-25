import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("org.springframework.boot") version "3.2.4"
    id("org.jmailen.kotlinter") version "4.3.0"
    id("io.spring.dependency-management") version "1.1.4"

    id("maven-publish")
    id("java-library")
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.3"

    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"
}

group = "com.valensas"
version = "0.1.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
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
    implementation("io.micrometer:micrometer-core:1.12.4")
}

tasks.getByName<Jar>("jar") {
    archiveClassifier.set("")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

signing {
    val keyId = System.getenv("SIGNING_KEYID")
    val secretKey = System.getenv("SIGNING_SECRETKEY")
    val passphrase = System.getenv("SIGNING_PASSPHRASE")

    useInMemoryPgpKeys(keyId, secretKey, passphrase)
}

centralPortal {
    username = System.getenv("SONATYPE_USERNAME")
    password = System.getenv("SONATYPE_PASSWORD")

    pom {
        name = "Simply Quartz"
        description = "An utility library that adds features to Spring Quartz"
        url = "https://valensas.com/"
        scm {
            url = "https://github.com/Valensas/simply-quartz"
        }

        licenses {
            license {
                name.set("MIT License")
                url.set("https://mit-license.org")
            }
        }

        developers {
            developer {
                id.set("0")
                name.set("Valensas")
                email.set("info@valensas.com")
            }
        }
    }
}