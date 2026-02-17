# Secure Mail - Modern Email Replacement

A privacy-first, secure email replacement system with end-to-end encryption, federation support, and anti-enumeration protections.

## Overview

Secure Mail reimagines email communication for the modern era with:

- **End-to-End Encryption (E2EE)**: All messages encrypted from sender to recipient
- **Federation Support**: Decentralized architecture allowing anyone to run a server
- **Anti-Enumeration**: Privacy protections preventing user discovery and metadata leakage
- **Transparency Logs**: Cryptographic auditability of identity operations
- **Modern Protocol**: Designed from the ground up with security and privacy in mind

## Project Status

**Current Phase**: Initial Development - Demo Milestone

This project is in early development. See [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) for roadmap and milestones.

### Demo Milestone Goal

Build a minimal working system with:
- 3 federated servers
- 1 client per server (3 clients total)
- Ability to exchange encrypted messages between users on different servers

## Documentation

### Product Requirements
- [Server PRD](docs/prd/SERVER_PRD.md) - Server component requirements and features
- [Client PRD](docs/prd/CLIENT_PRD.md) - Client application requirements and features

### Architecture
- [Protocol Overview](docs/architecture/PROTOCOL.md) - Core protocol design and message flow
- [Identity System](docs/architecture/IDENTITY.md) - Identity management and key handling
- [Federation](docs/architecture/FEDERATION.md) - Server-to-server communication
- [End-to-End Encryption](docs/architecture/E2EE.md) - Encryption implementation details
- [Anti-Enumeration](docs/architecture/ANTI_ENUMERATION.md) - Privacy protection mechanisms
- [Discovery & Attestation](docs/architecture/DISCOVERY.md) - How clients find and verify servers
- [Transparency Logs](docs/architecture/TRANSPARENCY.md) - Cryptographic audit trails

### Planning
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md) - Roadmap, milestones, and priorities

## Quick Start

### Running the Demo

```bash
# Start three federated servers
cd server
./run-demo-servers.sh

# In separate terminals, start clients
cd client
./run-demo-client.sh server1
./run-demo-client.sh server2
./run-demo-client.sh server3
```

### Development Setup

#### Server
```bash
cd server
npm install
npm run build
npm test
```

#### Client
```bash
cd client
npm install
npm run build
npm test
```

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Key Areas for Contribution
- Protocol design and security review
- Implementation of core features
- Testing and quality assurance
- Documentation improvements
- UI/UX design for client applications

## Security

Security is our top priority. Please report security vulnerabilities to security@[domain].

See [SECURITY.md](SECURITY.md) for our security policy and procedures.

## License

[Choose appropriate license - MIT, Apache 2.0, GPL, etc.]

## Contact

- Project Lead: [Name]
- Email: [contact email]
- Discussion: [Link to discussions/forum]
- Chat: [Link to chat platform]

## Acknowledgments

This project builds on research and ideas from:
- Signal Protocol for E2EE messaging
- Matrix for federation concepts
- Certificate Transparency for transparency logs
- Tor for anti-enumeration techniques
