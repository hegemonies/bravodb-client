import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:5.2.0")
    }
}

plugins {
    kotlin("jvm") version "1.4.21"
    java
    id("com.google.cloud.tools.jib") version "2.8.0"
}

group = "org.bravo"
version = "0.1"
java.sourceCompatibility = JavaVersion.VERSION_11

apply(plugin = "com.github.johnrengelman.shadow")

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-reactive", "1.4.2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.rsocket:rsocket-core:1.1.0")
    implementation("io.rsocket:rsocket-transport-netty:1.1.0")

    implementation("org.apache.logging.log4j", "log4j-api", "2.13.0")
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.0")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.jar {
    manifest {
        attributes(
            Pair("Main-Class", "org.bravo.bravodb.BravodbApplicationKt"),
            Pair("Multi-Release", true)
        )
    }
}

jib {
    container {
        mainClass = "org.bravo.bravodb.BravodbApplicationKt"
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
    to {
        image = "bravo/bravodb-client:${version}"
    }
}
