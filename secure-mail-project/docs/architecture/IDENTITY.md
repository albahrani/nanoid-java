# Identity System - Secure Mail

## Overview

The identity system in Secure Mail provides cryptographic identity for users, ensuring message authenticity and enabling secure key exchange. It's inspired by the Signal Protocol and Certificate Transparency.

## Goals

1. **Strong Authentication**: Cryptographically verify sender identity
2. **Key Continuity**: Detect unauthorized key changes
3. **Device Support**: Multiple devices per user
4. **Recovery**: Secure account recovery mechanisms
5. **Auditability**: Public transparency logs for accountability

## Identity Model

### User Identity

Each user has one **Identity Key Pair**:
- **Private Key**: Kept secret on user's devices
- **Public Key**: Published on server and transparency logs

```
User: alice@example.com
  └─ Identity Key (Ed25519)
      ├─ Private: [secret, on devices]
      └─ Public: [published, in transparency log]
```

### Device Keys

Each device has:
- **Device Key Pair**: Signs messages from this device
- **Signed by Identity Key**: Links device to user

```
User: alice@example.com
  └─ Identity Key
      ├─ Device 1 (Desktop)
      │   ├─ Device Key
      │   └─ Signature (signed by Identity Key)
      ├─ Device 2 (Phone)
      │   ├─ Device Key
      │   └─ Signature (signed by Identity Key)
      └─ Device 3 (Tablet)
          ├─ Device Key
          └─ Signature (signed by Identity Key)
```

## Key Types

### Identity Key (IK)
- **Type**: Ed25519
- **Purpose**: Long-term user identity
- **Lifetime**: Until explicitly rotated (years)
- **Storage**: Encrypted on devices, backed up securely

### Signed Prekey (SPK)
- **Type**: X25519
- **Purpose**: Medium-term encryption key
- **Lifetime**: 30 days (rotated monthly)
- **Signed By**: Identity Key

### One-Time Prekeys (OPK)
- **Type**: X25519
- **Purpose**: Perfect forward secrecy
- **Lifetime**: Single use
- **Count**: 100 uploaded per device

## Key Generation

### Initial Registration

```typescript
// 1. Generate Identity Key
const identityKey = Ed25519.generateKeyPair();

// 2. Generate Signed Prekey
const signedPrekey = {
  keyPair: X25519.generateKeyPair(),
  signature: Ed25519.sign(identityKey.private, prekey.public),
  timestamp: Date.now(),
  id: randomId()
};

// 3. Generate One-Time Prekeys
const oneTimePrekeys = [];
for (let i = 0; i < 100; i++) {
  oneTimePrekeys.push({
    id: randomId(),
    keyPair: X25519.generateKeyPair()
  });
}

// 4. Upload to server
await server.registerUser({
  username: "alice",
  identityKey: identityKey.public,
  signedPrekey: {
    id: signedPrekey.id,
    key: signedPrekey.keyPair.public,
    signature: signedPrekey.signature
  },
  oneTimePrekeys: oneTimePrekeys.map(k => ({
    id: k.id,
    key: k.keyPair.public
  }))
});
```

### Adding a Device

```typescript
// On new device
const deviceKey = Ed25519.generateKeyPair();

// Sign with identity key (from existing device or recovery)
const signature = Ed25519.sign(identityKey.private, deviceKey.public);

// Upload device registration
await server.addDevice({
  deviceKey: deviceKey.public,
  signature: signature,
  signedPrekey: {...},
  oneTimePrekeys: [...]
});
```

## Identity Verification

### Safety Numbers

Users can verify each other's identity by comparing **Safety Numbers**:

```typescript
function computeSafetyNumber(
  userA: { address: string, identityKey: PublicKey },
  userB: { address: string, identityKey: PublicKey }
): string {
  // Deterministic order
  const [first, second] = [userA, userB].sort((a, b) => 
    a.address.localeCompare(b.address)
  );
  
  // Compute fingerprint
  const data = concat([
    first.address,
    first.identityKey,
    second.address,
    second.identityKey
  ]);
  
  const hash = SHA256(data);
  
  // Format as readable number (6 groups of 5 digits)
  return formatAsNumericCode(hash);
}
```

Example: `12345 67890 13579 24680 98765 43210`

### Verification Methods

1. **In-Person**: Compare safety numbers verbally
2. **QR Code**: Scan QR code containing identity key
3. **Video Call**: Show safety numbers on screen
4. **Out-of-Band**: Verify via different channel (phone, etc.)

### Trust Levels

- **Unverified**: Default, never verified
- **Verified**: Manually verified by user
- **Trusted**: Verified + consistent history

## Key Rotation

### Identity Key Rotation

Rare, only when:
- Key compromise suspected
- User requests rotation
- Recovery from backup

```typescript
async function rotateIdentityKey() {
  // 1. Generate new identity key
  const newIdentityKey = Ed25519.generateKeyPair();
  
  // 2. Sign new key with old key
  const signature = Ed25519.sign(
    oldIdentityKey.private,
    newIdentityKey.public
  );
  
  // 3. Upload rotation
  await server.rotateIdentityKey({
    newKey: newIdentityKey.public,
    signature: signature,
    timestamp: Date.now()
  });
  
  // 4. Logged in transparency log
  await transparencyLog.logRotation({
    address: "alice@example.com",
    oldKey: oldIdentityKey.public,
    newKey: newIdentityKey.public,
    signature: signature
  });
  
  // 5. Notify contacts
  await notifyContacts("Identity key rotated");
}
```

### Signed Prekey Rotation

Automatic, every 30 days:

```typescript
async function rotateSignedPrekey() {
  // Generate new signed prekey
  const newPrekey = {
    keyPair: X25519.generateKeyPair(),
    signature: Ed25519.sign(identityKey.private, prekey.public),
    timestamp: Date.now()
  };
  
  // Upload (old prekey kept for overlap period)
  await server.uploadSignedPrekey(newPrekey);
}
```

### One-Time Prekey Replenishment

Automatic when < 20 remaining:

```typescript
async function replenishOneTimePrekeys() {
  const remaining = await server.getPrekeyCount();
  
  if (remaining < 20) {
    const newKeys = generateOneTimePrekeys(100);
    await server.uploadOneTimePrekeys(newKeys);
  }
}
```

## Account Recovery

### Recovery Methods

1. **Recovery Codes**: Pre-generated codes
2. **Backup Devices**: Trusted devices can authorize new devices
3. **Social Recovery**: M-of-N trusted contacts (future)

### Recovery Code Flow

```typescript
// During registration
const recoveryCodes = generateRecoveryCodes(10);
displayToUser(recoveryCodes); // User must save these

// During recovery
async function recoverAccount(username, recoveryCode) {
  // Server verifies recovery code
  const verified = await server.verifyRecoveryCode(username, recoveryCode);
  
  if (verified) {
    // User can set new identity key
    const newIdentityKey = Ed25519.generateKeyPair();
    await server.recoverAccount(username, newIdentityKey.public);
  }
}
```

## Transparency Logs

### Logged Events

All identity operations are logged:
- User registration
- Identity key rotation
- Device addition/removal
- Key revocation

### Log Entry Format

```json
{
  "timestamp": "2026-02-17T21:00:00Z",
  "sequence": 12345,
  "address": "alice@example.com",
  "operation": "register|rotate|add-device|revoke",
  "identityKey": "base64-public-key",
  "previousKey": "base64-public-key-or-null",
  "signature": "base64-signature",
  "merkleTreeRoot": "base64-hash"
}
```

### Auditing

Clients periodically verify:
1. Their own identity in logs
2. Contacts' identity history
3. Merkle tree consistency

```typescript
async function auditIdentity(address: string) {
  // Fetch log entries for user
  const entries = await transparencyLog.getEntries(address);
  
  // Verify chain of trust
  for (let i = 1; i < entries.length; i++) {
    const valid = Ed25519.verify(
      entries[i-1].identityKey,
      entries[i].identityKey,
      entries[i].signature
    );
    
    if (!valid) {
      throw new Error("Untrusted key rotation detected!");
    }
  }
  
  // Verify merkle tree inclusion
  const proof = await transparencyLog.getProof(address, entries[0].sequence);
  const valid = verifyMerkleProof(proof, entries[0]);
  
  if (!valid) {
    throw new Error("Log tampering detected!");
  }
}
```

## Multi-Device Synchronization

### Device List Sync

Devices fetch peer device list periodically:

```typescript
async function syncDevices() {
  const devices = await server.getMyDevices();
  
  for (const device of devices) {
    if (!localDeviceList.has(device.id)) {
      // New device detected - verify signature
      const valid = Ed25519.verify(
        identityKey.public,
        device.key,
        device.signature
      );
      
      if (valid) {
        localDeviceList.add(device);
      } else {
        // Unauthorized device!
        await server.reportUnauthorizedDevice(device.id);
      }
    }
  }
}
```

### Message Sync

When user sends from Device A, Device B receives copy:

```typescript
// Device A sends message
await server.sendMessage({
  to: "bob@example.org",
  content: encryptedContent,
  syncToDevices: true
});

// Server delivers to bob@example.org
// AND sends encrypted copy to alice@example.com's other devices
```

## Privacy Considerations

### Metadata Minimization

- Identity keys don't contain personal info
- No email addresses in keys
- No timestamps unless necessary

### Anti-Enumeration

- Can't list all users
- Can't enumerate devices
- Key lookups rate-limited

### Deniability

- No non-repudiation (by design)
- Signatures only for authentication, not proof
- Forward secrecy prevents archival attacks

## Security Analysis

### Threat Model

**Protected Against:**
- ✓ Impersonation (identity keys)
- ✓ MITM (key verification)
- ✓ Key substitution (transparency logs)
- ✓ Compromise of old messages (forward secrecy)

**Not Protected Against:**
- ✗ Endpoint compromise (device has keys)
- ✗ Rubber-hose cryptanalysis (physical coercion)
- ✗ Social engineering (user verification required)

### Known Limitations

1. **Trust on First Use (TOFU)**: Initial key exchange not authenticated
   - Mitigation: Manual verification recommended

2. **Server can lie about key existence**: Server could claim user doesn't exist
   - Mitigation: Transparency logs + gossip protocol

3. **Device compromise**: Attacker with device access can read messages
   - Mitigation: Device encryption, biometric locks

## Implementation Notes

### Libraries

Recommended cryptographic libraries:
- **libsodium**: Complete crypto suite
- **@noble/ed25519**: Pure JS Ed25519
- **@noble/curves**: Pure JS X25519

### Key Storage

- **Desktop**: OS keychain (Keychain on macOS, Credential Manager on Windows, Secret Service on Linux)
- **Mobile**: Secure Enclave (iOS), Keystore (Android)
- **Web**: IndexedDB with Web Crypto API (limited)

### Performance

- Identity operations are infrequent (registration, verification)
- Prekey operations cached client-side
- Transparency log verification can be async/background

## Future Enhancements

1. **Post-Quantum Cryptography**: Add PQ key exchange
2. **Social Recovery**: M-of-N trusted contacts for recovery
3. **Hardware Keys**: Support for hardware security modules
4. **Revocation**: Faster key revocation propagation
5. **Cross-Signing**: Devices sign each other's keys
