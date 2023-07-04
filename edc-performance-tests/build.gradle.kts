plugins {
    `java-library`
    id("io.gatling.gradle").version("3.9.5")
}



dependencies {
    gatlingImplementation(libs.edc.util)
    gatlingImplementation(libs.edc.api.management)
//    gatlingImplementation(libs.jackson.databind)
}

// do not publish
edcBuild {
    publish.set(false)
}

