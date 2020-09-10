# simplemes-core

[![Build Status](https://github.com/simplemes/simplemes-core/workflows/BuildAndTestAll/badge.svg)](https://github.com/simplemes/simplemes-core/actions)


**This framework is in a pre-release state.**

See the [documentation](https://simplemes.github.io/simplemes-core/)

SimpleMES (Manufacturing Execution System) Core Modules.  The supported modules include:

* [Enterprise Framework Module](eframe)
* [MES Core Module](mes-core)
* [MES Assembly Module](mes-assy)
* [Webix Assets](webix)

# Design Decisions

Key design/architecture decisions are briefly documented in the various modules. 

* [Enterprise Framework Decisions](eframe/src/main/docs/adr/DesignDecisions.md)

# Build Instructions

Some common build options for the entire project includes:

* **./gradlew buildAll** - Build and test for all sub-projects (modules). 
* **./gradlew testAll** -  Run non-GUI tests for all sub-projects (modules).
* **./gradlew assembleAll** - Assemble .jar files for all sub-projects (modules).
* **./gradlew asciidoctorAll** - Build HTML docs for all sub-projects (modules).
* **./gradlew groovydocAll** - Build Groovy/Javadoc for all sub-projects (modules).
* **./gradlew cleanAll** - Cleans output directories for all sub-projects (modules).

Each module has similar actions (e.g. build, test. etc).  
This project currently supports OpenJDK 14.0.

