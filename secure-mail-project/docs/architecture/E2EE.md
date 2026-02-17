# End-to-End Encryption (E2EE) - Secure Mail

## Overview

Secure Mail uses the Signal Protocol for end-to-end encryption, ensuring that only the sender and recipient can read message contents. Servers cannot decrypt messages, providing strong confidentiality guarantees.

## Goals

1. **Confidentiality**: Only sender and recipient read messages
2. **Forward Secrecy**: Past messages secure if keys compromised
3. **Future Secrecy**: Self-healing after key compromise
4. **Authentication**: Verify message sender
5. **Deniability**: No cryptographic proof of authorship

## Protocol Choice: Signal Protocol

We use the Signal Protocol (formerly Axolotl/TextSecure) because:
- ✓ Battle-tested (billions of users)
- ✓ Forward and future secrecy
- ✓ Async messaging support
- ✓ Well-documented and audited
- ✓ Multiple implementations available

## Key Components

### 1. X3DH (Extended Triple Diffie-Hellman)

Initial key agreement when starting a conversation:

```
Alice                                                Bob
---------                                            ---------
Identity Key IK_A                                    Identity Key IK_B
Ephemeral Key EK_A                                   Signed Prekey SPK_B
                                                     One-Time Prekey OPK_B

Fetches Bob's keys from server
  ← IK_B, SPK_B, OPK_B

Computes shared secret:
  DH1 = DH(IK_A, SPK_B)
  DH2 = DH(EK_A, IK_B)
  DH3 = DH(EK_A, SPK_B)
  DH4 = DH(EK_A, OPK_B)
  
  SK = KDF(DH1 || DH2 || DH3 || DH4)

Sends initial message with EK_A
  → EK_A, encrypted(message)

                                                     Computes same shared secret
                                                     Decrypts message
```

### 2. Double Ratchet

Ongoing message encryption with forward/future secrecy:

```
┌─────────────────────────────────────────────────┐
│               Double Ratchet                     │
├─────────────────────────────────────────────────┤
│                                                  │
│  Symmetric Ratchet (per message):               │
│    Chain Key → Chain Key' → Chain Key''         │
│         ↓           ↓              ↓             │
│    Message Key  Message Key   Message Key       │
│                                                  │
│  DH Ratchet (per round trip):                   │
│    DH(A₁,B₁) → DH(A₂,B₁) → DH(A₂,B₂) → ...     │
│                                                  │
└─────────────────────────────────────────────────┘
```

## Encryption Flow

### First Message (Alice → Bob)

```typescript
// 1. Fetch Bob's prekey bundle
const bobBundle = await server.getPrekeyBundle('bob@example.org');

// 2. Perform X3DH
const x3dh = new X3DH();
const sharedSecret = x3dh.calculateSecret(
  aliceIdentityKey,
  aliceEphemeralKey,
  bobBundle
);

// 3. Initialize ratchet
const ratchet = new DoubleRatchet(sharedSecret);

// 4. Encrypt message
const { ciphertext, messageKey } = ratchet.encrypt('Hello, Bob!');

// 5. Create message envelope
const envelope = {
  version: '1.0',
  from: 'alice@example.com',
  to: 'bob@example.org',
  type: 'prekey_message',
  ephemeralKey: base64(aliceEphemeralKey.public),
  prekeyId: bobBundle.prekeyId,
  encryptedContent: base64(ciphertext),
  signature: sign(aliceIdentityKey, ciphertext)
};

// 6. Send to server
await server.send(envelope);
```

### Subsequent Messages (Alice → Bob)

```typescript
// 1. Get existing ratchet state
const ratchet = await getRatchetState('bob@example.org');

// 2. Encrypt message
const { ciphertext, header } = ratchet.encrypt('Another message');

// 3. Create message envelope
const envelope = {
  version: '1.0',
  from: 'alice@example.com',
  to: 'bob@example.org',
  type: 'message',
  ratchetHeader: header,
  encryptedContent: base64(ciphertext)
};

// 4. Send to server
await server.send(envelope);

// 5. Update ratchet state
await saveRatchetState('bob@example.org', ratchet);
```

### Message Decryption (Bob)

```typescript
// 1. Receive message from server
const envelope = await server.receive();

if (envelope.type === 'prekey_message') {
  // First message - perform X3DH
  const x3dh = new X3DH();
  const sharedSecret = x3dh.calculateSecret(
    bobIdentityKey,
    envelope.ephemeralKey,
    bobPrekeyPrivate,
    bobOneTimePrekeyPrivate
  );
  
  // Initialize ratchet
  const ratchet = new DoubleRatchet(sharedSecret);
  
  // Decrypt
  const plaintext = ratchet.decrypt(envelope.encryptedContent);
  
  // Save ratchet state
  await saveRatchetState(envelope.from, ratchet);
  
  // Delete used one-time prekey
  await deleteOneTimePrekey(envelope.prekeyId);
  
} else {
  // Subsequent message
  const ratchet = await getRatchetState(envelope.from);
  const plaintext = ratchet.decrypt(
    envelope.encryptedContent,
    envelope.ratchetHeader
  );
  
  await saveRatchetState(envelope.from, ratchet);
}
```

## Message Format

### Prekey Message (First Message)

```json
{
  "version": "1.0",
  "type": "prekey_message",
  "messageId": "uuid",
  "from": "alice@example.com",
  "to": "bob@example.org",
  "timestamp": "2026-02-17T21:00:00Z",
  "ephemeralKey": "base64-x25519-public",
  "identityKey": "base64-ed25519-public",
  "prekeyId": "uuid",
  "oneTimePrekeyId": "uuid",
  "encryptedContent": "base64-ciphertext",
  "signature": "base64-signature"
}
```

### Regular Message

```json
{
  "version": "1.0",
  "type": "message",
  "messageId": "uuid",
  "from": "alice@example.com",
  "to": "bob@example.org",
  "timestamp": "2026-02-17T21:00:00Z",
  "ratchetHeader": {
    "dhPublicKey": "base64-x25519-public",
    "previousChainLength": 42,
    "messageNumber": 7
  },
  "encryptedContent": "base64-ciphertext"
}
```

### Encrypted Content (After Decryption)

```json
{
  "contentType": "text/plain",
  "content": "Hello, Bob!",
  "timestamp": "2026-02-17T21:00:00Z",
  "threadId": "uuid",
  "padding": "random-bytes-for-length-hiding"
}
```

## Group Messages (Future)

### Sender Keys Protocol

For efficient group encryption:

```typescript
class GroupSession {
  private senderKey: Uint8Array;
  private chainKey: Uint8Array;
  private generation: number = 0;
  
  async encryptForGroup(
    message: string,
    groupMembers: string[]
  ): Promise<GroupMessage> {
    // Encrypt message with sender key
    const { ciphertext, messageKey } = this.encryptWithChainKey(message);
    
    // Distribute sender key to new members
    for (const member of newMembers) {
      await this.sendSenderKey(member);
    }
    
    return {
      senderId: this.userId,
      generation: this.generation,
      ciphertext,
      signature: this.sign(ciphertext)
    };
  }
  
  async sendSenderKey(recipient: string): Promise<void> {
    // Encrypt sender key with recipient's 1:1 session
    const session = await this.getSession(recipient);
    const encryptedKey = session.encrypt({
      senderKey: this.senderKey,
      chainKey: this.chainKey,
      generation: this.generation
    });
    
    await server.send(recipient, encryptedKey);
  }
}
```

## Key Management

### Key Storage

```typescript
interface RatchetState {
  rootKey: Uint8Array;
  sendingChainKey: Uint8Array;
  receivingChainKey: Uint8Array;
  dhSendingKey: KeyPair;
  dhReceivingKey: PublicKey;
  previousChainLength: number;
  messageNumber: number;
  skippedMessages: Map<string, Uint8Array>;
}

class SecureStorage {
  async saveRatchetState(
    conversationId: string,
    state: RatchetState
  ): Promise<void> {
    // Encrypt state with device key before storing
    const encrypted = await this.encrypt(JSON.stringify(state));
    await db.put(`ratchet:${conversationId}`, encrypted);
  }
  
  async getRatchetState(
    conversationId: string
  ): Promise<RatchetState | null> {
    const encrypted = await db.get(`ratchet:${conversationId}`);
    if (!encrypted) return null;
    
    const decrypted = await this.decrypt(encrypted);
    return JSON.parse(decrypted);
  }
}
```

### Out-of-Order Messages

Handle messages arriving out of order:

```typescript
class DoubleRatchet {
  private skippedMessages = new Map<string, Uint8Array>();
  
  async decrypt(
    ciphertext: Uint8Array,
    header: RatchetHeader
  ): Promise<string> {
    const key = `${header.dhPublicKey}:${header.messageNumber}`;
    
    // Check if we skipped this message before
    if (this.skippedMessages.has(key)) {
      const messageKey = this.skippedMessages.get(key)!;
      this.skippedMessages.delete(key);
      return this.decryptWithKey(ciphertext, messageKey);
    }
    
    // Catch up to current message
    while (this.currentMessageNumber < header.messageNumber) {
      const skippedKey = this.advanceChain();
      const skippedKeyId = `${header.dhPublicKey}:${this.currentMessageNumber}`;
      this.skippedMessages.set(skippedKeyId, skippedKey);
    }
    
    // Decrypt current message
    const messageKey = this.advanceChain();
    return this.decryptWithKey(ciphertext, messageKey);
  }
}
```

## Security Properties

### Forward Secrecy

Compromise of current keys doesn't reveal past messages:

```
Time:     t₀────t₁────t₂────t₃────→
Keys:     k₀────k₁────k₂────k₃────→
Messages: m₀────m₁────m₂────m₃────→

Attacker gets k₃:
  ✓ Can decrypt: m₃, future messages
  ✗ Cannot decrypt: m₀, m₁, m₂
```

### Future Secrecy (Self-Healing)

After key compromise, security restored after key exchange:

```
Time:     t₀────t₁────t₂────t₃────t₄────→
Keys:     k₀────k₁────k₂────k₃────k₄────→
                      ↑
                  Compromise
                      
After DH ratchet at t₄:
  ✗ Cannot decrypt future messages (k₄ independent of k₂)
```

### Authentication

Every message authenticated:
- Identity signature on prekey messages
- MAC on all encrypted messages
- Prevents impersonation and tampering

### Deniability

No non-repudiation:
- Message MACs use symmetric keys
- Both parties could have created message
- Provides authentication without proof

## Performance Considerations

### Optimization Strategies

1. **Prekey Batching**: Fetch multiple prekeys at once
2. **State Caching**: Keep ratchet states in memory
3. **Parallel Encryption**: Encrypt messages in parallel
4. **Hardware Acceleration**: Use CPU crypto instructions

```typescript
class PerformanceOptimizedRatchet {
  private stateCache = new LRUCache<string, RatchetState>(100);
  
  async encrypt(messages: Message[]): Promise<EncryptedMessage[]> {
    // Parallel encryption
    return await Promise.all(
      messages.map(msg => this.encryptSingle(msg))
    );
  }
  
  async encryptSingle(message: Message): Promise<EncryptedMessage> {
    // Use cached state
    let state = this.stateCache.get(message.recipient);
    if (!state) {
      state = await this.loadRatchetState(message.recipient);
      this.stateCache.set(message.recipient, state);
    }
    
    // Encrypt
    const result = state.encrypt(message.content);
    
    // Update cache
    this.stateCache.set(message.recipient, state);
    
    return result;
  }
}
```

### Benchmarks

Target performance on modern hardware:
- X3DH key agreement: < 10ms
- Message encryption: < 1ms
- Message decryption: < 1ms
- State serialization: < 5ms

## Testing

### Unit Tests

```typescript
describe('Signal Protocol', () => {
  it('should encrypt and decrypt messages', async () => {
    const alice = new SignalClient('alice');
    const bob = new SignalClient('bob');
    
    // Setup
    await alice.generateIdentityKey();
    await bob.generateIdentityKey();
    await bob.generatePrekeys();
    
    // Send message
    const encrypted = await alice.encrypt('bob', 'Hello!');
    const decrypted = await bob.decrypt('alice', encrypted);
    
    expect(decrypted).toBe('Hello!');
  });
  
  it('should provide forward secrecy', async () => {
    // ... test implementation
  });
});
```

### Security Testing

1. **Key Material Wiping**: Verify keys erased from memory
2. **Side-Channel Resistance**: Timing attack prevention
3. **Cryptographic Correctness**: Test vectors from Signal spec
4. **State Machine**: Verify ratchet state transitions

## Implementation Recommendations

### Libraries

- **@signalapp/libsignal-client**: Official JavaScript library
- **libsignal-protocol-java**: Java implementation
- **libsignal-protocol-c**: C implementation (for native)

### Best Practices

1. **Never Reuse Keys**: Each message uses unique key
2. **Wipe Sensitive Data**: Clear keys from memory
3. **Validate Input**: Check all cryptographic inputs
4. **Handle Errors**: Graceful degradation, never silent failures
5. **Audit Regularly**: External security audits

## Migration & Compatibility

### Version Support

- Support multiple protocol versions
- Graceful degradation for older clients
- Migration path for protocol updates

```typescript
async function negotiateVersion(
  clientVersion: string,
  serverVersions: string[]
): Promise<string> {
  const commonVersions = intersection(
    [clientVersion],
    serverVersions
  );
  
  if (commonVersions.length === 0) {
    throw new Error('No compatible protocol version');
  }
  
  return commonVersions[0];
}
```

## Future Enhancements

1. **Post-Quantum Cryptography**: Add PQ key exchange
2. **Multiparty Sessions**: Optimize group encryption
3. **Offline Encryption**: Encrypt without fetching prekeys
4. **Key Transparency**: Verify key distribution
5. **Abuse-Resistant Sender Keys**: Revoke group members efficiently
