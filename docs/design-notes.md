# HelpFlow — Design Notes

## Assumptions

- **A-01 (Phase 0):** The machine has Maven 3.8.7 installed, but the steering requires 3.9+.
  Decision: add the Maven Wrapper (`mvnw`) to both Maven projects in Phase 1, pinned to
  Maven **3.9.13** (latest stable 3.9 patch as of October 2026). All build commands in
  docs, README and Docker will use `./mvnw` instead of `mvn`. The installed 3.8.7 is only
  used to bootstrap the wrapper once during Phase 1 setup.

## Architecture decisions

_Populated as phases progress._
