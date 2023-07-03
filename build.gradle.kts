import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.version

plugins {
    kotlin("jvm") version "1.8.22" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    kotlin("plugin.serialization") version "1.8.22" apply false
}

group = "io.bouckaert"
version = "0.1-SNAPSHOT"
