load("//tools:utils.bzl", "index_artifacts")

_DAGGER_SNIPPET = """
java_library(
    name = "dagger",
    exports = [
        ":dagger-api",
        "@maven//javax/inject:javax_inject",
    ],
    exported_plugins = [":plugin"],
    visibility = ["//visibility:public"],
)

raw_jvm_import(
    name = "dagger-api",
    jar = "@com_google_dagger_dagger//maven",
    visibility = ["//visibility:public"],
    deps = [
        "@maven//javax/inject:javax_inject",
    ],
)

java_plugin(
    name = "plugin",
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    generates_api = True,
    deps = [":dagger-compiler"],
)
"""

_AUTO_VALUE_SNIPPET = """
java_library(
    name = "value",
    exports = [
        ":auto-value-annotations",
    ],
    exported_plugins = [":plugin"],
    visibility = ["//visibility:public"],
)

raw_jvm_import(
    name = "auto-value",
    jar = "@com_google_auto_value_auto_value//maven",
    visibility = ["@maven//com/ryanharter/auto/value:__subpackages__"],
)

java_plugin(
    name = "plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    generates_api = True,
    deps = [
      ":auto-value",
      "@maven//com/google/auto:auto-common",
    ],
)
"""

_JSR305_SNIPPET = """
raw_jvm_import(
    name = "jsr305",
    jar = "@com_google_code_findbugs_jsr305//maven",
    visibility = ["//visibility:public"],
    neverlink = True,
)
"""

MAVEN_ARTIFACTS = {
    "com.google.auto:auto-common:0.11": {"insecure": True},
    "com.google.auto.value:auto-value-annotations:1.6.3": {"insecure": True},
    "com.google.auto.value:auto-value:1.6.3": {"insecure": True, "build_snippet": _AUTO_VALUE_SNIPPET},
    "com.google.code.findbugs:jsr305:1.3.9": {"insecure": True, "build_snippet": _JSR305_SNIPPET},
    "com.google.code.gson:gson:2.8.6": {"insecure": True},
    "com.google.code.java-allocation-instrumenter:java-allocation-instrumenter:3.3.0": {"insecure": True},
    "com.google.dagger:dagger:2.29.1": {"insecure": True, "build_snippet": _DAGGER_SNIPPET},
    "com.google.dagger:dagger-compiler:2.29.1": {"insecure": True},
    "com.google.dagger:dagger-producers:2.29.1": {"insecure": True},
    "com.google.dagger:dagger-spi:2.29.1": {"insecure": True},
    "com.google.errorprone:error_prone_annotations:2.3.2": {"insecure": True},
    "com.google.errorprone:javac-shaded:9-dev-r4023-3": {"insecure": True},
    "com.google.guava:guava:29.0-jre": {"insecure": True},
    "com.google.guava:failureaccess:1.0.1": {"insecure": True},
    "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava": {"insecure": True},
    "com.google.googlejavaformat:google-java-format:1.5": {"insecure": True},
    "com.google.j2objc:j2objc-annotations:1.3": {"insecure": True},
    "com.google.truth:truth:1.0": {"insecure": True},
    "com.googlecode.java-diff-utils:diffutils:1.3.0": {"insecure": True},
    "com.squareup:javapoet:1.11.1": {"insecure": True},
    "com.squareup.okhttp:okhttp:2.5.0": {"insecure": True},
    "com.squareup.okio:okio:1.6.0": {"insecure": True},
    "com.sun.jersey:jersey-client:1.11": {"insecure": True},
    "com.sun.jersey:jersey-core:1.11": {"insecure": True},
    "javax.annotation:javax.annotation-api:1.3.2": {"insecure": True},
    "javax.annotation:jsr250-api:1.0": {"insecure": True},
    "javax.inject:javax.inject:1": {"insecure": True},
    "joda-time:joda-time:2.10.6": {"insecure": True},
    "junit:junit:4.13": {"insecure": True},
    "net.ltgt.gradle.incap:incap:0.2": {"insecure": True},
    "org.checkerframework:checker-compat-qual:2.5.5": {"insecure": True},
    "org.checkerframework:checker-qual:2.8.1": {"insecure": True},
    "org.codehaus.mojo:animal-sniffer-annotations:1.18": {"insecure": True},
    "org.hamcrest:hamcrest-core:1.3": {"insecure": True},
    "org.jetbrains:annotations:13.0": {"insecure": True},
    "org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0": {"insecure": True},
    "org.jetbrains.kotlin:kotlin-stdlib:1.3.72": {"insecure": True},
    "org.jetbrains.kotlin:kotlin-stdlib-common:1.3.72": {"insecure": True},
    "org.mockito:mockito-all:1.9.5": {"insecure": True},
    "org.ow2.asm:asm-analysis:7.2": {"insecure": True},
    "org.ow2.asm:asm-commons:7.2": {"insecure": True},
    "org.ow2.asm:asm-tree:7.2": {"insecure": True},
    "org.ow2.asm:asm-util:7.2": {"insecure": True},
    "org.ow2.asm:asm:7.2": {"insecure": True},
    "org.pantsbuild:jarjar:1.7.2": {
        "insecure": True,
        "exclude": ["org.apache.ant:ant", "org.apache.maven:maven-plugin-api"],
    },
}



ARTIFACT_VERSIONS = index_artifacts(MAVEN_ARTIFACTS)
