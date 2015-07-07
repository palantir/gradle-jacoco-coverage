# Changelog for Gradle Jacoco Coverage Plugin

## 0.2.0
- Introduction of coverage "realms" (file, class, package, scope) in lieu of purely regex-based threshold definitions
- New threshold specification syntax based on realms: '_file 0.5' or '_package 0.5' instead of 'threshold 0.5 PATTERN'

## 0.1.5
- Add jacocoFull extension with excludeProject configuration

## 0.1.4
- Generalise from file-name-based to scope-based coverage specification (no configuration back-compat break)
- Evaluate JacocoFullReport inputs lazily

## 0.1.3
- Add 'jacoco-full-report' plugin
- Add integration tests

## 0.1.0
- Initial release
