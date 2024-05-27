plugins {
    kotlin("jvm") version "1.9.21"
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.hugo.thankmaslobby"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven(url = "https://jitpack.io")
    maven(url = "https://repo.papermc.io/repository/maven-public/")
    maven(url = "https://maven.citizensnpcs.co/repo")
}

val exposedVersion: String by project
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")

    // Citizens API
    compileOnly("net.citizensnpcs:citizens-main:2.0.33-SNAPSHOT") {
        exclude(mutableMapOf("group" to "*", "module" to "*"))
    }

    ksp("io.insert-koin:koin-ksp-compiler:1.3.1")

    // Work on a paper specific library!
    api("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation(files("C:/Users/hugov/IdeaProjects/ThankmasPaper/build/libs/ThankmasPaper-1.0-SNAPSHOT.jar"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

tasks.shadowJar {
    minimize()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

tasks.withType<JavaCompile> { // Preserve parameter names in the bytecode
    options.compilerArgs.add("-parameters")
}

tasks.compileKotlin {
    kotlinOptions.javaParameters = true
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
        javaParameters = true
    }
}