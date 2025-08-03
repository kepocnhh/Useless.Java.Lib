import sp.gx.core.asFile
import sp.gx.core.buildDir
import sp.gx.core.buildSrc
import sp.gx.core.check

buildscript {
    repositories.mavenCentral()

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlin}")
    }
}

task<Delete>("clean") {
    delete = setOf(buildDir(), buildSrc.buildDir())
}

task("checkLicense") {
    doLast {
        val author = "Stanley Wintergreen" // todo
        file("LICENSE").check(
            expected = emptySet(),
            regexes = setOf("^Copyright 2\\d{3} $author$".toRegex()),
            report = buildDir()
                .dir("reports/analysis/license")
                .asFile("index.html"),
        )
    }
}
