# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-08-03

### Added
- Initial release of nanoid-java
- `nanoid()` method for generating 21-character IDs with default URL-friendly alphabet
- `nanoid(int size)` method for generating IDs with custom length
- `customNanoid(String alphabet, int size)` method for generating IDs with custom alphabet and length
- Comprehensive test suite with 11 test cases
- Input validation for all public methods
- Complete JavaDoc documentation
- Maven build configuration with Java 17 support
- GitHub Actions CI/CD pipeline
- Comprehensive README with usage examples
- MIT License
- Contributing guidelines
- Issue templates for bug reports and feature requests

### Security
- Uses `SecureRandom` for cryptographically secure ID generation
- Uniform distribution across alphabet characters
- No predictable patterns in generated IDs

[1.0.0]: https://github.com/ai/nanoid-java/releases/tag/v1.0.0
