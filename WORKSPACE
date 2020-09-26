workspace(name = "caliper")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

MAVEN_REPOSITORY_RULES_VERSION = "2.0.0-alpha-4"

MAVEN_REPOSITORY_RULES_SHA = "a6484fec8d1aebd4affff7ae1ee9b59141858b2c636222bdb619526ccd8b3358"

http_archive(
    name = "maven_repository_rules",
    sha256 = MAVEN_REPOSITORY_RULES_SHA,
    strip_prefix = "bazel_maven_repository-%s" % MAVEN_REPOSITORY_RULES_VERSION,
    type = "zip",
    urls = ["https://github.com/square/bazel_maven_repository/archive/%s.zip" % MAVEN_REPOSITORY_RULES_VERSION],
)
# Setup maven repository handling.
load("@maven_repository_rules//maven:maven.bzl", "maven_repository_specification")

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


_AUTO_VALUE_SNIPPET =  """
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

artifacts = {
    "com.google.auto:auto-common:0.11": {"insecure": True},
    "com.google.auto.value:auto-value-annotations:1.6.3": {"insecure": True},
    "com.google.auto.value:auto-value:1.6.3": {"insecure": True, "build_snippet": _AUTO_VALUE_SNIPPET},
    "com.google.code.findbugs:jsr305:1.3.9": {"insecure": True},
    "com.google.code.gson:gson:2.2.2": {"insecure": True},
    "com.google.code.java-allocation-instrumenter:java-allocation-instrumenter:3.3.0": {"insecure": True},
    "com.google.dagger:dagger:2.22.1": {"insecure": True, "build_snippet": _DAGGER_SNIPPET},
    "com.google.dagger:dagger-compiler:2.22.1": {"insecure": True},
    "com.google.dagger:dagger-producers:2.22.1": {"insecure": True},
    "com.google.dagger:dagger-spi:2.22.1": {"insecure": True},
    "com.google.errorprone:error_prone_annotations:2.3.2": {"insecure": True},
    "com.google.errorprone:javac-shaded:9-dev-r4023-3": {"insecure": True},
    "com.google.guava:failureaccess:1.0.1": {"insecure": True},
    "com.google.guava:guava:28.1-jre": {"insecure": True},
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
    "joda-time:joda-time:2.1": {"insecure": True},
    "junit:junit:4.12": {"insecure": True},
    "org.checkerframework:checker-compat-qual:2.5.5": {"insecure": True},
    "org.checkerframework:checker-qual:2.8.1": {"insecure": True},
    "org.codehaus.mojo:animal-sniffer-annotations:1.18": {"insecure": True},
    "org.hamcrest:hamcrest-core:1.3": {"insecure": True},
    "org.mockito:mockito-all:1.9.5": {"insecure": True},
    "org.ow2.asm:asm-analysis:7.2": {"insecure": True},
    "org.ow2.asm:asm-commons:7.2": {"insecure": True},
    "org.ow2.asm:asm-tree:7.2": {"insecure": True},
    "org.ow2.asm:asm-util:7.2": {"insecure": True},
    "org.ow2.asm:asm:7.2": {"insecure": True},
}


maven_repository_specification(
    name = "maven",
    artifacts = artifacts,
    dependency_target_substitutes = {
        "com.google.dagger": {"@maven//com/google/dagger:dagger": "@maven//com/google/dagger:dagger-api"},
    },
)
