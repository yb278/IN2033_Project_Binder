plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.iposca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

javafx {
    version = "23"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    // Database
    implementation("mysql:mysql-connector-java:8.0.33")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "com.iposca.Main"
}

tasks.test {
    useJUnitPlatform()
}