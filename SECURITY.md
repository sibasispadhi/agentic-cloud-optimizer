# Security Policy

## Supported Versions

The following versions of Agent Cloud Optimizer are currently supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1   | :x:                |

## Reporting a Vulnerability

We take the security of Agent Cloud Optimizer seriously. If you believe you have found a security vulnerability, please report it to us responsibly.

### How to Report

**Please do NOT report security vulnerabilities through public GitHub issues.**

Instead, please report them via:

1. **GitHub Security Advisories** (preferred)
   - Navigate to the repository's "Security" tab
   - Click "Report a vulnerability"
   - Fill out the advisory form

2. **Email** (alternative)
   - Send details to: [your-email-here]
   - Use subject line: "[SECURITY] Agent Cloud Optimizer Vulnerability"

### What to Include

Please include the following information in your report:

- **Type of vulnerability** (e.g., injection, authentication bypass, etc.)
- **Full paths of affected source file(s)**
- **Location of the affected code** (tag/branch/commit or direct URL)
- **Step-by-step instructions to reproduce the issue**
- **Proof-of-concept or exploit code** (if possible)
- **Impact of the vulnerability**
- **Suggested fix** (if you have one)

### What to Expect

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours
- **Communication**: We will keep you informed of our progress
- **Timeline**: We aim to patch critical vulnerabilities within 7 days
- **Credit**: We will credit you in the security advisory (unless you prefer to remain anonymous)

### Our Commitment

- We will respond to your report promptly
- We will keep you informed throughout the process
- We will work with you to understand and resolve the issue
- We will publicly acknowledge your responsible disclosure (with your permission)

## Security Best Practices

When using Agent Cloud Optimizer:

### For Development

- **Keep dependencies updated**: Regularly update Spring Boot, Spring AI, and other dependencies
- **Review metrics data**: Ensure sensitive data is not logged in metrics
- **Secure Ollama**: If using LLM agent, ensure Ollama is not exposed to untrusted networks
- **Validate inputs**: Always validate external inputs before processing

### For Production (Future)

This is currently a proof-of-concept. Before production use:

- [ ] Implement authentication and authorization
- [ ] Add rate limiting and request throttling
- [ ] Enable HTTPS/TLS for all communications
- [ ] Implement audit logging
- [ ] Add input validation and sanitization
- [ ] Configure security headers
- [ ] Regular security scanning of dependencies
- [ ] Principle of least privilege for service accounts

## Known Security Considerations

### Current Version (0.1.0)

1. **No Authentication**: Demo API has no authentication (intentional for POC)
2. **Local Development Only**: Not hardened for production environments
3. **Metrics Data**: Metrics are written to local disk without encryption
4. **LLM Integration**: Ollama runs locally without sandboxing

### Dependency Security

- Dependencies are regularly scanned
- See `pom.xml` for current versions
- Update to patch versions as they become available

## Security Updates

Security updates will be:

- Released as patch versions (e.g., 0.1.1)
- Announced via GitHub Security Advisories
- Documented in CHANGELOG.md
- Tagged with "security" label in releases

## Scope

### In Scope

- Vulnerabilities in application code
- Dependency vulnerabilities
- Configuration security issues
- Authentication/authorization bypasses (when implemented)
- Data exposure issues

### Out of Scope

- Denial of Service (DoS) attacks (this is a local development tool)
- Social engineering attacks
- Physical access attacks
- Issues in third-party dependencies (report to respective projects)

## Additional Resources

- [OWASP Top Ten](https://owasp.org/www-project-top-ten/)
- [Spring Security](https://spring.io/projects/spring-security)
- [Java Security Best Practices](https://snyk.io/blog/java-security-best-practices/)

## Contact

For security-related questions that are not vulnerabilities, please open a GitHub Discussion.

---

**Last Updated**: December 30, 2025
