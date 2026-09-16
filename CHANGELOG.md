# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Gradle project structure with Kotlin DSL
- Core foundation classes:
  - `PushPlatform` - Main SDK entry point (singleton)
  - `PushConfiguration` - SDK configuration
  - `InstallationManager` - UUID generation and persistence
  - `SecureStorage` - EncryptedSharedPreferences wrapper (API 23+, fallback for API 21-22)
  - `Logger` - Debug logging with token masking
  - `SdkError` - SDK error types
- AndroidManifest.xml with INTERNET permission
- ProGuard/R8 consumer rules
- Unit tests: 36+ tests for core foundation
- README and CHANGELOG

### Security
- Installation ID stored in EncryptedSharedPreferences (API 23+)
- Token masking in debug logs (first 8 characters only)
- MODE_PRIVATE for SharedPreferences fallback

## [1.0.0] - TBD

Initial release (in development)
