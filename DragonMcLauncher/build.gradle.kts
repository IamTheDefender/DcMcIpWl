plugins {
    java
    application
    id("com.gradleup.shadow") version "9.3.2"
    id("io.freefair.lombok") version "8.4"
}

group = "dev.dragonmc"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "dev.dragonmc.bot.Main"
}

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    implementation("net.dv8tion:JDA:5.1.2") {
        exclude(module = "opus-java")
    }
    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

tasks.shadowJar {
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "xyz.iamthedefender.dragonmc.Main"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

