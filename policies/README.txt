YAGO: Infrastructure
Author: Fabian M. Suchanek

This document describes the technical infrastructure of YAGO,
meaning how it can be run, what files serve which purpose, and
what tools are available.

--------- Structure of the Project

The following Java projects belong to YAGO
* Javatools
  These classes are Java utilities. They are shared with other MPI projects.
* Basics
  These classes are used to represent facts, TSV files, etc.
  The files in "data" describe the schema of YAGO.
  This project can be shared with others or be externalized.
* YAGO
  This project contains all main YAGO extractors, together with
  - their data (patterns etc.)
  - policies that describe the code, architecture, and data format 
  - scripts that run YAGO (s.b.)
  - test cases (see policies)
