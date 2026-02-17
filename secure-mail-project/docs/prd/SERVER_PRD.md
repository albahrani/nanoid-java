# Server PRD - Secure Mail

## Product Overview

The Secure Mail server is a federated email-replacement server that handles message storage, routing, and federation while maintaining end-to-end encryption and privacy protections.

## Goals

1. **Enable Secure Communication**: Route E2EE messages between users
2. **Support Federation**: Interoperate with other Secure Mail servers
3. **Protect Privacy**: Implement anti-enumeration and metadata minimization
4. **Ensure Reliability**: Provide highly available message delivery
5. **Maintain Auditability**: Log identity operations in transparency logs

## Non-Goals

- Reading or decrypting user messages (E2EE ensures servers cannot read content)
- Social features like contacts management (handled client-side)
- Spam filtering based on content (only metadata-based filtering)
- Real-time typing indicators (privacy concern)

## User Stories

### As a User
- I want my messages to be stored securely when recipients are offline
- I want to access my messages from multiple devices
- I want my identity to be cryptographically verifiable
- I want assurance my communications are private

### As a Server Operator
- I want to run my own server to maintain independence
- I want to federate with other servers to reach all users
- I want clear resource requirements and scalability options
- I want automated security updates and monitoring

### As a Developer
- I want clear APIs for client integration
- I want comprehensive security documentation
- I want extensibility for custom features
- I want good logging for debugging

## Core Features

### 1. User Account Management

**Priority**: P0 (Demo Milestone)

- User registration with cryptographic identity
- Account recovery mechanisms
- Multi-device support
- Key rotation capabilities

**API**:
```
POST /api/v1/accounts/register
POST /api/v1/accounts/login
POST /api/v1/accounts/add-device
PUT /api/v1/accounts/rotate-keys
```

### 2. Message Storage & Delivery

**Priority**: P0 (Demo Milestone)

- Receive encrypted messages from clients
- Store messages for offline recipients
- Deliver messages when recipients connect
- Message acknowledgment and deletion

**API**:
```
POST /api/v1/messages/send
GET /api/v1/messages/inbox
POST /api/v1/messages/ack
DELETE /api/v1/messages/{id}
```

### 3. Federation

**Priority**: P0 (Demo Milestone)

- Discover other servers via DNS/HTTPS
- Establish secure connections to peer servers
- Route messages to users on remote servers
- Handle server availability and retries

**API**:
```
POST /federation/v1/deliver
GET /federation/v1/capabilities
GET /federation/v1/user-info
```

### 4. Anti-Enumeration

**Priority**: P1

- No public user directories
- Timing attack mitigation on user lookups
- Rate limiting on discovery attempts
- Plausible deniability for user existence

**Features**:
- Constant-time user existence checks
- Honeypot accounts to detect enumeration
- IP-based rate limiting
- No user search functionality

### 5. Key Management & Identity

**Priority**: P0 (Demo Milestone)

- Public key directory for users
- Key verification and signing
- Identity key rotation
- Prekey management for Signal protocol

**API**:
```
GET /api/v1/keys/{user}
POST /api/v1/keys/upload
POST /api/v1/keys/rotate
GET /api/v1/prekeys/{user}
```

### 6. Transparency Logs

**Priority**: P2

- Log all identity operations (registration, key changes)
- Merkle tree-based audit trail
- Signed tree heads
- Public verifiability

**API**:
```
GET /transparency/v1/logs
GET /transparency/v1/proof/{user}
GET /transparency/v1/tree-head
```

### 7. Discovery & Attestation

**Priority**: P1

- Server discovery via .well-known
- TLS certificate validation
- Server capability advertisement
- Version negotiation

**Endpoints**:
```
GET /.well-known/secure-mail
GET /api/v1/server-info
```

## Technical Requirements

### Performance
- Support 10,000+ concurrent connections (demo: 100+)
- Message delivery latency < 500ms within same server
- Cross-server delivery < 2s (network dependent)
- Database query time < 100ms (p95)

### Scalability
- Horizontal scaling for stateless components
- Database sharding support
- Message queue for async processing
- Load balancing ready

### Security
- TLS 1.3 for all connections
- Perfect forward secrecy
- Regular security audits
- Automated vulnerability scanning
- Rate limiting on all endpoints

### Privacy
- Minimal metadata collection
- No content inspection
- IP address protection options (Tor support)
- Anonymous usage metrics only

### Reliability
- 99.9% uptime SLA (production)
- Automated backups
- Disaster recovery procedures
- Health check endpoints

## Data Models

### User Account
```json
{
  "userId": "uuid",
  "username": "string",
  "domain": "string",
  "identityKey": "base64-public-key",
  "registrationTimestamp": "iso8601",
  "devices": ["device-id-1", "device-id-2"]
}
```

### Message
```json
{
  "messageId": "uuid",
  "from": "user@domain",
  "to": "user@domain",
  "encryptedPayload": "base64",
  "timestamp": "iso8601",
  "deliveryStatus": "pending|delivered|failed"
}
```

### Device
```json
{
  "deviceId": "uuid",
  "userId": "uuid",
  "signingKey": "base64-public-key",
  "prekeyBundle": "base64"
}
```

## Success Metrics

### Demo Milestone
- ✓ 3 servers can federate
- ✓ Messages delivered between all server pairs
- ✓ E2EE verified working
- ✓ Basic anti-enumeration in place

### Production Metrics
- Message delivery success rate > 99.9%
- User registration time < 5 seconds
- Zero unauthorized data access incidents
- Server uptime > 99.9%

## Open Questions

1. **Database Choice**: PostgreSQL vs. SQLite vs. Cassandra?
   - Demo: SQLite for simplicity
   - Production: PostgreSQL recommended

2. **Message Retention**: How long to store undelivered messages?
   - Recommendation: 30 days, configurable

3. **Federation Discovery**: DNS vs. centralized directory?
   - Recommendation: DNS-based (.well-known), optional directory

4. **Rate Limiting**: Per IP, per user, or both?
   - Recommendation: Both, with different thresholds

## Dependencies

- **Language**: Node.js (TypeScript) or Go
- **Database**: PostgreSQL (production), SQLite (demo)
- **Crypto**: libsodium or noble-curves
- **Web Framework**: Express (Node) or Gin (Go)
- **Queue**: Redis or in-memory (demo)

## Timeline

- **Phase 1 (Demo)**: 4-6 weeks
  - Basic message routing
  - Simple federation
  - E2EE foundation
  
- **Phase 2 (Alpha)**: 8-12 weeks
  - Anti-enumeration complete
  - Transparency logs
  - Production hardening
  
- **Phase 3 (Beta)**: 12-16 weeks
  - Performance optimization
  - Comprehensive testing
  - Security audit

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Protocol vulnerabilities | High | Security review, formal verification |
| Federation complexity | Medium | Start simple, iterate |
| Scalability issues | Medium | Load testing early, horizontal scaling |
| Key management errors | High | Well-tested crypto libraries, key backup |
| Metadata leakage | High | Privacy review, minimal collection |
