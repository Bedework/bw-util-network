# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.1.0-SNAPSHOT)

## [6.0.0] - 2025-06-24
* First jakarta release

## [5.1.6] - 2025-05-25
* Make servlet class more generally useful. Use Util.fixPath.
* Add features to help build web apps
* Add new admin application. Implement (partially) 1 of the actions.
  Add some changes to the util classes to facilitate
* Fix ServletBase so it doesn't always invalidate session.
  Fix the jsp likewise.
* Refactor framework to enhance method handling.
* Move configuration for servlet into a json file in /classes. Setup initial configuration for eventreg ws and admin.
* Implement forwarding by name. Rearrange configs with explicit ajax error forwards and also one POST method.
* Centralize checking of request parameters to simplify code.
* Added a parent to AppInfo so we can climb up to find defaults
* Implement redirect and use for some actions.
  Preserve the calsuite and formName in the globals across actions.
* Refactor session serialization logic into SessionSerializer.
* Add parameter to signify appInfo is being used
* Move response classes and ToString into bw-base module.

## [5.1.5] - 2024-07-25
* Further changes to remove dependencies on form objects.

## [5.1.4] - 2024-03-28
* Sonatype failure- redo release.

## [5.1.3] - 2023-12-05
* Update util-conf version.

## [5.1.2] - 2023-12-05
* Update library versions.

## [5.1.1] - 2023-12-05
* Update library versions.

## [5.1.0] - 2023-12-04
* Update library versions.
* ConfiguredXSLTFilter: Use name defined in PresentationState
* Fix up a bunch of introduced config errors
* Add support for the Saxon XSLT support
* Allow "on" in ReqPar
* HttpServletUtils: Implement stripping of domain from principal

## [5.0.1] - 2022-03-06
* Update library versions.
* HttpServletUtils: Use the principal name if REMOTE_USER is not set. Keycloak shib code needs this.

## [5.0.0] - 2022-02-12
* Update library versions.
* Use bedework-parent

## [4.1.4] - 2021-09-11
* Update library versions.

## [4.1.3] - 2021-06-07
* Update library versions.

## [4.1.2] - 2021-05-30
* Update library versions.

## [4.1.1] - 2021-05-20
* Update library versions.
* Remove a bunch of unnecessary throws clauses

## [4.1.0] - 2020-03-20
* Split off from bw-util
* Dav: Add getExtMkcolResponse method
* Dav: Fix parsing of multi-status. Was not handling no propstat correctly
* Http: Add proppatch method
* Http: Parameterize ProcessResponse interface

