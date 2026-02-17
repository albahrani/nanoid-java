# Transparency Logs - Secure Mail

## Overview

Transparency logs provide cryptographic auditability for identity operations, allowing detection of unauthorized key changes, server misbehavior, and identity theft. Based on Certificate Transparency and Key Transparency concepts.

## Goals

1. **Detect Key Substitution**: Catch servers lying about user keys
2. **Audit Trail**: Complete history of identity operations
3. **Public Verifiability**: Anyone can audit the logs
4. **Tamper-Evident**: Cannot modify history without detection
5. **Efficient Verification**: Fast proofs of inclusion/consistency

## Design

### Merkle Tree Structure

Logs use a Merkle tree for efficient cryptographic proofs:

```
                    Root Hash
                   /         \
                  /           \
               Hash1          Hash2
              /    \          /    \
           Hash3  Hash4   Hash5   Hash6
           /  \    / \     / \     / \
          E1  E2  E3 E4   E5 E6   E7 E8
          
E1-E8 = Log entries
```

**Properties:**
- Append-only (new entries added to right)
- Tamper-evident (changing entry changes root)
- Efficient proofs (log N proof size)
- Consistent (can prove tree grew correctly)

### Log Entry Format

```typescript
interface LogEntry {
  // Entry metadata
  sequence: number;
  timestamp: string;
  
  // Identity operation
  operation: 'register' | 'rotate' | 'add-device' | 'revoke';
  address: string;
  domain: string;
  
  // Cryptographic data
  identityKey: string; // Current key
  previousKey?: string; // Previous key (for rotations)
  signature: string; // Signature with previous key
  
  // Merkle tree position
  merkleTreeHash: string; // Hash of this entry
  
  // Server signature
  serverSignature: string;
}
```

### Signed Tree Head (STH)

Periodic snapshots of tree state:

```typescript
interface SignedTreeHead {
  // Tree state
  treeSize: number;
  timestamp: string;
  rootHash: string;
  
  // Server info
  serverDomain: string;
  logId: string;
  
  // Signature
  signature: string; // Signed by server key
}
```

## Operations

### 1. Append Entry

When user performs identity operation:

```typescript
async function appendToLog(
  operation: IdentityOperation
): Promise<LogEntry> {
  // Create entry
  const entry: LogEntry = {
    sequence: await getNextSequence(),
    timestamp: new Date().toISOString(),
    operation: operation.type,
    address: operation.address,
    domain: operation.domain,
    identityKey: operation.identityKey,
    previousKey: operation.previousKey,
    signature: operation.signature,
    merkleTreeHash: '', // Computed below
    serverSignature: ''
  };
  
  // Compute entry hash
  entry.merkleTreeHash = sha256(JSON.stringify({
    sequence: entry.sequence,
    timestamp: entry.timestamp,
    operation: entry.operation,
    address: entry.address,
    identityKey: entry.identityKey,
    previousKey: entry.previousKey
  }));
  
  // Sign entry with server key
  entry.serverSignature = await signWithServerKey(
    entry.merkleTreeHash
  );
  
  // Append to tree
  await merkleTree.append(entry.merkleTreeHash);
  
  // Store entry
  await db.insertLogEntry(entry);
  
  // Update STH periodically (every N entries or M time)
  if (shouldUpdateSTH()) {
    await publishSignedTreeHead();
  }
  
  return entry;
}
```

### 2. Verify Inclusion

Prove an entry is in the log:

```typescript
interface InclusionProof {
  leafIndex: number;
  treeSize: number;
  path: string[]; // Merkle path from leaf to root
  sth: SignedTreeHead;
}

async function proveInclusion(
  entry: LogEntry
): Promise<InclusionProof> {
  const leafIndex = entry.sequence - 1;
  const treeSize = await merkleTree.getSize();
  
  // Get Merkle path
  const path = await merkleTree.getPath(leafIndex);
  
  // Get current STH
  const sth = await getLatestSTH();
  
  return {
    leafIndex,
    treeSize,
    path,
    sth
  };
}

async function verifyInclusion(
  entry: LogEntry,
  proof: InclusionProof
): Promise<boolean> {
  // Verify STH signature
  if (!await verifySTHSignature(proof.sth)) {
    return false;
  }
  
  // Compute root from proof
  let hash = entry.merkleTreeHash;
  let index = proof.leafIndex;
  
  for (const sibling of proof.path) {
    if (index % 2 === 0) {
      hash = sha256(hash + sibling);
    } else {
      hash = sha256(sibling + hash);
    }
    index = Math.floor(index / 2);
  }
  
  // Check computed root matches STH
  return hash === proof.sth.rootHash;
}
```

### 3. Verify Consistency

Prove tree grew correctly (no history modification):

```typescript
interface ConsistencyProof {
  oldSize: number;
  newSize: number;
  path: string[];
  oldSTH: SignedTreeHead;
  newSTH: SignedTreeHead;
}

async function verifyConsistency(
  proof: ConsistencyProof
): Promise<boolean> {
  // Verify both STH signatures
  if (!await verifySTHSignature(proof.oldSTH)) return false;
  if (!await verifySTHSignature(proof.newSTH)) return false;
  
  // Verify proof path
  const oldRoot = proof.oldSTH.rootHash;
  const newRoot = proof.newSTH.rootHash;
  
  return verifyConsistencyPath(
    proof.oldSize,
    proof.newSize,
    oldRoot,
    newRoot,
    proof.path
  );
}
```

## Client Operations

### Monitoring Own Identity

Clients periodically check their identity in logs:

```typescript
async function auditMyIdentity(address: string): Promise<void> {
  // Fetch all log entries for this address
  const entries = await transparencyLog.getEntries(address);
  
  if (entries.length === 0) {
    throw new Error('Identity not found in transparency log!');
  }
  
  // Verify chain of trust
  for (let i = 1; i < entries.length; i++) {
    const prev = entries[i - 1];
    const curr = entries[i];
    
    // Current operation should be signed by previous key
    const valid = await Ed25519.verify(
      prev.identityKey,
      curr.identityKey,
      curr.signature
    );
    
    if (!valid) {
      throw new SecurityError(
        `Unauthorized key rotation detected!\n` +
        `At sequence ${curr.sequence}\n` +
        `Previous key: ${prev.identityKey}\n` +
        `New key: ${curr.identityKey}`
      );
    }
  }
  
  // Verify inclusion in latest tree
  for (const entry of entries) {
    const proof = await transparencyLog.getInclusionProof(
      entry.sequence
    );
    
    const valid = await verifyInclusion(entry, proof);
    if (!valid) {
      throw new Error(`Entry ${entry.sequence} not in tree!`);
    }
  }
  
  console.log('✓ Identity audit passed');
}
```

### Monitoring Contacts

Check contacts' key history:

```typescript
async function auditContactIdentity(
  address: string
): Promise<AuditResult> {
  const entries = await transparencyLog.getEntries(address);
  
  const issues: SecurityIssue[] = [];
  
  // Check for suspicious key rotations
  const rotations = entries.filter(e => e.operation === 'rotate');
  
  for (const rotation of rotations) {
    const timeSinceRegistration = 
      new Date(rotation.timestamp) - 
      new Date(entries[0].timestamp);
    
    // Suspicious if rotated within 24 hours of registration
    if (timeSinceRegistration < 24 * 60 * 60 * 1000) {
      issues.push({
        type: 'suspicious_rotation',
        entry: rotation,
        reason: 'Key rotated shortly after registration'
      });
    }
  }
  
  // Check for multiple rotations
  if (rotations.length > 3) {
    issues.push({
      type: 'excessive_rotations',
      count: rotations.length,
      reason: 'Unusually high number of key rotations'
    });
  }
  
  return {
    address,
    entries,
    issues,
    verified: issues.length === 0
  };
}
```

## Server Implementation

### Log Storage

```typescript
class TransparencyLogStorage {
  private db: Database;
  private merkleTree: MerkleTree;
  
  async appendEntry(entry: LogEntry): Promise<void> {
    // Begin transaction
    const tx = await this.db.beginTransaction();
    
    try {
      // Insert entry
      await tx.execute(
        'INSERT INTO log_entries VALUES (?, ?, ?, ?)',
        [entry.sequence, entry.timestamp, entry.merkleTreeHash, 
         JSON.stringify(entry)]
      );
      
      // Update Merkle tree
      await this.merkleTree.append(entry.merkleTreeHash);
      
      // Commit
      await tx.commit();
    } catch (error) {
      await tx.rollback();
      throw error;
    }
  }
  
  async getEntries(
    address: string,
    fromSequence?: number,
    toSequence?: number
  ): Promise<LogEntry[]> {
    let query = 'SELECT data FROM log_entries WHERE address = ?';
    const params: any[] = [address];
    
    if (fromSequence !== undefined) {
      query += ' AND sequence >= ?';
      params.push(fromSequence);
    }
    
    if (toSequence !== undefined) {
      query += ' AND sequence <= ?';
      params.push(toSequence);
    }
    
    query += ' ORDER BY sequence ASC';
    
    const rows = await this.db.query(query, params);
    return rows.map(row => JSON.parse(row.data));
  }
}
```

### STH Publishing

```typescript
class STHPublisher {
  private currentSTH: SignedTreeHead | null = null;
  private updateInterval = 3600000; // 1 hour
  
  async start(): Promise<void> {
    // Publish initial STH
    await this.publishSTH();
    
    // Periodic updates
    setInterval(() => this.publishSTH(), this.updateInterval);
  }
  
  async publishSTH(): Promise<void> {
    const treeSize = await merkleTree.getSize();
    const rootHash = await merkleTree.getRootHash();
    
    const sth: SignedTreeHead = {
      treeSize,
      timestamp: new Date().toISOString(),
      rootHash,
      serverDomain: SERVER_DOMAIN,
      logId: LOG_ID,
      signature: ''
    };
    
    // Sign STH
    const data = JSON.stringify({
      treeSize: sth.treeSize,
      timestamp: sth.timestamp,
      rootHash: sth.rootHash,
      serverDomain: sth.serverDomain,
      logId: sth.logId
    });
    
    sth.signature = await Ed25519.sign(SERVER_KEY, data);
    
    // Store STH
    await db.insertSTH(sth);
    this.currentSTH = sth;
    
    // Broadcast to monitors
    await this.broadcastSTH(sth);
  }
}
```

## Monitoring & Gossip

### Independent Monitors

Third-party monitors audit logs:

```typescript
class LogMonitor {
  private knownSTHs = new Map<string, SignedTreeHead>();
  
  async monitorLog(logUrl: string): Promise<void> {
    setInterval(async () => {
      try {
        // Fetch latest STH
        const newSTH = await fetch(`${logUrl}/sth`).then(r => r.json());
        
        // Get previous STH for this log
        const oldSTH = this.knownSTHs.get(logUrl);
        
        if (oldSTH) {
          // Verify consistency
          const proof = await fetch(
            `${logUrl}/consistency/${oldSTH.treeSize}/${newSTH.treeSize}`
          ).then(r => r.json());
          
          const consistent = await verifyConsistency({
            oldSize: oldSTH.treeSize,
            newSize: newSTH.treeSize,
            path: proof.path,
            oldSTH,
            newSTH
          });
          
          if (!consistent) {
            await this.alertInconsistency(logUrl, oldSTH, newSTH);
          }
        }
        
        // Store new STH
        this.knownSTHs.set(logUrl, newSTH);
        
      } catch (error) {
        await this.alertError(logUrl, error);
      }
    }, 600000); // Every 10 minutes
  }
}
```

### Gossip Protocol

Servers gossip STHs to detect split-view attacks:

```typescript
class STHGossip {
  async gossipSTH(sth: SignedTreeHead): Promise<void> {
    // Send to peer servers
    const peers = await getPeerServers();
    
    for (const peer of peers) {
      try {
        await fetch(`${peer.url}/gossip/sth`, {
          method: 'POST',
          body: JSON.stringify(sth),
          headers: { 'Content-Type': 'application/json' }
        });
      } catch (error) {
        console.error(`Failed to gossip to ${peer.domain}:`, error);
      }
    }
  }
  
  async receiveSTH(sth: SignedTreeHead): Promise<void> {
    // Verify signature
    if (!await verifySTHSignature(sth)) {
      throw new Error('Invalid STH signature');
    }
    
    // Check against our view
    const ourSTH = await getOurSTH(sth.serverDomain);
    
    if (ourSTH && ourSTH.rootHash !== sth.rootHash) {
      // Different views!
      await alertSplitView(sth.serverDomain, ourSTH, sth);
    }
    
    // Store received STH
    await storeGossipedSTH(sth);
  }
}
```

## API Endpoints

### Client API

```http
# Get entries for user
GET /transparency/v1/entries/{address}

# Get inclusion proof
GET /transparency/v1/proof/inclusion/{sequence}

# Get consistency proof
GET /transparency/v1/proof/consistency/{oldSize}/{newSize}

# Get latest STH
GET /transparency/v1/sth

# Get historical STH
GET /transparency/v1/sth/{timestamp}
```

### Example Responses

**Get Entries:**
```json
{
  "address": "alice@example.com",
  "entries": [
    {
      "sequence": 1234,
      "timestamp": "2026-01-01T00:00:00Z",
      "operation": "register",
      "identityKey": "base64-key",
      "merkleTreeHash": "base64-hash",
      "serverSignature": "base64-sig"
    }
  ]
}
```

**Inclusion Proof:**
```json
{
  "leafIndex": 1234,
  "treeSize": 10000,
  "path": [
    "base64-hash-1",
    "base64-hash-2",
    "base64-hash-3"
  ],
  "sth": {
    "treeSize": 10000,
    "rootHash": "base64-root",
    "timestamp": "2026-02-17T21:00:00Z",
    "signature": "base64-sig"
  }
}
```

## Privacy Considerations

### Address Correlation

Logs contain addresses, enabling correlation:

**Mitigations:**
- Rate-limit log queries
- Require authentication for queries
- Add noise/delays to query responses
- Consider zero-knowledge proofs (future)

### Timing Analysis

Entry timestamps reveal activity patterns:

**Mitigations:**
- Batch entries (publish every N entries or M time)
- Round timestamps to hour/day
- Add dummy entries (future)

## Performance

### Optimization

```typescript
class OptimizedMerkleTree {
  private cache = new LRUCache<number, string>(1000);
  
  async getRootHash(): Promise<string> {
    const treeSize = await this.getSize();
    
    // Check cache
    const cached = this.cache.get(treeSize);
    if (cached) return cached;
    
    // Compute root
    const root = await this.computeRoot();
    
    // Cache result
    this.cache.set(treeSize, root);
    
    return root;
  }
  
  // Batch append for better performance
  async appendBatch(hashes: string[]): Promise<void> {
    const tx = await this.db.beginTransaction();
    
    try {
      for (const hash of hashes) {
        await this.appendSingle(hash, tx);
      }
      await tx.commit();
    } catch (error) {
      await tx.rollback();
      throw error;
    }
  }
}
```

### Scalability

Handle millions of entries:

- **Sharding**: Separate logs per domain
- **Archival**: Archive old entries
- **Compression**: Compress historical data
- **CDN**: Serve proofs from CDN

## Testing

```typescript
describe('Transparency Logs', () => {
  it('should append entries and verify inclusion', async () => {
    const log = new TransparencyLog();
    
    // Append entry
    const entry = await log.appendEntry({
      operation: 'register',
      address: 'alice@example.com',
      identityKey: 'key123'
    });
    
    // Get proof
    const proof = await log.getInclusionProof(entry.sequence);
    
    // Verify
    const valid = await verifyInclusion(entry, proof);
    expect(valid).toBe(true);
  });
  
  it('should detect log tampering', async () => {
    // ... test implementation
  });
});
```

## Future Enhancements

1. **Zero-Knowledge Proofs**: Prove inclusion without revealing entry
2. **Private Set Intersection**: Check membership privately
3. **Distributed Logs**: Multiple servers maintain same log
4. **Blockchain Integration**: Anchor STHs in blockchain
5. **Threshold Signatures**: Require multiple parties to sign STH
