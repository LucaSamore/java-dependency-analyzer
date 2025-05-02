import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.7.21"
  application
  id("com.github.johnrengelman.shadow") version "7.1.2"
  id("com.ncorti.ktfmt.gradle") version "0.22.0"
}

group = "pcd.ass02"

version = "1.0.0-SNAPSHOT"

repositories { mavenCentral() }

val vertxVersion = "4.5.14"
val junitJupiterVersion = "5.9.1"

val mainVerticleName = "pcd.ass02.MainVerticle"
val launcherClassName = "io.vertx.core.Launcher"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application { mainClass.set(launcherClassName) }

dependencies {
  implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
  implementation("io.vertx:vertx-lang-kotlin")
  implementation(kotlin("stdlib-jdk8"))
  testImplementation("io.vertx:vertx-unit")
  testImplementation("junit:junit:4.13.2")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions.jvmTarget = "17"

tasks.withType<ShadowJar> {
  archiveClassifier.set("fat")
  manifest { attributes(mapOf("Main-Verticle" to mainVerticleName)) }
  mergeServiceFiles()
}

tasks.withType<Test> {
  useJUnit()
  testLogging { events = setOf(PASSED, SKIPPED, FAILED) }
}

tasks.withType<JavaExec> {
  args =
      listOf(
          "run",
          mainVerticleName,
          "--redeploy=$watchForChange",
          "--launcher-class=$launcherClassName",
          "--on-redeploy=$doOnChange")
}
