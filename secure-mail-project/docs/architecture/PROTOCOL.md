# Protocol Overview - Secure Mail

## Introduction

The Secure Mail Protocol is a modern, federated messaging protocol designed for privacy, security, and decentralization. It combines elements from email (federation), Signal (E2EE), and Matrix (modern architecture) while addressing their various limitations.

## Design Principles

1. **Privacy First**: Minimal metadata collection, anti-enumeration, E2EE by default
2. **Decentralized**: Anyone can run a server, no central authority
3. **Secure by Default**: Strong cryptography, forward secrecy, authenticated encryption
4. **Simple**: Clean protocol design, easy to implement correctly
5. **Auditable**: Transparency logs for identity operations

## Protocol Stack

```
┌─────────────────────────────────────────┐
│         Application Layer               │
│    (Message Composition, UI, etc.)      │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│      Secure Mail Protocol (SMP)         │
│   - Message Format                      │
│   - Routing                             │
│   - Federation                          │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│      End-to-End Encryption Layer        │
│   - Signal Protocol                     │
│   - Key Exchange                        │
│   - Forward Secrecy                     │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│         Transport Security              │
│   - TLS 1.3                             │
│   - Certificate Validation              │
└─────────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│            HTTP/2 or QUIC               │
└─────────────────────────────────────────┘
```

## Core Concepts

### Addressing

Users are addressed as `username@domain`, similar to email:
- `alice@example.com`
- `bob@securemail.org`

Addresses are:
- **Case-insensitive**: `Alice@Example.Com` = `alice@example.com`
- **Globally unique**: Each username@domain is unique
- **Federated**: Users can message across domains

### Identity

Each user has:
- **Identity Key**: Long-term Ed25519 public/private keypair
- **Signed Prekeys**: Medium-term keys signed by identity key
- **One-Time Prekeys**: Ephemeral keys for perfect forward secrecy

Identity keys are published in:
1. Server's key directory
2. Transparency logs (for auditability)

### Message Flow

#### Client-to-Client (Same Server)

```
Alice@srv1            Server 1            Bob@srv1
    │                     │                    │
    │  1. Fetch Keys      │                    │
    ├────────────────────>│                    │
    │<────────────────────┤                    │
    │                     │                    │
    │  2. Encrypt+Send    │                    │
    ├────────────────────>│                    │
    │                     │  3. Store          │
    │                     │                    │
    │                     │  4. Deliver        │
    │                     ├───────────────────>│
    │                     │                    │
    │  5. ACK             │  6. ACK            │
    │<────────────────────┤<───────────────────┤
```

#### Client-to-Client (Different Servers)

```
Alice@srv1      Server 1       Server 2      Bob@srv2
    │               │               │              │
    │  1. Send      │               │              │
    ├──────────────>│               │              │
    │               │  2. Federate  │              │
    │               ├──────────────>│              │
    │               │               │  3. Store    │
    │               │               │              │
    │               │               │  4. Deliver  │
    │               │               ├─────────────>│
    │               │  5. ACK       │  6. ACK      │
    │               │<──────────────┤<─────────────┤
    │  7. ACK       │               │              │
    │<──────────────┤               │              │
```

## Message Format

### Encrypted Message Envelope

```json
{
  "version": "1.0",
  "messageId": "uuid-v4",
  "from": "alice@example.com",
  "to": "bob@example.org",
  "timestamp": "2026-02-17T21:00:00Z",
  "type": "message",
  "encryptedPayload": {
    "ciphertext": "base64-encrypted-data",
    "ephemeralKey": "base64-public-key",
    "counter": 42,
    "previousChainLength": 1337
  },
  "signature": "base64-sender-signature"
}
```

### Decrypted Message Content

```json
{
  "contentType": "text/plain",
  "content": "Hello, Bob!",
  "timestamp": "2026-02-17T21:00:00Z",
  "threadId": "uuid-v4",
  "padding": "random-data-for-length-hiding"
}
```

## API Endpoints

### Client-to-Server API

#### Authentication
```
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
```

#### Key Management
```
GET /api/v1/keys/{address}
POST /api/v1/keys/upload
POST /api/v1/keys/prekeys
GET /api/v1/keys/prekeys/{address}
```

#### Messages
```
POST /api/v1/messages/send
GET /api/v1/messages/inbox
POST /api/v1/messages/ack/{messageId}
DELETE /api/v1/messages/{messageId}
```

#### Contacts
```
GET /api/v1/users/{address}/info
GET /api/v1/users/{address}/devices
```

### Server-to-Server API (Federation)

```
POST /federation/v1/deliver
GET /federation/v1/capabilities
GET /federation/v1/keys/{address}
POST /federation/v1/verify
```

### Discovery

```
GET /.well-known/secure-mail
GET /.well-known/secure-mail/server-info
```

Example `.well-known/secure-mail`:
```json
{
  "domain": "example.com",
  "serverUrl": "https://mail.example.com",
  "version": "1.0",
  "capabilities": [
    "federation",
    "transparency-logs",
    "anti-enumeration"
  ],
  "publicKey": "base64-server-identity-key",
  "certificateTransparency": {
    "logUrl": "https://transparency.example.com",
    "logId": "base64-log-id"
  }
}
```

## Security Properties

### Confidentiality
- **E2EE**: Servers cannot read message content
- **Forward Secrecy**: Past messages secure even if keys compromised
- **Future Secrecy**: Self-healing ratchet

### Authenticity
- **Identity Keys**: Verify sender identity
- **Message Signatures**: Prevent tampering
- **Transparency Logs**: Detect key substitution

### Integrity
- **AEAD**: Authenticated encryption
- **Message Signatures**: Detect modifications
- **Delivery Confirmations**: Track message state

### Privacy
- **Anti-Enumeration**: Can't discover user list
- **Metadata Minimization**: Limited metadata collection
- **Tor Support**: IP address protection
- **Padding**: Hide message length patterns

## State Management

### Client State
- Local message database
- Contact list with identity keys
- Own identity and device keys
- Conversation ratchet states
- Pending message queue

### Server State
- User accounts and public keys
- Undelivered message queue
- Transparency logs
- Federation peer list
- Rate limiting counters

## Error Handling

### Client Errors (4xx)
- `400 Bad Request`: Malformed message
- `401 Unauthorized`: Invalid credentials
- `403 Forbidden`: Access denied
- `404 Not Found`: User doesn't exist (with anti-enumeration delay)
- `429 Too Many Requests`: Rate limited

### Server Errors (5xx)
- `500 Internal Server Error`: Server error
- `502 Bad Gateway`: Federation error
- `503 Service Unavailable`: Server overloaded

### Retry Logic
- Exponential backoff for temporary failures
- Maximum retry count
- Dead letter queue for permanent failures

## Versioning

Protocol version in all messages: `"version": "1.0"`

Version negotiation:
1. Client sends supported versions
2. Server responds with chosen version
3. Use lowest common version

Breaking changes require new major version.

## Performance Considerations

### Optimization Strategies
- **Batching**: Send multiple messages in one request
- **Compression**: Gzip/Brotli for message payloads
- **Caching**: Cache public keys and server info
- **Connection Pooling**: Reuse TLS connections
- **Prekey Bundles**: Fetch multiple prekeys at once

### Scalability
- **Horizontal Scaling**: Stateless servers
- **Database Sharding**: By user ID hash
- **Message Queues**: Async processing
- **CDN**: Static content delivery

## Comparison to Other Protocols

| Feature | Secure Mail | Email (SMTP) | Signal | Matrix |
|---------|-------------|--------------|--------|--------|
| E2EE | ✓ (Default) | ✗ (Optional) | ✓ | ✓ (Optional) |
| Federation | ✓ | ✓ | ✗ | ✓ |
| Metadata Protection | ✓ | ✗ | ✓ | Partial |
| Decentralized | ✓ | ✓ | ✗ | ✓ |
| Forward Secrecy | ✓ | ✗ | ✓ | ✓ |
| Anti-Enumeration | ✓ | ✗ | ✓ | ✗ |

## Future Extensions

- **Group Messages**: Multi-party encryption
- **Voice/Video**: Real-time encrypted calls
- **File Attachments**: Large file support
- **Reactions**: Message reactions/emojis
- **Threading**: Conversation threads
- **Search**: Encrypted search support
