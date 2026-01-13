val kotlin_version: String by project

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

repositories {
    mavenCentral()
}

group = "io.structify"


dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:6.0.2")
}
