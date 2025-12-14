plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

dependencies {
    implementation(project(":pipeline-core"))
}

application {
    // Placeholder main class; to be updated when code is added
    mainClass.set("com.example.yoga.dataprep.MainKt")
}

kotlin {
    jvmToolchain(17)
}


