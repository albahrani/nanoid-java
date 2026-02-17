# Discovery & Attestation - Secure Mail

## Overview

Discovery and attestation mechanisms allow clients to find servers, verify their authenticity, and establish trust. This ensures users connect to legitimate servers and prevents impersonation attacks.

## Goals

1. **Easy Discovery**: Simple process to find a server for a domain
2. **Authentic Servers**: Verify server identity and ownership
3. **Capability Advertisement**: Servers declare supported features
4. **Trust Establishment**: Build confidence in server security
5. **Decentralized**: No central authority required

## Discovery Methods

### 1. Well-Known URI (.well-known)

Primary discovery method using HTTPS:

```
https://example.com/.well-known/secure-mail
```

**Advantages:**
- Standard HTTPS infrastructure
- Easy to implement
- Certificate validation built-in
- No additional DNS records needed

**Response Format:**

```json
{
  "version": "1.0",
  "domain": "example.com",
  "serverUrl": "https://mail.example.com",
  "publicKey": "base64-ed25519-public-key",
  "capabilities": [
    "federation",
    "e2ee",
    "transparency-logs",
    "anti-enumeration",
    "tor-support"
  ],
  "endpoints": {
    "api": "https://mail.example.com/api/v1",
    "federation": "https://mail.example.com/federation/v1",
    "transparency": "https://transparency.example.com/v1"
  },
  "limits": {
    "maxMessageSize": 10485760,
    "maxAttachmentSize": 104857600,
    "rateLimit": {
      "messages": 1000,
      "window": 3600
    }
  },
  "supportedVersions": ["1.0", "1.1"],
  "termsOfService": "https://example.com/terms",
  "privacyPolicy": "https://example.com/privacy",
  "updatedAt": "2026-02-17T21:00:00Z",
  "signature": "base64-signature-of-above-data"
}
```

**Implementation:**

```typescript
async function discoverServer(domain: string): Promise<ServerInfo> {
  const url = `https://${domain}/.well-known/secure-mail`;
  
  try {
    const response = await fetch(url, {
      headers: {
        'User-Agent': 'SecureMail-Client/1.0'
      },
      // Verify TLS certificate
      agent: createHTTPSAgent({
        rejectUnauthorized: true,
        minVersion: 'TLSv1.3'
      })
    });
    
    if (!response.ok) {
      throw new Error(`Discovery failed: ${response.status}`);
    }
    
    const serverInfo = await response.json();
    
    // Verify signature
    await verifyServerInfoSignature(serverInfo);
    
    return serverInfo;
  } catch (error) {
    throw new Error(`Cannot discover server for ${domain}: ${error.message}`);
  }
}
```

### 2. DNS TXT Records (Fallback)

Alternative discovery via DNS:

```
_securemail.example.com TXT "v=sm1 url=https://mail.example.com"
```

**Advantages:**
- Works even if web server is down
- Standard DNS infrastructure
- Can include DNSSEC for validation

**Query:**

```typescript
async function discoverViaDNS(domain: string): Promise<string> {
  const records = await dns.resolveTxt(`_securemail.${domain}`);
  
  for (const record of records) {
    const text = record.join('');
    if (text.startsWith('v=sm1')) {
      const match = text.match(/url=([^\s]+)/);
      if (match) {
        return match[1];
      }
    }
  }
  
  throw new Error('No Secure Mail DNS record found');
}
```

### 3. SRV Records (Future)

For more detailed service information:

```
_securemail._tcp.example.com SRV 10 5 443 mail.example.com
```

## Server Attestation

### Certificate Validation

**TLS Certificate Requirements:**
- Valid domain certificate
- Not self-signed
- Not expired
- From trusted CA
- Matches domain being accessed

```typescript
function validateTLSCertificate(
  cert: Certificate,
  domain: string
): boolean {
  // Check expiration
  const now = Date.now();
  if (now < cert.validFrom || now > cert.validTo) {
    return false;
  }
  
  // Check domain match
  if (!cert.subjectAltNames.includes(domain)) {
    return false;
  }
  
  // Check issuer is trusted
  if (!isTrustedCA(cert.issuer)) {
    return false;
  }
  
  // Check not revoked (OCSP/CRL)
  if (await isCertificateRevoked(cert)) {
    return false;
  }
  
  return true;
}
```

### Server Identity Key

Each server has a long-term identity key:

```typescript
interface ServerIdentity {
  domain: string;
  publicKey: string; // Ed25519
  signature: string; // Self-signed or CA-signed
  validFrom: Date;
  validTo: Date;
}

async function verifyServerIdentity(
  serverInfo: ServerInfo
): Promise<boolean> {
  // Verify signature on serverInfo
  const data = JSON.stringify({
    domain: serverInfo.domain,
    serverUrl: serverInfo.serverUrl,
    publicKey: serverInfo.publicKey,
    // ... other fields
  });
  
  const valid = Ed25519.verify(
    serverInfo.publicKey,
    data,
    serverInfo.signature
  );
  
  if (!valid) {
    throw new Error('Invalid server signature');
  }
  
  // Check if key is in transparency log
  await verifyKeyInTransparencyLog(serverInfo);
  
  return true;
}
```

### Capability Verification

Clients verify advertised capabilities:

```typescript
async function verifyCapabilities(
  serverUrl: string,
  capabilities: string[]
): Promise<boolean> {
  const tests = {
    'federation': () => checkFederationEndpoint(serverUrl),
    'e2ee': () => checkE2EESupport(serverUrl),
    'transparency-logs': () => checkTransparencyLogs(serverUrl),
    'anti-enumeration': () => checkAntiEnumeration(serverUrl)
  };
  
  for (const capability of capabilities) {
    if (tests[capability]) {
      const supported = await tests[capability]();
      if (!supported) {
        console.warn(`Server claims ${capability} but it's not working`);
        return false;
      }
    }
  }
  
  return true;
}
```

## Trust Models

### Trust on First Use (TOFU)

Default for initial connection:

```typescript
class TOFUTrustStore {
  async verifyServer(serverInfo: ServerInfo): Promise<TrustLevel> {
    const stored = await this.getStoredServer(serverInfo.domain);
    
    if (!stored) {
      // First time seeing this server
      await this.storeServer(serverInfo);
      return TrustLevel.TOFU;
    }
    
    // Check if key changed
    if (stored.publicKey !== serverInfo.publicKey) {
      // KEY CHANGED - potential MITM!
      await this.alertUser({
        type: 'key_changed',
        domain: serverInfo.domain,
        oldKey: stored.publicKey,
        newKey: serverInfo.publicKey
      });
      return TrustLevel.UNTRUSTED;
    }
    
    return TrustLevel.VERIFIED;
  }
}
```

### Pinning

Pin server keys for high-security users:

```typescript
interface ServerPin {
  domain: string;
  publicKey: string;
  pinnedAt: Date;
  pinnedBy: 'user' | 'admin' | 'organization';
}

async function verifyPinnedServer(
  domain: string,
  serverInfo: ServerInfo
): Promise<boolean> {
  const pin = await getPinnedServer(domain);
  
  if (!pin) {
    return true; // Not pinned, allow
  }
  
  if (pin.publicKey !== serverInfo.publicKey) {
    throw new SecurityError(
      `Server key mismatch for ${domain}. ` +
      `Expected: ${pin.publicKey}, ` +
      `Got: ${serverInfo.publicKey}`
    );
  }
  
  return true;
}
```

### Certificate Transparency

Verify server keys are logged publicly:

```typescript
async function verifyKeyInTransparencyLog(
  serverInfo: ServerInfo
): Promise<boolean> {
  const logUrl = serverInfo.endpoints.transparency;
  
  // Fetch entry for this server
  const entry = await fetch(
    `${logUrl}/entries/${serverInfo.domain}`
  );
  
  if (!entry.ok) {
    throw new Error('Server key not in transparency log');
  }
  
  const logData = await entry.json();
  
  // Verify key matches
  if (logData.publicKey !== serverInfo.publicKey) {
    throw new Error('Key mismatch with transparency log');
  }
  
  // Verify inclusion proof
  const proof = await fetch(
    `${logUrl}/proof/${serverInfo.domain}`
  );
  const proofData = await proof.json();
  
  return verifyMerkleProof(proofData, logData);
}
```

## Server Reputation

### Community-Verified Servers

Shared list of known good servers:

```json
{
  "name": "Secure Mail Verified Servers",
  "version": 42,
  "updatedAt": "2026-02-17T21:00:00Z",
  "servers": [
    {
      "domain": "example.com",
      "publicKey": "base64-key",
      "verifiedBy": [
        "security-researcher-1",
        "security-researcher-2",
        "foundation-org"
      ],
      "verifiedAt": "2026-01-01T00:00:00Z",
      "reputation": "excellent",
      "notes": "Active security team, regular audits"
    }
  ]
}
```

### Reputation Tracking

Track server behavior over time:

```typescript
interface ServerReputation {
  domain: string;
  score: number; // 0-100
  metrics: {
    uptime: number;
    deliverySuccess: number;
    responseTime: number;
    securityIncidents: number;
  };
  lastUpdated: Date;
}

async function updateReputation(domain: string): Promise<void> {
  const metrics = await collectMetrics(domain);
  
  const score = calculateScore({
    uptime: metrics.uptime * 0.3,
    deliverySuccess: metrics.deliverySuccess * 0.4,
    responseTime: (1 - metrics.responseTime / 5000) * 0.2,
    securityIncidents: (1 - metrics.securityIncidents / 10) * 0.1
  });
  
  await updateServerReputation(domain, score, metrics);
}
```

## Client Discovery Process

### Full Discovery Flow

```typescript
async function connectToServer(domain: string): Promise<Connection> {
  // 1. Discover server
  let serverInfo: ServerInfo;
  try {
    serverInfo = await discoverViaWellKnown(domain);
  } catch (error) {
    // Fallback to DNS
    serverInfo = await discoverViaDNS(domain);
  }
  
  // 2. Verify TLS certificate
  await verifyTLSCertificate(serverInfo.serverUrl, domain);
  
  // 3. Verify server identity
  await verifyServerIdentity(serverInfo);
  
  // 4. Check pinning (if applicable)
  await verifyPinnedServer(domain, serverInfo);
  
  // 5. Verify in transparency log
  await verifyKeyInTransparencyLog(serverInfo);
  
  // 6. Check reputation (optional)
  const reputation = await getServerReputation(domain);
  if (reputation.score < 50) {
    await warnUser(`Server ${domain} has low reputation`);
  }
  
  // 7. Verify capabilities
  await verifyCapabilities(serverInfo.serverUrl, serverInfo.capabilities);
  
  // 8. Check TOFU
  const trustLevel = await verifyTOFU(serverInfo);
  
  // 9. Establish connection
  return await createConnection(serverInfo, trustLevel);
}
```

## Security Considerations

### Downgrade Attacks

Prevent attackers from forcing use of weak features:

```typescript
function negotiateVersion(
  clientVersions: string[],
  serverVersions: string[]
): string {
  // Only accept strong versions
  const acceptableVersions = ['1.1', '1.0'];
  
  const common = intersection(
    clientVersions,
    serverVersions,
    acceptableVersions
  );
  
  if (common.length === 0) {
    throw new Error('No acceptable protocol version');
  }
  
  // Use newest version
  return common.sort().reverse()[0];
}
```

### DNS Spoofing

Protect against DNS attacks:

- Use DNSSEC when available
- Verify TLS certificates match domain
- Cache validated server info
- Alert on sudden changes

```typescript
async function detectDNSSpoofing(
  domain: string,
  serverUrl: string
): Promise<boolean> {
  // Check if URL matches expected pattern
  const expectedPattern = new RegExp(
    `^https://([a-z0-9-]+\\.)?${domain}(/|$)`
  );
  
  if (!expectedPattern.test(serverUrl)) {
    await alertUser({
      type: 'suspicious_server_url',
      domain,
      serverUrl,
      message: 'Server URL does not match domain'
    });
    return true;
  }
  
  return false;
}
```

### Man-in-the-Middle

Multiple layers of protection:

1. **TLS**: Encrypted, authenticated transport
2. **Certificate Pinning**: Pin first-seen or admin-specified certs
3. **Transparency Logs**: Detect certificate misissuance
4. **TOFU**: Detect key changes
5. **E2EE**: Even server compromise doesn't reveal content

## Monitoring & Alerts

### Server Health Monitoring

Clients periodically check server health:

```typescript
async function monitorServer(domain: string): Promise<void> {
  setInterval(async () => {
    try {
      const health = await fetch(
        `https://${domain}/api/v1/health`,
        { timeout: 5000 }
      );
      
      if (!health.ok) {
        await alertUser({
          type: 'server_unhealthy',
          domain,
          status: health.status
        });
      }
    } catch (error) {
      await alertUser({
        type: 'server_unreachable',
        domain,
        error: error.message
      });
    }
  }, 300000); // Every 5 minutes
}
```

### Certificate Expiration

Warn before certificate expiration:

```typescript
async function checkCertificateExpiration(
  domain: string
): Promise<void> {
  const cert = await getTLSCertificate(domain);
  const daysUntilExpiry = (cert.validTo - Date.now()) / (1000 * 60 * 60 * 24);
  
  if (daysUntilExpiry < 7) {
    await warnUser(
      `Server certificate for ${domain} expires in ${daysUntilExpiry} days`
    );
  }
}
```

## User Experience

### First Connection

```
┌─────────────────────────────────────────┐
│  Connecting to example.com...           │
│                                          │
│  ✓ Server discovered                    │
│  ✓ Certificate validated                │
│  ✓ Server identity verified             │
│  ✓ Capabilities confirmed               │
│                                          │
│  This is your first time connecting     │
│  to this server.                        │
│                                          │
│  Server Key Fingerprint:                │
│  1234 5678 90AB CDEF ...                │
│                                          │
│  [ Trust ] [ Cancel ]                   │
└─────────────────────────────────────────┘
```

### Key Change Warning

```
┌─────────────────────────────────────────┐
│  ⚠️  WARNING: Server Key Changed        │
│                                          │
│  The server example.com has a different │
│  identity key than before.              │
│                                          │
│  This could indicate:                   │
│  • Server was migrated                  │
│  • Man-in-the-middle attack             │
│  • Server compromise                    │
│                                          │
│  Old Key: 1234 5678 ...                 │
│  New Key: ABCD EF01 ...                 │
│                                          │
│  [ Verify ] [ Cancel ] [ More Info ]    │
└─────────────────────────────────────────┘
```

## Testing

```typescript
describe('Server Discovery', () => {
  it('should discover server via .well-known', async () => {
    const serverInfo = await discoverServer('example.com');
    expect(serverInfo.domain).toBe('example.com');
    expect(serverInfo.serverUrl).toBeTruthy();
  });
  
  it('should verify server signature', async () => {
    const serverInfo = await discoverServer('example.com');
    const valid = await verifyServerIdentity(serverInfo);
    expect(valid).toBe(true);
  });
  
  it('should detect key changes', async () => {
    const serverInfo1 = await discoverServer('example.com');
    await storeServerInfo(serverInfo1);
    
    // Simulate key change
    const serverInfo2 = { ...serverInfo1, publicKey: 'different-key' };
    
    const trustLevel = await verifyTOFU(serverInfo2);
    expect(trustLevel).toBe(TrustLevel.UNTRUSTED);
  });
});
```

## Future Enhancements

1. **Onion Services**: Tor hidden service discovery
2. **Multi-Path Validation**: Verify server via multiple paths
3. **Notary Services**: Third-party attestation of server keys
4. **Automated Testing**: Regular security checks of servers
5. **Decentralized Reputation**: Blockchain-based reputation system
