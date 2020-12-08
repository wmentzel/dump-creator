plugins {
    application
    kotlin("jvm") version "1.4.20"
    id("com.diffplug.gradle.spotless") version "4.3.1"
}

allprojects {
    apply(plugin = "com.diffplug.gradle.spotless")

    spotless {
        kotlin {
            ktlint("0.37.2")
        }
        kotlinGradle {
            ktlint("0.37.2")
        }
    }
}

application {
    mainClassName = "com.randomlychosenbytes.openfoodfactsdumper.MainKt"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.apache.commons:commons-csv:1.3")

    testImplementation("junit:junit:4.12}")
}

repositories {
    jcenter()
}

tasks {
    "wrapper"(Wrapper::class) {
        gradleVersion = "6.3"
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
