# simplemes-core

[![Build Status](https://github.com/simplemes/simplemes-core/workflows/BuildAndTestAll/badge.svg)](https://github.com/simplemes/simplemes-core/actions)


**This framework is in a pre-release state.**


SimpleMES (Manufacturing Execution System) Core Modules.  The supported modules include:

* [Enterprise Framework Module](eframe)
* [Webix Assets](webix)

# Build Instructions

Some common build options for the entire project includes:

* **./gradlew buildAll** - Build and test for all sub-projects (modules). 
* **./gradlew testAll** -  Run non-GUI tests for all sub-projects (modules).
* **./gradlew assembleAll** - Assemble .jar files for all sub-projects (modules).
* **./gradlew asciidoctorAll** - Build HTML docs for all sub-projects (modules).
* **./gradlew asciidoctorAll** **-PbackendProp=pdf** - Build PDF docs for all sub-projects (modules).
* **./gradlew cleanAll** - Cleans output directories for all sub-projects (modules).

Each module has similar actions (e.g. build, test. etc).

