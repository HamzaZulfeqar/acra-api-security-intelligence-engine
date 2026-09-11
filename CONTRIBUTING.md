# Contributing Guidelines

Thank you for your interest in contributing.

These repositories contain cybersecurity research, security tooling, academic projects, experiments, and practical security engineering work. Contributions should maintain a high standard of technical quality, security, and documentation.

## Development Workflow

1. Fork the repository
2. Create a dedicated feature or fix branch
3. Make your changes
4. Add or update relevant tests
5. Review the changes for security and code quality
6. Update documentation where necessary
7. Submit a pull request

## Code Standards

- Follow the existing project structure
- Write clean, readable, and maintainable code
- Keep security-sensitive logic explicit and reviewable
- Avoid unnecessary dependencies
- Validate external input appropriately
- Handle errors safely
- Do not commit credentials, secrets, API keys, or private data
- Add tests for new functionality where applicable
- Update documentation for significant changes

## Security Contributions

Security-related contributions are especially welcome when they improve:

- API security analysis
- Authorization and access-control testing
- Vulnerability detection
- Security automation
- Detection engineering
- Evidence collection and analysis
- Security validation
- Test coverage
- Secure software design

Security research and testing must be conducted only against systems and environments for which appropriate authorization exists.

## Pull Requests

Pull requests should clearly explain:

- What was changed
- Why the change was necessary
- How it was tested
- Any security implications
- Any limitations or known issues

Keep pull requests focused on a specific improvement whenever possible.

## Commit Messages

Use clear and descriptive commit messages.

Examples:

feat: add authorization analyzer

fix: resolve API validation issue

test: add access control test cases

docs: update security documentation

refactor: improve request analysis pipeline
