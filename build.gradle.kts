plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("com.github.johnrengelman.shadow")
    id("io.papermc.paperweight.userdev")
}

group = "me.hugo.thankmaslobby"
version = "1.0-SNAPSHOT"

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    compileOnly(libs.luck.perms)

    // Citizens API
    compileOnly(libs.citizens) {
        exclude(mutableMapOf("group" to "*", "module" to "*"))
    }

    ksp(libs.koin.ksp.compiler)

    // Work on a paper specific library!
    implementation(project(":common-paper"))

    implementation(libs.bundles.exposed.runtime)
    api(libs.exposed.jbdc)
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