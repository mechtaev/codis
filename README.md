# CODIS

CODIS is an inductive program synthesizer.

## Installation

1. Compile Z3 with Java support.
2. Install `com.microsoft.z3` to Maven:

              mvn install:install-file -Dfile=/path/to/com.microsift.z3.jar \
                                       -DgroupId=com.microsoft.z3 \
                                       -DartifactId=z3 \
                                       -Dversion=4 \
                                       -Dpackaging=jar
3. Execute `mvn package`.
