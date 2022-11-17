prusa-i3-mk3s-profiles
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.prusa-i3-mk3s-profiles/com.io7m.prusa-i3-mk3s-profiles.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.prusa-i3-mk3s-profiles%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/com.io7m.prusa-i3-mk3s-profiles/com.io7m.prusa-i3-mk3s-profiles.svg?style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/prusa-i3-mk3s-profiles/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m/prusa-i3-mk3s-profiles.svg?style=flat-square)](https://codecov.io/gh/io7m/prusa-i3-mk3s-profiles)

![prusa-i3-mk3s-profiles](./src/site/resources/prusa-i3-mk3s-profiles.jpg?raw=true)

| JVM             | Platform | Status |
|-----------------|----------|--------|
| OpenJDK LTS     | Linux    | [![Build (OpenJDK LTS, Linux)](https://img.shields.io/github/workflow/status/io7m/prusa-i3-mk3s-profiles/main-openjdk_lts-linux)](https://github.com/io7m/prusa-i3-mk3s-profiles/actions?query=workflow%3Amain-openjdk_lts-linux) |
| OpenJDK Current | Linux    | [![Build (OpenJDK Current, Linux)](https://img.shields.io/github/workflow/status/io7m/prusa-i3-mk3s-profiles/main-openjdk_current-linux)](https://github.com/io7m/prusa-i3-mk3s-profiles/actions?query=workflow%3Amain-openjdk_current-linux)
| OpenJDK Current | Windows  | [![Build (OpenJDK Current, Windows)](https://img.shields.io/github/workflow/status/io7m/prusa-i3-mk3s-profiles/main-openjdk_current-windows)](https://github.com/io7m/prusa-i3-mk3s-profiles/actions?query=workflow%3Amain-openjdk_current-windows)

## Building

```
$ mvn package && ./export.sh
```

This will compile all of the profiles to `target/profiles`. The profiles
can then be copied to the `.PrusaSlicer` configuration directory on your
system.

