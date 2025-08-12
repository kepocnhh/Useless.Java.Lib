import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sp.gx.core.GitHub
import sp.gx.core.Maven
import sp.gx.core.asFile
import sp.gx.core.assemble
import sp.gx.core.buildDir
import sp.gx.core.check
import sp.gx.core.create
import sp.gx.core.eff
import sp.gx.core.task

version = "0.4.0"

val maven = Maven.Artifact(
    group = "com.github.kepocnhh",
    id = rootProject.name,
)

val gh = GitHub.Repository(
    owner = "kepocnhh",
    name = rootProject.name,
)

repositories.mavenCentral()

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.gradle.jacoco")
}

val compileKotlinTask = tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = Version.jvmTarget
        freeCompilerArgs += setOf("-module-name", maven.moduleName())
    }
}

tasks.getByName<JavaCompile>("compileTestJava") {
    targetCompatibility = Version.jvmTarget
}

tasks.getByName<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = Version.jvmTarget
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Version.jupiter}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Version.jupiter}")
}

fun Test.getExecutionData(): File {
    return buildDir()
        .dir("jacoco")
        .asFile("$name.exec")
}

val taskUnitTest = task<Test>("checkUnitTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED") // https://github.com/gradle/gradle/issues/18647
    doLast {
        getExecutionData().eff()
    }
}

jacoco.toolVersion = Version.jacoco

val taskCoverageReport = task<JacocoReport>("assembleCoverageReport") {
    dependsOn(taskUnitTest)
    reports {
        csv.required = false
        html.required = true
        xml.required = false
    }
    sourceDirectories.setFrom(file("src/main/kotlin"))
    classDirectories.setFrom(sourceSets.main.get().output.classesDirs)
    executionData(taskUnitTest.getExecutionData())
    doLast {
        val report = buildDir()
            .dir("reports/jacoco/$name/html")
            .eff("index.html")
        println("Coverage report: ${report.absolutePath}")
    }
}

"unstable".also { variant ->
    val version = "${version}u-SNAPSHOT"
    tasks.create("check", variant, "Readme") {
        doLast {
            val expected = setOf(
                "GitHub [$version]", // todo GitHub release
//                Markdown.link("Maven", Maven.Snapshot.url(maven, version)), // todo maven url
                "maven(\"https://central.sonatype.com/repository/maven-snapshots\")", // todo maven import
                "implementation(\"${maven.moduleName(version)}\")",
            )
            rootDir.resolve("README.md").check(
                expected = expected,
                report = buildDir()
                    .dir("reports/analysis/readme")
                    .asFile("index.html"),
            )
        }
    }
    tasks.create("assemble", variant, "MavenMetadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("maven-metadata.yml")
                .assemble(
                    """
                        repository:
                         groupId: '${maven.group}'
                         artifactId: '${maven.id}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
    task<Jar>("assemble", variant, "Jar") {
        dependsOn(compileKotlinTask)
        archiveBaseName = maven.id
        archiveVersion = version
        from(compileKotlinTask.destinationDirectory.asFileTree)
    }
    task<Jar>("assemble", variant, "Source") {
        archiveBaseName = maven.id
        archiveVersion = version
        archiveClassifier = "sources"
        from(sourceSets.main.get().allSource)
    }
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val file = buildDir()
                .dir("libs")
                .file("${maven.name(version)}.pom")
                .assemble(
                    maven.pom(
                        version = version,
                        packaging = "jar",
                    ),
                )
            println("POM: ${file.absolutePath}")
        }
    }
    tasks.create("assemble", variant, "Metadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("metadata.yml")
                .assemble(
                    """
                        repository:
                         owner: '${gh.owner}'
                         name: '${gh.name}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
}

"snapshot".also { variant ->
    val version = "$version-SNAPSHOT"
    tasks.create("assemble", variant, "Metadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("metadata.yml")
                .assemble(
                    """
                        repository:
                         owner: '${gh.owner}'
                         name: '${gh.name}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
    tasks.create("assemble", variant, "MavenMetadata") {
        doLast {
            val file = buildDir()
                .dir("yml")
                .file("maven-metadata.yml")
                .assemble(
                    """
                        repository:
                         groupId: '${maven.group}'
                         artifactId: '${maven.id}'
                        version: '$version'
                    """.trimIndent(),
                )
            println("Metadata: ${file.absolutePath}")
        }
    }
    task<Jar>("assemble", variant, "Jar") {
        dependsOn(compileKotlinTask)
        archiveBaseName = maven.id
        archiveVersion = version
        from(compileKotlinTask.destinationDirectory.asFileTree)
    }
    task<Jar>("assemble", variant, "Source") {
        archiveBaseName = maven.id
        archiveVersion = version
        archiveClassifier = "sources"
        from(sourceSets.main.get().allSource)
    }
    tasks.create("assemble", variant, "Pom") {
        doLast {
            val file = buildDir()
                .dir("libs")
                .file("${maven.name(version)}.pom")
                .assemble(
                    maven.pom(
                        version = version,
                        packaging = "jar",
                    ),
                )
            println("POM: ${file.absolutePath}")
        }
    }
}
