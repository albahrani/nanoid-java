# Anti-Enumeration - Secure Mail

## Overview

Anti-enumeration protections prevent attackers from discovering which users exist on a server, protecting user privacy and preventing targeted attacks. This is a critical privacy feature that traditional email lacks.

## Threat Model

### Attacks We Prevent

1. **User Discovery**: Enumerating all users on a server
2. **Targeted Profiling**: Discovering if specific person uses service
3. **Social Graph Mapping**: Building relationships between users
4. **Presence Leakage**: Determining when users are online/offline
5. **Metadata Collection**: Gathering user activity patterns

### Attackers

- **Mass Scanners**: Automated tools scanning many servers
- **Targeted Adversaries**: Looking for specific individuals
- **Surveillance Entities**: Building databases of users
- **Malicious Servers**: Abusing federation to gather data

## Defense Strategies

### 1. No Public User Directory

**Problem**: User lists allow bulk enumeration

**Solution**: No endpoint that lists users

```typescript
// ❌ BAD - Allows enumeration
GET /api/v1/users
GET /api/v1/users/search?q=alice

// ✓ GOOD - No user listing
// Only direct lookups allowed
GET /api/v1/users/alice@example.com/info
```

### 2. Constant-Time User Existence Checks

**Problem**: Timing differences reveal if user exists

**Solution**: Same response time regardless of user existence

```typescript
async function checkUserExists(address: string): Promise<boolean> {
  // Always query database (no early returns)
  const user = await db.query(
    'SELECT EXISTS(SELECT 1 FROM users WHERE address = ?)',
    [address]
  );
  
  // Constant-time comparison
  const exists = user.length > 0;
  
  // Add timing jitter to hide database timing
  await randomDelay(5, 15); // 5-15ms
  
  return exists;
}

async function randomDelay(minMs: number, maxMs: number): Promise<void> {
  const delay = minMs + Math.random() * (maxMs - minMs);
  await new Promise(resolve => setTimeout(resolve, delay));
}
```

### 3. Identical Responses for Exists/Not Exists

**Problem**: Different error messages reveal existence

**Solution**: Same response format regardless

```typescript
async function getUserInfo(address: string): Promise<Response> {
  const user = await db.findUser(address);
  
  if (!user) {
    // Return fake data that looks real
    return {
      status: 404,
      body: {
        error: 'user_not_found_or_private',
        // Still return reasonable-looking timing
        timestamp: Date.now()
      }
    };
  }
  
  return {
    status: 404, // Same status code!
    body: {
      error: 'user_not_found_or_private',
      timestamp: Date.now()
    }
  };
}
```

Actually, better approach:

```typescript
async function getUserInfo(address: string): Promise<Response> {
  const user = await db.findUser(address);
  
  if (!user) {
    // Return synthetic user info
    return {
      status: 200,
      body: generateFakeUserInfo(address)
    };
  }
  
  return {
    status: 200,
    body: {
      address: user.address,
      identityKey: user.identityKey,
      devices: user.devices
    }
  };
}

function generateFakeUserInfo(address: string): UserInfo {
  // Deterministically generate fake data from address
  const seed = sha256(address + SERVER_SECRET);
  const rng = new SeededRandom(seed);
  
  return {
    address: address,
    identityKey: rng.generateFakeKey(),
    devices: [
      { deviceId: rng.uuid(), deviceKey: rng.generateFakeKey() }
    ]
  };
}
```

### 4. Rate Limiting

**Problem**: Bulk queries reveal many users

**Solution**: Aggressive rate limiting on user lookups

```typescript
const RATE_LIMITS = {
  userLookup: {
    perIP: {
      count: 100,
      window: 3600 // 1 hour
    },
    perUser: {
      count: 1000,
      window: 3600
    },
    global: {
      count: 10000,
      window: 3600
    }
  }
};

class RateLimiter {
  async checkUserLookup(ip: string, userId?: string): Promise<boolean> {
    // Check IP-based limit
    const ipKey = `ratelimit:lookup:ip:${ip}`;
    const ipCount = await redis.incr(ipKey);
    if (ipCount === 1) {
      await redis.expire(ipKey, RATE_LIMITS.userLookup.perIP.window);
    }
    if (ipCount > RATE_LIMITS.userLookup.perIP.count) {
      return false;
    }
    
    // Check user-based limit (if authenticated)
    if (userId) {
      const userKey = `ratelimit:lookup:user:${userId}`;
      const userCount = await redis.incr(userKey);
      if (userCount === 1) {
        await redis.expire(userKey, RATE_LIMITS.userLookup.perUser.window);
      }
      if (userCount > RATE_LIMITS.userLookup.perUser.count) {
        return false;
      }
    }
    
    return true;
  }
}
```

### 5. Honeypot Accounts

**Problem**: Attackers can test enumeration quietly

**Solution**: Fake accounts that trigger alerts

```typescript
const HONEYPOT_USERNAMES = [
  'admin',
  'test',
  'root',
  'demo',
  // ... more common names
];

async function checkHoneypot(address: string, ip: string): Promise<void> {
  const username = address.split('@')[0];
  
  if (HONEYPOT_USERNAMES.includes(username)) {
    // Log potential enumeration attempt
    await logger.warn('Honeypot triggered', {
      address,
      ip,
      timestamp: Date.now()
    });
    
    // Alert security team if threshold exceeded
    const attempts = await redis.incr(`honeypot:${ip}`);
    if (attempts > 5) {
      await alertSecurityTeam({
        ip,
        attempts,
        message: 'Possible enumeration attack'
      });
    }
    
    // Block IP after many attempts
    if (attempts > 20) {
      await blockIP(ip, 3600); // 1 hour block
    }
  }
}
```

### 6. Require Authentication for Discovery

**Problem**: Unauthenticated queries enable mass scanning

**Solution**: Require valid user account to look up others

```typescript
async function getUserInfo(
  address: string,
  requester: AuthenticatedUser
): Promise<UserInfo> {
  // Only authenticated users can look up others
  if (!requester) {
    throw new UnauthorizedError('Authentication required');
  }
  
  // Log all lookups for audit
  await auditLog.record({
    action: 'user_lookup',
    requester: requester.address,
    target: address,
    timestamp: Date.now()
  });
  
  // Check if requester is abusing lookups
  const lookupCount = await getLookupCount(requester.address, 3600);
  if (lookupCount > 100) {
    throw new RateLimitError('Too many lookups');
  }
  
  return await fetchUserInfo(address);
}
```

### 7. Federation Request Authentication

**Problem**: Malicious servers can enumerate via federation

**Solution**: Mutual authentication and rate limiting

```typescript
async function federationUserLookup(
  request: FederationRequest
): Promise<UserInfo> {
  // Verify requesting server's identity
  const sourceServer = await verifyServerIdentity(request);
  
  // Rate limit per server
  const lookupKey = `federation:lookup:${sourceServer.domain}`;
  const count = await redis.incr(lookupKey);
  if (count === 1) {
    await redis.expire(lookupKey, 3600);
  }
  if (count > 1000) { // 1000 lookups per hour per server
    throw new RateLimitError('Federation rate limit exceeded');
  }
  
  // Require proof of message delivery intent
  if (!request.hasValidMessageIntent) {
    throw new Error('User lookups require message intent');
  }
  
  return await fetchUserInfo(request.targetAddress);
}
```

## Message Send Protection

### 8. Blind Message Acceptance

**Problem**: Server confirms delivery, revealing user existence

**Solution**: Accept all messages, queue for valid users

```typescript
async function acceptMessage(message: Message): Promise<Response> {
  const targetUser = await db.findUser(message.to);
  
  if (!targetUser) {
    // Still accept the message (don't reveal non-existence)
    await blackHoleQueue.add(message);
    
    // Return success
    return {
      status: 202,
      messageId: message.messageId,
      body: { status: 'accepted' }
    };
  }
  
  // Queue for real user
  await deliveryQueue.add(message);
  
  // Same response
  return {
    status: 202,
    messageId: message.messageId,
    body: { status: 'accepted' }
  };
}

// Black hole queue - silently discards after delay
class BlackHoleQueue {
  async add(message: Message): Promise<void> {
    // Keep message for a realistic time
    const delay = 24 * 60 * 60; // 24 hours
    
    await redis.setex(
      `blackhole:${message.messageId}`,
      delay,
      JSON.stringify(message)
    );
    
    // Automatically discarded after TTL
  }
}
```

### 9. No Read Receipts by Default

**Problem**: Read receipts leak presence information

**Solution**: Opt-in only, with warnings

```typescript
interface MessageReceipt {
  messageId: string;
  status: 'delivered' | 'read';
  timestamp: Date;
}

async function sendReadReceipt(
  messageId: string,
  reader: User
): Promise<void> {
  // Check if user has read receipts enabled
  if (!reader.settings.enableReadReceipts) {
    return; // Silent no-op
  }
  
  // Warn user about privacy implications
  if (!reader.settings.acknowledgedReadReceiptWarning) {
    await showWarning(
      'Read receipts reveal when you read messages. This may leak presence information.'
    );
  }
  
  // Send receipt
  await sendReceipt({
    messageId,
    status: 'read',
    timestamp: Date.now()
  });
}
```

## Registration Protection

### 10. CAPTCHA for Registration

**Problem**: Automated account creation for enumeration

**Solution**: CAPTCHA or proof-of-work

```typescript
async function register(
  username: string,
  password: string,
  captchaToken: string
): Promise<User> {
  // Verify CAPTCHA
  const captchaValid = await verifyCaptcha(captchaToken);
  if (!captchaValid) {
    throw new Error('Invalid CAPTCHA');
  }
  
  // Check username not on reserved/honeypot list
  if (isReservedUsername(username)) {
    throw new Error('Username not available');
  }
  
  // Create account
  return await createUser(username, password);
}
```

### 11. Invitation System (Optional)

**Problem**: Open registration enables enumeration accounts

**Solution**: Require invitation from existing user

```typescript
async function registerWithInvite(
  username: string,
  password: string,
  inviteCode: string
): Promise<User> {
  // Verify invite code
  const invite = await db.findInvite(inviteCode);
  if (!invite || invite.used) {
    throw new Error('Invalid or used invite code');
  }
  
  // Create account
  const user = await createUser(username, password);
  
  // Mark invite as used
  await db.markInviteUsed(inviteCode, user.id);
  
  // Give new user limited invites
  await createInvites(user.id, 3);
  
  return user;
}
```

## Monitoring & Detection

### 12. Enumeration Detection

```typescript
class EnumerationDetector {
  async detectEnumeration(events: AuditEvent[]): Promise<Alert[]> {
    const alerts: Alert[] = [];
    
    // Group by IP
    const byIP = groupBy(events, e => e.ip);
    
    for (const [ip, ipEvents] of Object.entries(byIP)) {
      // Check for sequential username patterns
      const usernames = ipEvents.map(e => e.targetUsername);
      if (this.isSequentialPattern(usernames)) {
        alerts.push({
          type: 'sequential_enumeration',
          ip,
          severity: 'high',
          usernames: usernames.slice(0, 10) // Sample
        });
      }
      
      // Check for dictionary attacks
      if (this.isDictionaryPattern(usernames)) {
        alerts.push({
          type: 'dictionary_enumeration',
          ip,
          severity: 'high'
        });
      }
      
      // Check for high volume
      if (ipEvents.length > 1000) {
        alerts.push({
          type: 'volume_enumeration',
          ip,
          count: ipEvents.length,
          severity: 'critical'
        });
      }
    }
    
    return alerts;
  }
  
  private isSequentialPattern(usernames: string[]): boolean {
    // Check for user1, user2, user3 patterns
    const numberPattern = /^(.+?)(\d+)$/;
    const numbers: number[] = [];
    
    for (const username of usernames) {
      const match = username.match(numberPattern);
      if (match) {
        numbers.push(parseInt(match[2]));
      }
    }
    
    // Check if numbers are sequential
    if (numbers.length < 5) return false;
    
    let sequential = 0;
    for (let i = 1; i < numbers.length; i++) {
      if (numbers[i] === numbers[i-1] + 1) {
        sequential++;
      }
    }
    
    return sequential / numbers.length > 0.8;
  }
}
```

### 13. Automated Response

```typescript
async function handleEnumerationAlert(alert: Alert): Promise<void> {
  switch (alert.severity) {
    case 'critical':
      // Immediate block
      await blockIP(alert.ip, 86400); // 24 hours
      await notifySecurityTeam(alert);
      break;
      
    case 'high':
      // Temporary block + monitoring
      await blockIP(alert.ip, 3600); // 1 hour
      await addToWatchlist(alert.ip);
      break;
      
    case 'medium':
      // Add to watchlist
      await addToWatchlist(alert.ip);
      break;
  }
  
  // Log for forensics
  await auditLog.record({
    type: 'enumeration_detected',
    alert,
    action: 'auto_response',
    timestamp: Date.now()
  });
}
```

## Privacy-Preserving Features

### 14. No "User Online" Status

```typescript
// ❌ BAD - Leaks presence
interface UserStatus {
  online: boolean;
  lastSeen: Date;
}

// ✓ GOOD - No presence information
interface UserInfo {
  address: string;
  identityKey: string;
  // No online/lastSeen fields
}
```

### 15. Message Padding

Hide message size patterns:

```typescript
function padMessage(content: string): Uint8Array {
  const plaintext = new TextEncoder().encode(content);
  
  // Pad to next multiple of 1KB
  const paddedLength = Math.ceil(plaintext.length / 1024) * 1024;
  const padded = new Uint8Array(paddedLength);
  
  padded.set(plaintext);
  // Fill rest with random data
  crypto.getRandomValues(padded.subarray(plaintext.length));
  
  return padded;
}
```

### 16. Timing Obfuscation

Add random delays to hide patterns:

```typescript
async function sendMessage(message: Message): Promise<void> {
  // Random delay 0-500ms
  await sleep(Math.random() * 500);
  
  await actualSend(message);
}
```

## Testing Anti-Enumeration

```typescript
describe('Anti-Enumeration', () => {
  it('should not reveal user existence via timing', async () => {
    const timings: number[] = [];
    
    // Time 100 requests for existing users
    for (let i = 0; i < 100; i++) {
      const start = Date.now();
      await getUserInfo('existing_user@example.com');
      timings.push(Date.now() - start);
    }
    const existingAvg = average(timings);
    
    timings.length = 0;
    
    // Time 100 requests for non-existing users
    for (let i = 0; i < 100; i++) {
      const start = Date.now();
      await getUserInfo('nonexisting_user@example.com');
      timings.push(Date.now() - start);
    }
    const nonExistingAvg = average(timings);
    
    // Should be within 10% of each other
    expect(Math.abs(existingAvg - nonExistingAvg) / existingAvg).toBeLessThan(0.1);
  });
  
  it('should trigger honeypot on common usernames', async () => {
    const alerts: Alert[] = [];
    
    for (const username of ['admin', 'test', 'root']) {
      await getUserInfo(`${username}@example.com`);
    }
    
    expect(alerts.length).toBeGreaterThan(0);
  });
});
```

## Trade-offs

### Usability vs. Privacy

| Feature | Privacy Benefit | Usability Cost |
|---------|----------------|----------------|
| No user search | High | Medium (users must know exact address) |
| Blind acceptance | High | Low (transparent to users) |
| Authentication required | Medium | Low (already required) |
| Invitation-only | High | High (limits growth) |
| No read receipts | Medium | Medium (users expect them) |

### Recommendations

- **Default**: Authentication required, rate limiting, honeypots
- **High Privacy Mode**: Add invitation-only, fake responses
- **Opt-in Features**: Read receipts, "typing" indicators (with warnings)

## Comparison to Other Systems

| System | User Enumeration | Presence Leakage | Metadata Protection |
|--------|-----------------|------------------|---------------------|
| Email | ✗ (SMTP VRFY) | ✗ | ✗ |
| Signal | ✓ | ✓ | ✓ |
| WhatsApp | Partial (phone numbers) | ✗ | Partial |
| Matrix | ✗ | ✗ | Partial |
| Secure Mail | ✓ | ✓ | ✓ |

## Future Improvements

1. **Private Information Retrieval**: Fetch keys without revealing which key
2. **Dummy Traffic**: Generate fake traffic to hide real patterns
3. **Tor Integration**: Hide IP addresses completely
4. **Anonymous Credentials**: Prove authorization without identity
5. **Differential Privacy**: Add noise to query responses
