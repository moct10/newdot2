import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.1.10"
  id("org.jetbrains.intellij.platform") version "2.12.0"
}

group = "com.github.moct10"
version = "0.2.35"

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  intellijPlatform {
    intellijIdea("2025.2")
    plugin("IdeaVIM:2.27.2")
  }
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      sinceBuild = "252"
    }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}
