# Secure Mail Client

Demo client implementation for the Secure Mail project.

## Features (Demo Milestone)

- User authentication
- End-to-end encrypted messaging
- Contact management
- Server discovery
- Signal Protocol implementation
- Local message storage

## Quick Start

```bash
# Install dependencies
npm install

# Run development
npm run dev

# Build for production
npm run build

# Run tests
npm test
```

## Configuration

Create `.env` file:

```env
# Client Configuration
REACT_APP_DEFAULT_SERVER=https://server1.local:3000

# Development
REACT_APP_DEV_MODE=true
REACT_APP_LOG_LEVEL=debug
```

## Features

### Core Functionality
- ✅ User registration/login
- ✅ End-to-end encryption (Signal Protocol)
- ✅ Message send/receive
- ✅ Contact management
- ✅ Server discovery
- ✅ Local storage (encrypted)

### User Interface
- Login/Registration screen
- Conversation list
- Message thread view
- Contact profile
- Settings panel

### Security
- Signal Protocol E2EE
- Local database encryption (SQLCipher)
- Secure key storage (OS keychain)
- Certificate validation

## Architecture

```
client/
├── src/
│   ├── components/          # React components
│   │   ├── Auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   └── RegisterScreen.tsx
│   │   ├── Conversations/
│   │   │   ├── ConversationList.tsx
│   │   │   └── MessageThread.tsx
│   │   ├── Contacts/
│   │   │   ├── ContactList.tsx
│   │   │   └── ContactProfile.tsx
│   │   └── Settings/
│   │       └── SettingsPanel.tsx
│   ├── services/            # Business logic
│   │   ├── auth.ts
│   │   ├── crypto.ts
│   │   ├── messaging.ts
│   │   └── storage.ts
│   ├── store/               # State management
│   │   ├── auth.ts
│   │   ├── messages.ts
│   │   └── contacts.ts
│   ├── crypto/              # Cryptography
│   │   ├── signal.ts
│   │   ├── keys.ts
│   │   └── ratchet.ts
│   ├── api/                 # Server communication
│   │   ├── client.ts
│   │   └── federation.ts
│   ├── db/                  # Local database
│   │   ├── schema.ts
│   │   └── queries.ts
│   ├── App.tsx              # Main app component
│   └── index.tsx            # Entry point
├── tests/
│   ├── components/
│   ├── crypto/
│   └── services/
├── package.json
├── tsconfig.json
└── README.md
```

## Development

```bash
# Start development server
npm run dev

# Build for production
npm run build

# Run tests
npm test

# Run linter
npm run lint

# Run type checking
npm run typecheck
```

## Demo Usage

### 1. Start Client

```bash
npm run dev
```

### 2. Register User

```
1. Click "Register"
2. Enter username: alice
3. Enter server: server1.local
4. Enter password
5. Client generates keys automatically
6. Registration complete
```

### 3. Send Message

```
1. Click "New Message"
2. Enter recipient: bob@server2.local
3. Type message
4. Click "Send"
5. Message encrypted locally
6. Delivered via federation
```

### 4. Receive Message

```
1. Client polls server for new messages
2. New message notification
3. Automatic decryption
4. Display in conversation thread
```

## Signal Protocol Integration

```typescript
// Initialize session
const session = new SignalProtocol();
await session.initialize(userIdentityKey);

// Send first message
const bobKeys = await fetchPrekeys('bob@server2.local');
const { ciphertext, session } = await session.encrypt(
  'bob@server2.local',
  'Hello, Bob!',
  bobKeys
);

// Subsequent messages use existing session
const { ciphertext } = await session.encrypt(
  'bob@server2.local',
  'Another message'
);

// Receive and decrypt
const plaintext = await session.decrypt(
  'alice@server1.local',
  encryptedMessage
);
```

## Local Storage

```typescript
// Encrypted SQLite database
interface Database {
  users: UserTable;
  contacts: ContactTable;
  messages: MessageTable;
  conversations: ConversationTable;
  sessions: RatchetSessionTable;
}

// All data encrypted at rest
const db = await openDatabase({
  path: './data/client.db',
  encryption: {
    key: deriveKeyFromPassword(userPassword)
  }
});
```

## UI Screenshots

### Login Screen
```
┌─────────────────────────────────────┐
│  Secure Mail                        │
├─────────────────────────────────────┤
│                                     │
│  Username: [____________]           │
│  Server:   [____________]           │
│  Password: [____________]           │
│                                     │
│  [ Login ]  [ Register ]            │
│                                     │
└─────────────────────────────────────┘
```

### Conversation List
```
┌─────────────────────────────────────┐
│  Conversations          [ + ]       │
├─────────────────────────────────────┤
│  👤 Bob                         2m  │
│     Hey, how are you?          (1)  │
├─────────────────────────────────────┤
│  👤 Carol                      1h   │
│     Thanks for the info!            │
├─────────────────────────────────────┤
│  👤 David                      2d   │
│     See you tomorrow                │
└─────────────────────────────────────┘
```

### Message Thread
```
┌─────────────────────────────────────┐
│  ← Bob                     🔒       │
├─────────────────────────────────────┤
│                                     │
│     Hello!              10:30 AM    │
│  [Bob]                              │
│                                     │
│                     Hi there! [You] │
│                       10:31 AM      │
│                                     │
│     How are you?        10:32 AM    │
│  [Bob]                              │
│                                     │
├─────────────────────────────────────┤
│  [____________] [ Send ]            │
└─────────────────────────────────────┘
```

## Testing

```bash
# Unit tests
npm run test:unit

# Integration tests
npm run test:integration

# E2E tests
npm run test:e2e

# All tests
npm test
```

## Building

### Desktop

```bash
# Build Electron app
npm run build:electron

# Package for distribution
npm run package:mac     # macOS
npm run package:win     # Windows
npm run package:linux   # Linux
```

### Mobile (Future)

```bash
# Build React Native app
npm run build:ios       # iOS
npm run build:android   # Android
```

## Security Best Practices

1. **Key Storage**
   - Use OS keychain (macOS Keychain, Windows Credential Manager, Linux Secret Service)
   - Encrypt database with user password
   - Never store plaintext keys

2. **Memory Safety**
   - Wipe sensitive data after use
   - Avoid logging sensitive information
   - Use secure randomness

3. **Network Security**
   - Validate TLS certificates
   - Pin server keys (TOFU)
   - Use secure WebSocket for real-time

4. **User Privacy**
   - Minimize metadata
   - Optional read receipts
   - No typing indicators by default

## Troubleshooting

### Common Issues

**Cannot connect to server**
- Check server URL is correct
- Verify server is running
- Check firewall settings

**Encryption fails**
- Verify recipient's keys are available
- Check Signal Protocol initialization
- Review console logs

**Database errors**
- Check database file permissions
- Verify encryption key is correct
- Try rebuilding database

## License

[To be determined]
