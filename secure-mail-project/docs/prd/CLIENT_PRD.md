# Client PRD - Secure Mail

## Product Overview

The Secure Mail client is a cross-platform application for sending and receiving encrypted messages. It handles local key management, encryption/decryption, and provides a user-friendly interface for secure communication.

## Goals

1. **Secure by Default**: E2EE for all messages without user intervention
2. **User-Friendly**: Simple, intuitive interface that doesn't expose complexity
3. **Cross-Platform**: Work on desktop (Windows, macOS, Linux) and mobile (iOS, Android)
4. **Privacy-Focused**: Minimize metadata exposure and protect user identity
5. **Offline-Capable**: Allow composing messages while offline

## Non-Goals

- Server management (users connect to existing servers)
- Advanced cryptography options (secure defaults, no configuration)
- Social networking features
- End-user server hosting from the client app

## User Stories

### As a User
- I want to send encrypted messages easily without thinking about keys
- I want confidence that my messages are truly private
- I want to verify my contacts' identities
- I want my messages synced across my devices
- I want to know when my message was delivered and read

### As a Privacy-Conscious User
- I want to use the app over Tor
- I want minimal metadata collection
- I want to verify server certificates
- I want control over what data is shared

### As a New User
- I want quick onboarding without complex setup
- I want clear explanations of security features
- I want confidence the app is trustworthy

## Core Features

### 1. Account Management

**Priority**: P0 (Demo Milestone)

- Create account with secure password
- Automatic key generation and backup
- Account recovery options
- Multi-device support

**User Flow**:
1. User enters username and password
2. Client generates identity keys locally
3. Client uploads public keys to server
4. User receives recovery codes

**UI Components**:
- Registration screen
- Login screen
- Account settings
- Device management

### 2. Contact Management

**Priority**: P0 (Demo Milestone)

- Add contacts by address (user@domain)
- Verify contact identity (safety numbers)
- Display contact status
- Search/filter contacts

**Features**:
- Contact list view
- Contact profile view
- Identity verification screen
- Contact search

### 3. Message Composition & Sending

**Priority**: P0 (Demo Milestone)

- Compose text messages
- Automatic encryption
- Delivery status indicators
- Read receipts (optional)

**User Flow**:
1. User selects contact or enters address
2. User types message
3. Client encrypts locally
4. Client sends to server
5. User sees delivery confirmation

**UI Components**:
- Message compose screen
- Send button with status
- Delivery indicators (sent, delivered, read)

### 4. Message Display & Reading

**Priority**: P0 (Demo Milestone)

- Display conversation threads
- Automatic decryption
- Message timestamps
- Unread message indicators

**Features**:
- Conversation list
- Message thread view
- Message search
- Notifications

### 5. End-to-End Encryption

**Priority**: P0 (Demo Milestone)

- Transparent E2EE (user doesn't need to know)
- Perfect forward secrecy
- Key exchange automation
- Encryption indicators

**Security**:
- Signal Protocol implementation
- Prekey bundles
- Ratcheting for forward secrecy
- Padlock icon shows encryption status

### 6. Key Management

**Priority**: P0 (Demo Milestone)

- Automatic key generation
- Secure key storage (OS keychain)
- Key backup and recovery
- Key rotation

**Features**:
- Automatic prekey upload
- Key status indicators
- Recovery code generation
- Device key management

### 7. Server Discovery & Connection

**Priority**: P0 (Demo Milestone)

- Connect to user's home server
- Discover federated servers
- TLS certificate validation
- Connection status display

**Configuration**:
- Server address input
- Automatic .well-known discovery
- Custom server support
- Connection retry logic

### 8. Privacy Features

**Priority**: P1

- Tor support for connections
- Metadata minimization
- Typing indicators (disabled by default)
- Read receipts (opt-in)

**Settings**:
- Privacy settings panel
- Tor toggle
- Read receipts toggle
- Metadata visibility controls

### 9. Identity Verification

**Priority**: P1

- Safety number comparison
- QR code scanning
- Trust indicator
- Verification status display

**UI**:
- Verification screen
- QR code display/scan
- Safety number display
- Trust badge on contacts

### 10. Notifications

**Priority**: P1

- New message notifications
- Desktop notifications
- Mobile push (when available)
- Notification privacy (hide content)

**Features**:
- Notification settings
- Sound/vibration controls
- Content hiding option
- Do not disturb mode

## Technical Requirements

### Performance
- Message send latency < 1s (local encryption time)
- UI responsiveness < 100ms
- Startup time < 3s
- Low CPU/memory footprint

### Platforms
- **Desktop**: Electron or native (Qt, Flutter)
- **Mobile**: React Native or Flutter
- **Web**: Progressive Web App (optional)

### Security
- Secure local storage (encrypted database)
- OS keychain integration
- Memory wiping for sensitive data
- Screen capture protection (mobile)

### Privacy
- No telemetry without consent
- Minimal server communication
- Local-first data storage
- IP protection (Tor support)

### Offline Support
- Compose messages offline
- Queue for delivery when online
- Local message storage
- Sync when reconnected

## User Interface

### Key Screens

1. **Login/Registration**
   - Simple form
   - Progress indicators
   - Security explanations

2. **Conversation List**
   - Contact names
   - Last message preview
   - Unread indicators
   - Timestamp

3. **Message Thread**
   - Conversation history
   - Message bubbles
   - Compose field
   - Send button

4. **Contact Profile**
   - Contact info
   - Verification status
   - Safety number
   - Actions (block, verify)

5. **Settings**
   - Account settings
   - Privacy settings
   - Notification settings
   - About/Help

### Design Principles

- **Simple**: Hide complexity, show what matters
- **Clear**: Obvious actions, clear status
- **Trustworthy**: Security indicators, verification options
- **Accessible**: WCAG AA compliance, screen reader support

## Data Models

### Local Database

#### User Account
```json
{
  "userId": "uuid",
  "username": "string",
  "domain": "string",
  "identityKeyPair": "encrypted-keypair",
  "devices": ["device-list"]
}
```

#### Contact
```json
{
  "contactId": "uuid",
  "address": "user@domain",
  "displayName": "string",
  "identityKey": "base64-public-key",
  "verificationStatus": "unverified|verified",
  "safetyNumber": "string"
}
```

#### Message
```json
{
  "messageId": "uuid",
  "conversationId": "uuid",
  "from": "user@domain",
  "to": "user@domain",
  "content": "decrypted-text",
  "timestamp": "iso8601",
  "status": "sending|sent|delivered|read|failed",
  "isRead": boolean
}
```

#### Conversation
```json
{
  "conversationId": "uuid",
  "participants": ["user@domain"],
  "lastMessage": "message-id",
  "unreadCount": number,
  "lastActivity": "iso8601"
}
```

## Success Metrics

### Demo Milestone
- ✓ Send message between clients
- ✓ Receive and decrypt messages
- ✓ Display conversations correctly
- ✓ E2EE verified working

### Production Metrics
- User retention > 60% (30 days)
- Message send success rate > 99%
- Zero credential leaks
- Crash rate < 0.1%

## User Experience Flow

### First-Time Setup
1. Launch app
2. See welcome screen explaining security
3. Choose "Create Account" or "Login"
4. Enter username and password
5. App generates keys (show progress)
6. Display recovery codes
7. User confirms backup
8. Show tutorial
9. Ready to send messages

### Sending First Message
1. Click "New Message"
2. Enter contact address
3. Type message
4. Click send
5. See encryption indicator
6. See delivery confirmation
7. Receive reply notification

### Verifying Contact
1. Open contact profile
2. Click "Verify Identity"
3. See safety number
4. Compare in person or scan QR
5. Mark as verified
6. See trust badge

## Technical Stack

### Recommended
- **Framework**: Electron (desktop) + React Native (mobile)
- **UI**: React + TypeScript
- **State**: Redux or Zustand
- **Storage**: SQLite with SQLCipher
- **Crypto**: @signalapp/libsignal-client

### Alternative
- **Framework**: Flutter (all platforms)
- **Storage**: Hive + Flutter Secure Storage
- **Crypto**: Pure Dart crypto libraries

## Dependencies

- Signal Protocol library
- TLS/HTTPS client
- SQLite database
- Cryptography library (libsodium)
- QR code scanner/generator
- Notification framework

## Timeline

- **Phase 1 (Demo)**: 4-6 weeks
  - Basic UI
  - Message send/receive
  - E2EE implementation
  
- **Phase 2 (Alpha)**: 8-12 weeks
  - All core features
  - Cross-platform support
  - Polished UI
  
- **Phase 3 (Beta)**: 12-16 weeks
  - Performance optimization
  - Security audit
  - User testing

## Open Questions

1. **Platform Priority**: Desktop-first or mobile-first?
   - Recommendation: Desktop for demo, mobile for production

2. **Framework**: Native vs. cross-platform?
   - Recommendation: Electron/React Native for speed, native for performance

3. **Push Notifications**: How to handle privacy?
   - Recommendation: Generic notifications, no message content

4. **Backup**: Cloud backup vs. local only?
   - Recommendation: Local + optional encrypted cloud backup

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Crypto implementation bugs | Critical | Use well-tested libraries, security audit |
| Poor UX adoption | High | User testing, iterate on feedback |
| Platform fragmentation | Medium | Start with one platform, expand |
| Key loss | High | Multiple recovery options, clear warnings |
| Performance on old devices | Medium | Optimize, set minimum requirements |
