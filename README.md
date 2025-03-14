# bw-util-network [![Build Status](https://travis-ci.org/Bedework/bw-util-network.svg)](https://travis-ci.org/Bedework/bw-util-network)

Network (http, DAV,servlet etc) related utilities

This project provides a number of network (http, DAV,servlet etc) related classes and methods for
[Bedework](https://www.apereo.org/projects/bedework).

### Requirements

1. JDK 11
2. Maven 3

### Building Locally

> mvn clean install

### Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release:

> mvn -P bedework-dev release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn -P bedework-dev release:perform

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

### Release Notes
#### 4.1.0
* Split off from bw-util
* Dav: Add getExtMkcolResponse method
* Dav: Fix parsing of multi-status. Was not handling no propstat correctly
* Http: Add proppatch method
* Http: Parameterize ProcessResponse interface

#### 4.1.1
 * Update library versions.
 * Remove a bunch of unnecessary throws clauses

#### 4.1.2
 * Update library versions.

#### 4.1.3
 * Update library versions.

#### 4.1.4
 * Update library versions.

#### 5.0.0
* Update library versions.
* Use bedework-parent

#### 5.0.1
* Update library versions.
* HttpServletUtils: Use the principal name if REMOTE_USER is not set. Keycloak shib code needs this.

#### 5.0.2
* Update library versions.
* ConfiguredXSLTFilter: Use name defined in PresentationState

#### 5.1.0
* Update library versions.
* Fix up a bunch of introduced config errors
* Add support for the Saxon XSLT support
* Allow "on" in ReqPar
* HttpServletUtils: Implement stripping of domain from principal

#### 5.1.1
* Update library versions.

#### 5.1.2
* Update library versions.

#### 5.1.3
* Update util-conf version.

#### 5.1.4
* Sonatype failure- redo release.

#### 5.1.5
* Further changes to remove dependencies on form objects.

#### 5.1.6
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
