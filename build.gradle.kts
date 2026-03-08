import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.1.10"
  id("org.jetbrains.intellij.platform") version "2.12.0"
}

group = "com.github.moct10"
version = "0.2.27"

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
    intellijIdea("2025.1")
    plugin("IdeaVIM:2.27.2")
  }
}

intellijPlatform {
  pluginConfiguration {
    ideaVersion {
      // IdeaVIM 2.27.2 requires 251.23774+
      sinceBuild = "251.23774"
    }
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}
