# ⚡ Hydrosphere Mist (Fork)

[![Scala 2.13](https://img.shields.io/badge/Scala-2.13-red.svg)](https://scala-lang.org/)
[![Apache Spark 4](https://img.shields.io/badge/Spark-4.x-blue.svg)](https://spark.apache.org/)

## 📌 This Fork

This is a community-maintained fork of [Hydrosphere Mist](https://github.com/Hydrospheredata/mist) adapted to support **Scala 2.13.16** and ensure compatibility with **Apache Spark 4**.

### Why this fork?

The original Hydrosphere Mist is built with Scala 2.12 / 2.11 and does not work out-of-the-box with Apache Spark 4, which requires Scala 2.13.

### Changes in this fork

- ✅ **Scala 2.13.16** – Upgraded from earlier Scala versions
- ✅ **Apache Spark 4 compatibility** – Modified dependencies and build configuration to work with Spark 4.x
- ✅ Builds successfully with Scala 2.13.16

See the commits ahead of `Hydrospheredata/mist:master` for changes in detail.

### ⚠️ Important Note: H2 Database Upgrade

This fork includes an upgrade to a newer version of the H2 database. Due to changes in H2's storage format, **existing `.db` files from previous Mist versions are incompatible and must be removed** before starting the newly built Mist.

**Before running the new version:**

```bash
  rm -f /path/to/mist/recovery.*.db   
  # Remove old H2 database files or backup and manually migrate after new .db files created
```

### Build Instructions

```bash
  sbt ++2.13.16 clean assembly
```
---  


[![Build Status](https://ci.hydrosphere.io/buildStatus/icon?job=hydrosphere.io/mist/master)](https://ci.hydrosphere.io/job/hydrosphere.io/job/mist/job/master/)
[![Build Status](https://travis-ci.org/Hydrospheredata/mist.svg?branch=master)](https://travis-ci.org/Hydrospheredata)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.hydrosphere/mist-lib_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.hydrosphere/mist-lib_2.11/)
[![Docker Hub Pulls](https://img.shields.io/docker/pulls/hydrosphere/mist.svg)](https://img.shields.io/docker/pulls/hydrosphere/mist.svg)
# Hydrosphere Mist

[![Join the chat at https://gitter.im/Hydrospheredata/mist](https://badges.gitter.im/Hydrospheredata/mist.svg)](https://gitter.im/Hydrospheredata/mist?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[Hydrosphere](http://hydrosphere.io) Mist is a serverless proxy for Spark cluster.
Mist provides a new functional programming framework and deployment model for Spark applications. 

Please see our [quick start guide](https://hydrosphere.io/mist-docs/quick_start.html) and [documentation](https://hydrosphere.io/mist-docs/)

Features:
* **Spark Function as a Service**. Deploy Spark functions rather than notebooks or scripts.
* Spark Cluster and Session management. Fully managed Spark sessions backed by on-demand EMR, Hortonworks, Cloudera, DC/OS and vanilla Spark clusters.
* **Typesafe** programming framework that clearly defines inputs and outputs of every Spark job.
* **REST** HTTP & Messaging (MQTT, Kafka) API for Scala & Python Spark jobs.
* Multi-cluster mode: Seamless Spark cluster on-demand provisioning, autoscaling and termination(**pending**)
![Cluster of Spark Clusters](http://dv9c7babquml0.cloudfront.net/docs-images/mist-cluster-of-spark-clusters.gif)

It creates a unified API layer for building enterprise solutions and microservices on top of a Spark functions.

![Mist use cases](http://dv9c7babquml0.cloudfront.net/docs-images/mist-use-case.png)

## High Level Architecture

![High Level Architecture](http://dv9c7babquml0.cloudfront.net/docs-images/mist-highlevel-architecture.png)

## Contact

Please report bugs/problems to: 
<https://github.com/Hydrospheredata/mist/issues>.

<http://hydrosphere.io/>

[LinkedIn](https://www.linkedin.com/company/hydrospherebigdata)

[Facebook](https://www.facebook.com/hydrosphere.io/)

[Twitter](https://twitter.com/hydrospheredata)
