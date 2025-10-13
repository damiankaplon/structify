val kotlin_version: String by project
val ktor_version: String by project
val logback_version: String by project
val postgresql_version: String = "42.7.4"
val exposed_version: String = "0.58.0"

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    kotlin("plugin.serialization")
    kotlin("kapt") version "2.1.20"
    id("io.ktor.plugin") version "3.0.1"
    id("org.flywaydb.flyway") version "11.1.1"
}

group = "io.structify"


buildscript {
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:11.1.1")
        val postgresql_version: String by project
        classpath("org.postgresql:postgresql:42.7.4")
    }
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":domain"))
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-config-yaml-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-client-core-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.postgresql:postgresql:$postgresql_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-json:$exposed_version")
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("org.apache.kafka:kafka-clients:3.9.0")
    implementation("com.google.dagger:dagger:2.56.2")
    kapt("com.google.dagger:dagger-compiler:2.56.2")


    testImplementation(testFixtures(project(":domain")))
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm")
    testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
}

flyway {
    driver = "org.postgresql.Driver"
    url = getEnvOrProperty("FLYWAY_DB_URL") ?: "jdbc:postgresql://localhost:5432/structify"
    user = getEnvOrProperty("FLYWAY_DB_USER") ?: "structify"
    password = getEnvOrProperty("FLYWAY_DB_PASSWORD") ?: "structify"
    baselineOnMigrate = true
}

fun getEnvOrProperty(value: String): String? = System.getenv(value) ?: System.getProperty(value)
