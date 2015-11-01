Spanner
=====

Spanner is a micro benchmarking framework designed to run on the Android platform.

It is a fork of the original Caliper project for Java started by Google: code.google.com/p/caliper

# Getting started

## Download

Stable releases of Spanner is available on [JCenter](https://bintray.com/cmelchior/maven/spanner/view).

```
dependencies {
  compile 'dk.ilios:spanner:0.2.0'
}
```

The current state of master is available as a SNAPSHOT on [JFrog](http://oss.jfrog.org/oss-snapshot-local/dk/ilios/spanner/).

```
repositories {
    maven {
        url 'http://oss.jfrog.org/artifactory/oss-snapshot-local'
    }
}

dependencies {
  compile 'dk.ilios:spanner:0.2.1-SNAPSHOT'
}
```

## Benchmarks as unit tests

Spanner provides a custom JUnit4 runner that makes it possible to run benchmarks
as part of the JUnit framework. To use this feature you need to add the 
following dependencies manually:


```
androidTestCompile 'com.android.support:support-annotations:23.0.1'
androidTestCompile 'com.android.support.test:runner:0.4.1'
androidTestCompile 'com.android.support.test:rules:0.4.1'
androidTestCompile 'junit:junit:4.12'
```

## Online results

Spanner benchmarks results are compatible with the output from Caliper and can
therefore be uploaded to [https://microbenchmarks.appspot.com/](https://microbenchmarks.appspot.com/)
as well.

In order to upload benchmark results to the website you need the following
permission in AndroidManifest.xml:

```
<uses-permission android:name="android.permission.INTERNET" />
```

## Creating a benchmark

* See an example of a standalone benchmark [here](https://github.com/cmelchior/spanner/blob/master/sample/src/main/java/dk/ilios/spanner/example/ActivityBenchmarks.java).
* See an example of a JUnit benchmark [here](https://github.com/cmelchior/spanner/blob/master/sample/src/androidTest/java/dk/ilios/spanner/UnitTestBenchmarks.java).

## Benchmark results (TODO)

The output from a benchmark will be posted in 3 places:
- LogCat
- Json file in `context.getExternalFilesDir()` 
- Uploaded to Caliper website (if enabled)

## Differences from Caliper (TODO)

* Able to compare against a baseline
* Support for running in the JUnit framework
* Running on the same thread starting the benchmark
* Removed support for VM parameters 

# Benchmarking

## Why should I benchmark? (TODO)

* https://github.com/google/caliper/wiki/JavaMicrobenchmarks


## Benchmarking with Spanner

Each invocation of Spanner is called a *Run*. Each run consists of 1 benchmark 
class and one or more methods.

A run has different *axis'*, e.g. method to run and parameters to use.

A *Scenario* is a unique combination of these axis'.

An *Instrument* determines what is measured in any given scenario. Most commonly 
runtime is measured, but you could also measure memory usage or some arbitrary 
value.

The combination of a *Scenario* and an *Instrument* is called an *Experiment*. 
An experiment is thus a full description of what test(s) to run and how to 
measure it.

Running an experiment is called a *Trial*.

Each trial consists of one or more *Measurements*. 

In an ideal world it should be enough to run one trial with one measurement as 
it would always produce reliable, reproducible results. However, this is not the 
case as we are running inside a virtual machine and do not have full 
control over the operating system. For that reason we normally conduct multiple 
measurement in each trial in order to smooth our irregularities and gain 
confidence in our results. 

Each trial will output statistics about the measurements like min, max and mean. 

The output from a Spanner benchmark is the results of all the experiments.


## Creating a measurement (TODO)

* Detect number of repetitions needed to exceed threshold
* Warmup


## Benchmarking pitfalls

### JIT / AOT (TODO)

Dalvik uses Just-in-time compilation.
ART uses Ahead-of-time compilation.

Just-in-time compilers will analyze the code while it runs and optimize it, for 
this reason it is important to do warmup in these kind of  environments.

Ahead-of-time compilers do not modify the code while it is running, as such no
warmup should be needed
running on ART 

* Running tests in a different process
* Warmup
* JIT: Code being converted to native code
* ART will intoduce JIT in the future.


### Measuring time (TODO)

* Clock drift (System.nanoTime() / System.currentTimeMillis())
* Clock granularity, make sure to test it.
* Make sure that test runs longer than granularity
* Use appropriate system calls for measuring time.


### Benchmark variance (TODO)

* Garbage collector
* Many layers between Java code and CPU instructions
* Kernel controls scheduler
* CPU behaves differently under different loads

* Enable fly-mode
* Disable as many sensors as possible
* Remove as many apps as possible
* Minimize GC


### Benchmark overhead (TODO)

* Method calls
* Iterators
* Getting a timestamp


### Compiler optimizations (TODO)

* Compiler can reorder/remove code.
* Compile to native code.

### Interpreting results (TODO)

* Be mindful of measured overhead.
* Results do not say anything about the absolute speed.


### Math (TODO)

* What is the confidence interval. 
* What is variance, how to interpret it.


## FAQ

**Why spanner?**

Because a Spanner is a much more useful tool than a Caliper when working with 
Androids.


## Resources

- http://jeremymanson.blogspot.dk/2009/12/allocation-instrumenter-for-java_10.html