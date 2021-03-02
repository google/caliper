workspace(name = "caliper")

load("//:maven_artifacts.bzl", "MAVEN_ARTIFACTS")
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

maven_repository_specification(
    name = "maven",
    artifacts = MAVEN_ARTIFACTS,
    dependency_target_substitutes = {
        "com.google.dagger": {"@maven//com/google/dagger:dagger": "@maven//com/google/dagger:dagger-api"},
    },
)

android_sdk_repository(name = "androidsdk")
