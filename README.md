# CODIS

CODIS is an inductive program synthesizer.

## Installation

1. Compile MathSAT with Java support.
2. Install `mathsat.api` to Maven:

        mvn install:install-file -Dfile=/path/to/mathsat.jar \
                                 -DgroupId=mathsat.api \
                                 -DartifactId=mathsat \
                                 -Dversion=5 \
                                 -Dpackaging=jar
3. Execute `mvn package`.
