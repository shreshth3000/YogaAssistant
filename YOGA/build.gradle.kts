buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("com.chaquo.python:gradle:13.0.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        google()
    }
}
