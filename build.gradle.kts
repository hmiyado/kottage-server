import com.github.hmiyado.BuildConfigGenerator
import com.github.hmiyado.Dependencies
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.6.21"
    application
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("org.openapi.generator") version "5.4.0"
}

group = "kottage"
version = "v1-202204061458"

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
    implementation(Dependencies.Ktor.serverNetty)
    implementation(Dependencies.Ktor.serialization)
    implementation(Dependencies.Ktor.auth)
    implementation(Dependencies.Ktor.sessions)
    implementation(Dependencies.KtorClient.core)
    implementation(Dependencies.KtorClient.cio)
    implementation(Dependencies.Logback.classic)
    implementation(Dependencies.Logback.janino)
    implementation(Dependencies.Exposed.core)
    implementation(Dependencies.Exposed.dao)
    implementation(Dependencies.Exposed.jdbc)
    implementation(Dependencies.Exposed.javaTime)
    implementation(Dependencies.PostgreSql.core)
    implementation(Dependencies.MySql.core)
    implementation(Dependencies.Flyway.core)
    implementation(Dependencies.Redis.jedis)
    implementation(Dependencies.Koin.ktor)
    implementation(Dependencies.Miyado.csrfProtection)
    testImplementation(Dependencies.JUnit.jupiter)
    testImplementation(Dependencies.Kotest.jUnit5)
    testImplementation(Dependencies.Kotest.json)
    testImplementation(Dependencies.Kotest.dataTest)
    testImplementation(Dependencies.Kotest.koin)
    testImplementation(Dependencies.Kotest.ktor)
    testImplementation(Dependencies.Ktor.test)
    testImplementation(Dependencies.KtorClient.mock)
    testImplementation(Dependencies.Koin.test)
    testImplementation(Dependencies.Mockk.core)
}
