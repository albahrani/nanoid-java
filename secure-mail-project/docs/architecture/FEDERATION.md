# Federation - Secure Mail

## Overview

Federation allows independent Secure Mail servers to communicate, enabling users on different servers to exchange messages. This creates a decentralized network similar to email, but with modern security and privacy features.

## Design Goals

1. **Decentralization**: No single point of control
2. **Simple Discovery**: Easy to find and connect to servers
3. **Secure Communication**: TLS with certificate validation
4. **Privacy-Preserving**: Minimal metadata exposure
5. **Resilient**: Handle server failures gracefully

## Federation Model

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Server A   │◄───────►│   Server B   │◄───────►│   Server C   │
│example.com   │         │ secure.org   │         │  mail.net    │
└──────────────┘         └──────────────┘         └──────────────┘
       ▲                        ▲                        ▲
       │                        │                        │
    ┌──┴──┐                 ┌──┴──┐                 ┌──┴──┐
    │Alice│                 │ Bob │                 │Carol│
    └─────┘                 └─────┘                 └─────┘
```

Each server:
- Manages its own users
- Stores messages for its users
- Routes messages to other servers
- Validates incoming messages

## Server Discovery

### DNS-Based Discovery

Servers advertise themselves via `.well-known`:

```
https://example.com/.well-known/secure-mail
```

Example response:
```json
{
  "domain": "example.com",
  "serverUrl": "https://mail.example.com",
  "version": "1.0",
  "publicKey": "base64-server-signing-key",
  "capabilities": [
    "federation",
    "e2ee",
    "transparency-logs",
    "anti-enumeration"
  ],
  "federationEndpoint": "https://mail.example.com/federation/v1",
  "maxMessageSize": 10485760,
  "supportedVersions": ["1.0", "1.1"]
}
```

### Discovery Process

```typescript
async function discoverServer(domain: string): Promise<ServerInfo> {
  // Try HTTPS first
  const wellKnownUrl = `https://${domain}/.well-known/secure-mail`;
  
  try {
    const response = await fetch(wellKnownUrl);
    if (response.ok) {
      return await response.json();
    }
  } catch (error) {
    // Fallback to DNS TXT record (future)
    // const txtRecord = await dns.resolveTxt(`_securemail.${domain}`);
  }
  
  throw new Error(`Cannot discover server for ${domain}`);
}
```

## Message Delivery

### Same-Server Delivery

Messages between users on the same server:

```
Alice@srv1 → Server 1 → Bob@srv1
```

1. Alice sends to server
2. Server stores in Bob's inbox
3. Server delivers when Bob connects

### Cross-Server Delivery

Messages between users on different servers:

```
Alice@srv1 → Server 1 → Server 2 → Bob@srv2
```

1. Alice sends to Server 1
2. Server 1 discovers Server 2
3. Server 1 delivers to Server 2
4. Server 2 stores in Bob's inbox
5. Server 2 delivers when Bob connects

### Federation API

#### Deliver Message

```http
POST /federation/v1/deliver
Host: mail.example.org
Content-Type: application/json
Authorization: Bearer <server-auth-token>

{
  "messageId": "uuid",
  "from": "alice@example.com",
  "to": "bob@example.org",
  "encryptedPayload": "base64-ciphertext",
  "signature": "base64-sender-signature",
  "timestamp": "2026-02-17T21:00:00Z"
}
```

Response:
```json
{
  "status": "accepted|rejected",
  "messageId": "uuid",
  "reason": "optional error message"
}
```

#### Fetch User Info

```http
GET /federation/v1/users/bob@example.org/info
Host: mail.example.org
Authorization: Bearer <server-auth-token>
```

Response:
```json
{
  "address": "bob@example.org",
  "identityKey": "base64-public-key",
  "devices": [
    {
      "deviceId": "uuid",
      "deviceKey": "base64-public-key"
    }
  ]
}
```

## Server Authentication

### Server-to-Server Auth

Servers authenticate using:
1. **TLS Certificates**: Validate domain ownership
2. **Server Signing Keys**: Sign requests with server key
3. **Token-Based Auth**: JWT tokens for session auth

```typescript
async function authenticateFederationRequest(
  request: Request,
  sourceDomain: string
): Promise<boolean> {
  // 1. Verify TLS certificate
  const cert = request.socket.getPeerCertificate();
  if (!validateCertificate(cert, sourceDomain)) {
    return false;
  }
  
  // 2. Verify request signature
  const serverInfo = await discoverServer(sourceDomain);
  const signature = request.headers['x-signature'];
  const body = await request.text();
  
  const valid = Ed25519.verify(
    serverInfo.publicKey,
    body,
    signature
  );
  
  return valid;
}
```

### Request Signing

```typescript
async function signFederationRequest(
  method: string,
  path: string,
  body: string
): Promise<string> {
  const timestamp = Date.now();
  const nonce = randomBytes(16);
  
  const data = `${method}\n${path}\n${timestamp}\n${nonce}\n${body}`;
  const signature = Ed25519.sign(serverPrivateKey, data);
  
  return `v1:${timestamp}:${nonce}:${signature}`;
}
```

## Retry Logic

### Delivery Retry Strategy

```typescript
const RETRY_DELAYS = [
  30,      // 30 seconds
  300,     // 5 minutes
  1800,    // 30 minutes
  3600,    // 1 hour
  21600,   // 6 hours
  86400    // 24 hours
];

async function deliverWithRetry(
  targetServer: string,
  message: Message
): Promise<void> {
  for (let attempt = 0; attempt < RETRY_DELAYS.length; attempt++) {
    try {
      await federationClient.deliver(targetServer, message);
      return; // Success
    } catch (error) {
      if (isPermanentError(error)) {
        // Give up immediately
        await markAsPermanentFailure(message);
        return;
      }
      
      // Wait before retry
      if (attempt < RETRY_DELAYS.length - 1) {
        await sleep(RETRY_DELAYS[attempt] * 1000);
      }
    }
  }
  
  // All retries exhausted
  await markAsPermanentFailure(message);
}

function isPermanentError(error: Error): boolean {
  // 4xx errors are permanent (except 429)
  if (error.status >= 400 && error.status < 500 && error.status !== 429) {
    return true;
  }
  return false;
}
```

## Rate Limiting

### Incoming Federation Requests

```typescript
const RATE_LIMITS = {
  perServer: {
    messages: 1000,    // per hour
    keyLookups: 100,   // per hour
    userInfo: 50       // per hour
  },
  global: {
    messages: 10000,   // per hour across all servers
    connections: 100   // concurrent connections
  }
};

class FederationRateLimiter {
  async checkLimit(
    sourceDomain: string,
    action: string
  ): Promise<boolean> {
    const key = `federation:${sourceDomain}:${action}`;
    const count = await redis.incr(key);
    
    if (count === 1) {
      await redis.expire(key, 3600); // 1 hour
    }
    
    const limit = RATE_LIMITS.perServer[action];
    return count <= limit;
  }
}
```

### Backpressure

When receiving server is overloaded:

```http
HTTP/1.1 503 Service Unavailable
Retry-After: 300

{
  "error": "server_overloaded",
  "retryAfter": 300
}
```

## Server Blocklists

### Blocking Abusive Servers

```typescript
interface ServerBlocklist {
  domain: string;
  reason: string;
  blockedAt: Date;
  expiresAt: Date | null; // null = permanent
}

async function checkServerBlocked(domain: string): Promise<boolean> {
  const entry = await db.query(
    'SELECT * FROM server_blocklist WHERE domain = ? AND (expiresAt IS NULL OR expiresAt > NOW())',
    [domain]
  );
  
  return entry.length > 0;
}

async function blockServer(
  domain: string,
  reason: string,
  duration?: number
): Promise<void> {
  await db.insert('server_blocklist', {
    domain,
    reason,
    blockedAt: new Date(),
    expiresAt: duration ? new Date(Date.now() + duration) : null
  });
  
  // Reject all pending messages from this server
  await rejectPendingMessages(domain);
}
```

### Shared Blocklists (Optional)

Servers can subscribe to shared blocklists:

```json
{
  "name": "Secure Mail Default Blocklist",
  "maintainer": "community@securemail.org",
  "version": 42,
  "entries": [
    {
      "domain": "spam-server.com",
      "reason": "persistent spam",
      "severity": "high"
    },
    {
      "domain": "phishing-server.org",
      "reason": "phishing attempts",
      "severity": "critical"
    }
  ]
}
```

## Privacy Considerations

### Metadata Leakage

Information leaked during federation:
- Sender domain (necessary for routing)
- Recipient domain (necessary for routing)
- Message timestamp (necessary for ordering)
- Message size (approximate, padded)

NOT leaked:
- Sender username (if anti-enumeration enabled)
- Message content (E2EE)
- Recipient behavior (server doesn't confirm delivery to sender's server)

### Anti-Correlation

Prevent traffic analysis:
- Batch messages to same server
- Pad message sizes
- Random delays (within bounds)
- Mix networks (future: Tor/I2P integration)

## Monitoring & Health Checks

### Server Health Endpoint

```http
GET /federation/v1/health
```

Response:
```json
{
  "status": "healthy|degraded|down",
  "version": "1.0.2",
  "uptime": 86400,
  "queueDepth": 42,
  "load": {
    "cpu": 0.45,
    "memory": 0.62,
    "disk": 0.23
  }
}
```

### Federation Metrics

Servers track:
- Messages delivered/received per peer
- Delivery success rate per peer
- Average delivery latency per peer
- Failed delivery attempts
- Blocked requests

## Failure Scenarios

### Scenario 1: Target Server Down

```
1. Alice sends message to Bob@down-server.com
2. Alice's server tries to deliver
3. Connection fails (timeout/refused)
4. Alice's server queues message
5. Retries with exponential backoff
6. After 7 days, sends failure notification to Alice
```

### Scenario 2: Network Partition

```
1. Server A and Server B lose connectivity
2. Messages queue on both sides
3. When connection restored, queues drain
4. Messages delivered (may be out of order)
5. Clients handle out-of-order with timestamps
```

### Scenario 3: Certificate Expiry

```
1. Server B's TLS certificate expires
2. Server A refuses connection
3. Server A alerts admin
4. Server B admin renews certificate
5. Federation resumes automatically
```

## Scaling Federation

### Connection Pooling

```typescript
class FederationConnectionPool {
  private pools: Map<string, ConnectionPool> = new Map();
  
  async getConnection(domain: string): Promise<Connection> {
    if (!this.pools.has(domain)) {
      this.pools.set(domain, new ConnectionPool({
        maxConnections: 10,
        idleTimeout: 300000, // 5 minutes
        connectionTimeout: 30000 // 30 seconds
      }));
    }
    
    return await this.pools.get(domain)!.acquire();
  }
}
```

### Message Batching

```typescript
async function batchMessagesToServer(
  targetServer: string,
  messages: Message[]
): Promise<void> {
  const BATCH_SIZE = 100;
  const batches = chunk(messages, BATCH_SIZE);
  
  for (const batch of batches) {
    await federationClient.deliverBatch(targetServer, batch);
  }
}
```

## Future Enhancements

1. **Gossip Protocol**: Servers share info about other servers
2. **Server Reputation**: Trust scores based on behavior
3. **Anonymous Routing**: Onion routing for metadata protection
4. **DHT-Based Discovery**: Distributed server discovery
5. **Multi-Hop Routing**: Route through intermediate servers
6. **Server Clusters**: Multiple servers per domain for HA

## Testing Federation

### Local Testing Setup

```bash
# Start 3 test servers
docker-compose up -d server1 server2 server3

# Configure domains
echo "127.0.0.1 server1.local" >> /etc/hosts
echo "127.0.0.1 server2.local" >> /etc/hosts
echo "127.0.0.1 server3.local" >> /etc/hosts

# Test federation
curl https://server1.local/.well-known/secure-mail
```

### Integration Tests

```typescript
describe('Federation', () => {
  it('should deliver message between servers', async () => {
    // Setup
    const server1 = await startTestServer('server1.local');
    const server2 = await startTestServer('server2.local');
    
    // Create users
    const alice = await server1.createUser('alice');
    const bob = await server2.createUser('bob');
    
    // Send message
    await alice.send('bob@server2.local', 'Hello!');
    
    // Verify delivery
    const messages = await bob.getMessages();
    expect(messages[0].content).toBe('Hello!');
  });
});
```

## Security Considerations

- **DDoS Protection**: Rate limiting, connection limits
- **Spam Prevention**: Reputation systems, proof-of-work
- **Server Impersonation**: TLS validation, DNSSEC
- **Traffic Analysis**: Padding, timing obfuscation
- **Censorship Resistance**: Domain fronting, bridges (future)
