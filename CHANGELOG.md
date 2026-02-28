# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.2.0-SNAPSHOT)
## [6.1.0] - 2026-02-25
These changes disable the csp interceptor in struts2. 

We got flooded with browser console messages like this:
`Content-Security-Policy: (Report-Only policy) The page’s settings would block an event handler (script-src-attr) from being executed because it violates the following directive: “script-src 'nonce-bRLapbkO9tRMMygLridmc-n_' 'strict-dynamic' http: https:”. Consider using a hash ('sha256-6rROJosEzSXSQC+W4ivzwlHVxSEwISYX6qSw94QI7mk=') together with 'unsafe-hashes'.
Source: updateUrlDisplay()`

Struts2 isn't the correct place to be doing this - it probably needs to be undertow. Also the configuration is wrong. 

### Added
- struts2 module providing basic support.

### Added
- jsp module providing basic tag support

## [6.0.0] - 2025-06-24
### Changed
- First jakarta release

## [5.1.6] - 2025-05-25
### Added
- Support for simple web applications 
  configured by a json file in /classes

## [5.1.0] - 2023-12-04
### Added
- Support for the Saxon XSLT support

## [5.0.0] - 2022-02-12
### Changed
- Use bedework-parent

## [4.1.0] - 2020-03-20
### Added
- Whole package - split off from bw-util

