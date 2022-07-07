import com.github.hmiyado.BuildConfigGenerator
import com.github.hmiyado.Dependencies
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.7.10"
    application
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("org.openapi.generator") version "6.0.0"
}

group = "kottage"
version = "v1-202206181454"

val generatedSourcePath = buildDir.resolve(File("generated/src/main/kotlin"))
tasks.register("generateBuildConfig") {
    outputs.dir(generatedSourcePath)
    doLast {
        BuildConfigGenerator.generate(destination = generatedSourcePath, version = version.toString())
    }
}

application {
    if (System.getProperties().getProperty("cli") != null) {
        mainClass.set("com.github.hmiyado.kottage.cli.CliEntrypoint")
    } else {
        mainClass.set("io.ktor.server.netty.EngineMain")
    }
}

// https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-gradle-plugin
// https://github.com/OpenAPITools/openapi-generator/blob/master/docs/generators/kotlin-server.md
openApiGenerate {
    generatorName.set("kotlin-server")
    inputSpec.set("$rootDir/src/main/resources/api-spec/root.json")
    outputDir.set("$buildDir/generated")
    templateDir.set("$rootDir/src/main/resources/template")
    ignoreFileOverride.set("$rootDir/.openapi-generator-ignore")
    val rootPackage = "com.github.hmiyado.kottage.openapi"
    packageName.set(rootPackage)
    library.set("ktor")
    typeMappings.set(
        mutableMapOf(
            "DateTime" to "java.time.ZonedDateTime"
        )
    )
    configOptions.put("enumPropertyNaming", "PascalCase")
    verbose.set(false)
}

sourceSets {
    main {
        java.srcDir(generatedSourcePath)
    }
}

repositories {
    mavenCentral()
}

val generateBuildConfig by tasks.getting(Task::class)
val openApiGenerate by tasks.getting(Task::class)
val compileKotlin by tasks.getting(KotlinCompile::class) {
    kotlinOptions {
        jvmTarget = Dependencies.Kotlin.jvmTarget
    }
    dependsOn(generateBuildConfig, openApiGenerate)
}
val compileTestKotlin = tasks.getByName("compileTestKotlin") {
    if (this is KotlinCompile) {
        kotlinOptions {
            jvmTarget = Dependencies.Kotlin.jvmTarget
        }
    }
    dependsOn(generateBuildConfig, openApiGenerate)
}
val test by tasks.getting(Test::class) {
    useJUnitPlatform()
    jvmArgs = listOf("-Dio.ktor.development=false")
    testLogging {
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events = setOf(TestLogEvent.SKIPPED, TestLogEvent.PASSED, TestLogEvent.FAILED)
    }
}

dependencies {
    Dependencies.Kottage.implementations.forEach {
        implementation(it)
    }
    Dependencies.Kottage.testImplementations.forEach {
        implementation(it)
    }
}
