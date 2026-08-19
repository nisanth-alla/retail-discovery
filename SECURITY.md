# Security Policy

## Reporting a vulnerability

Please do not open a public issue for a security problem. Report it privately to the repository owner so it can be assessed and addressed before disclosure.

For anything that involves a credential, a browser session, or personal data, treat it as sensitive and report it directly. If a secret or session has already been exposed, revoke it immediately and preserve only the minimum evidence needed for remediation.

## Supported

This is a personal open-source project. Security fixes are prioritized based on severity and the availability of a maintainer. There is no SLA.

## Scope

- Authentication and session handling.
- Server-side vendor authorization.
- Secret and key management.
- Exposure of personal or catalogue data.

Out-of-scope items such as deployment infrastructure credentials live in the deployment platform's secret store, not the repository.
