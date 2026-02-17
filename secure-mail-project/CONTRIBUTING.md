# Contributing to Secure Mail

Thank you for your interest in contributing to Secure Mail! This document provides guidelines and information for contributors.

## Code of Conduct

We are committed to providing a welcoming and inclusive environment. Please be respectful and professional in all interactions.

## How to Contribute

### Reporting Bugs

1. Check if the bug has already been reported in [Issues](../../issues)
2. If not, create a new issue with:
   - Clear, descriptive title
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, version, etc.)
   - Screenshots if applicable

### Suggesting Features

1. Check [Issues](../../issues) and [Discussions](../../discussions) for similar suggestions
2. Create a new issue or discussion with:
   - Clear description of the feature
   - Use cases and benefits
   - Potential implementation approach

### Submitting Changes

1. **Fork the repository**
2. **Create a branch** for your changes
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
   - Follow the code style guidelines
   - Add tests for new functionality
   - Update documentation as needed
4. **Test your changes**
   ```bash
   npm test
   npm run lint
   npm run typecheck
   ```
5. **Commit your changes**
   ```bash
   git commit -m "feat: add new feature"
   ```
   Follow [Conventional Commits](https://www.conventionalcommits.org/)
6. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request**
   - Describe your changes
   - Reference related issues
   - Ensure CI passes

## Development Setup

### Prerequisites

- Node.js >= 18
- npm or yarn
- Git

### Setup

```bash
# Clone the repository
git clone https://github.com/albahrani/secure-mail.git
cd secure-mail

# Install dependencies
cd server && npm install
cd ../client && npm install

# Run tests
npm test

# Start development
npm run dev
```

## Code Style

### TypeScript

- Use TypeScript strict mode
- Prefer `const` over `let`
- Use meaningful variable names
- Add JSDoc comments for public APIs

### Formatting

We use Prettier for code formatting:
```bash
npm run format
```

### Linting

We use ESLint for code quality:
```bash
npm run lint
```

## Testing

- Write tests for all new functionality
- Aim for >80% code coverage
- Include unit, integration, and E2E tests as appropriate

```bash
npm test              # Run all tests
npm run test:unit     # Unit tests only
npm run test:coverage # Generate coverage report
```

## Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new feature
fix: fix bug
docs: update documentation
style: formatting changes
refactor: code refactoring
test: add tests
chore: maintenance tasks
```

## Security

**NEVER commit:**
- Private keys
- Passwords
- API keys
- Sensitive user data

If you discover a security vulnerability, please email security@[domain] instead of opening an issue.

## Documentation

- Update README.md for user-facing changes
- Update API documentation for API changes
- Add comments for complex code
- Update architecture docs for design changes

## Review Process

1. All PRs require review from at least one maintainer
2. CI must pass (tests, linting, type checking)
3. Documentation must be updated
4. Code must follow style guidelines

## Getting Help

- [Discussions](../../discussions) - General questions and ideas
- [Issues](../../issues) - Bug reports and feature requests
- [Discord/Slack] - Real-time chat (if available)

## License

By contributing, you agree that your contributions will be licensed under the project's license.

## Recognition

Contributors will be recognized in:
- CONTRIBUTORS.md file
- Release notes
- Project website (if applicable)

Thank you for contributing to Secure Mail! 🎉
