plugins {
    kotlin("jvm") version "1.8.22"
}

repositories {
    maven {
        url = uri("https://repo.osgeo.org/repository/release/")
    }
    maven {
        url = uri("https://packages.atlassian.com/maven-3rdparty/")
    }
    mavenCentral()
}

dependencies {
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("org.geotools:gt-main:29.0")
    implementation("org.geotools:gt-tile-client:29.0")
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("net.postgis:postgis-jdbc:2021.1.0")
    implementation("io.ktor:ktor-client-jvm:2.2.4")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}