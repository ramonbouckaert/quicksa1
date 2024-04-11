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
    implementation("com.github.librepdf:openpdf:1.3.30")
    implementation("org.jetbrains.exposed:exposed-core:0.40.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.40.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.40.1")
    implementation("net.postgis:postgis-jdbc:2021.1.0")
    implementation("io.ktor:ktor-client-jvm:2.2.4")
    implementation("io.ktor:ktor-client-cio-jvm:2.2.4")
    implementation("de.topobyte:jts-drawing-core:0.3.0")
    implementation("de.topobyte:jts-drawing-awt:0.3.0")
    implementation("de.topobyte:jgs:0.0.1")
    implementation("org.locationtech.jts:jts-core:1.16.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}