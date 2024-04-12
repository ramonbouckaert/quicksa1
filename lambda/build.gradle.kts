plugins {
    kotlin("jvm") version "1.9.22"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "1.8.22"
}

repositories {
    maven {
        url = uri("https://repo.osgeo.org/repository/release/")
    }
    maven {
        url = uri("https://packages.atlassian.com/maven-3rdparty/")
    }
    maven {
        url = uri("https://mvn.topobyte.de")
    }
    maven {
        url = uri("https://mvn.slimjars.com")
    }
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))
    implementation("com.amazonaws:aws-lambda-java-core:1.2.2")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("io.bouckaert.quicksa1.lambda.ServePDF")
}

kotlin {
    jvmToolchain(21)
}