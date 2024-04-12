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
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":shared"))
    implementation("mil.nga.geopackage:geopackage:6.6.1")
    implementation("org.liquibase:liquibase-core:4.12.0")
    implementation("com.github.lonnyj:liquibase-spatial:1.2.1")
    implementation("com.sksamuel.hoplite:hoplite-core:2.7.2")
    implementation("com.sksamuel.hoplite:hoplite-json:2.7.2")
    implementation("com.tersesystems.logback:logback-classic:1.1.1")
    implementation("io.ktor:ktor-client-jvm:2.2.4")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("io.bouckaert:jts2geojson-kotlin:0.21.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("io.bouckaert.quicksa1.utility.Main")
}

kotlin {
    jvmToolchain(21)
}