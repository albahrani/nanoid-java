# Secure Mail Server

Demo server implementation for the Secure Mail project.

## Features (Demo Milestone)

- User registration and authentication
- End-to-end encrypted message storage and delivery
- Basic federation support
- Key management (identity keys, prekeys)
- Server discovery (.well-known)

## Quick Start

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Run production server
npm start

# Run tests
npm test
```

## Configuration

Create a `.env` file:

```env
# Server Configuration
PORT=3000
DOMAIN=server1.local
SERVER_NAME=Server 1

# Database
DATABASE_PATH=./data/server.db

# Cryptography
SERVER_PRIVATE_KEY=<generate-with-keygen-script>

# Federation
ENABLE_FEDERATION=true
FEDERATION_PORT=3001

# Security
RATE_LIMIT_MESSAGES=1000
RATE_LIMIT_WINDOW=3600
```

## API Endpoints

### Authentication

```
POST /api/v1/auth/register    - Register new user
POST /api/v1/auth/login       - Login user
POST /api/v1/auth/logout      - Logout user
POST /api/v1/auth/refresh     - Refresh token
```

### Messages

```
POST /api/v1/messages/send    - Send message
GET  /api/v1/messages/inbox   - Get inbox
POST /api/v1/messages/ack     - Acknowledge message
DELETE /api/v1/messages/:id   - Delete message
```

### Keys

```
GET  /api/v1/keys/:address         - Get user's public keys
POST /api/v1/keys/upload           - Upload keys
GET  /api/v1/keys/prekeys/:address - Get prekey bundle
```

### Federation

```
POST /federation/v1/deliver           - Deliver message
GET  /federation/v1/keys/:address     - Get user keys
GET  /federation/v1/capabilities      - Get server capabilities
```

### Discovery

```
GET /.well-known/secure-mail    - Server discovery
GET /api/v1/health              - Health check
```

## Project Structure

```
server/
├── src/
│   ├── api/              # API routes
│   │   ├── auth.ts
│   │   ├── messages.ts
│   │   ├── keys.ts
│   │   └── federation.ts
│   ├── crypto/           # Cryptographic operations
│   │   ├── keys.ts
│   │   └── signatures.ts
│   ├── db/               # Database layer
│   │   ├── schema.ts
│   │   └── queries.ts
│   ├── federation/       # Federation logic
│   │   ├── discovery.ts
│   │   └── delivery.ts
│   ├── middleware/       # Express middleware
│   │   ├── auth.ts
│   │   └── ratelimit.ts
│   ├── models/           # Data models
│   │   ├── user.ts
│   │   ├── message.ts
│   │   └── key.ts
│   └── index.ts          # Entry point
├── tests/
│   ├── api/
│   ├── crypto/
│   └── federation/
├── scripts/
│   ├── keygen.ts         # Generate server keys
│   └── migrate.ts        # Database migrations
├── package.json
├── tsconfig.json
└── README.md
```

## Development

```bash
# Start development server with auto-reload
npm run dev

# Build for production
npm run build

# Run linter
npm run lint

# Run formatter
npm run format

# Run type checking
npm run typecheck
```

## Demo Setup

Run 3 servers for demo:

```bash
# Terminal 1 - Server 1
cd server
PORT=3001 DOMAIN=server1.local npm start

# Terminal 2 - Server 2
cd server
PORT=3002 DOMAIN=server2.local npm start

# Terminal 3 - Server 3
cd server
PORT=3003 DOMAIN=server3.local npm start
```

## Testing

```bash
# Unit tests
npm run test:unit

# Integration tests
npm run test:integration

# Federation tests
npm run test:federation

# All tests
npm test

# Coverage
npm run test:coverage
```

## Database Schema

```sql
-- Users
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  username TEXT NOT NULL,
  domain TEXT NOT NULL,
  password_hash TEXT NOT NULL,
  identity_key TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(username, domain)
);

-- Devices
CREATE TABLE devices (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  device_key TEXT NOT NULL,
  signed_prekey TEXT NOT NULL,
  signature TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES users(id)
);

-- One-Time Prekeys
CREATE TABLE one_time_prekeys (
  id TEXT PRIMARY KEY,
  device_id TEXT NOT NULL,
  public_key TEXT NOT NULL,
  used BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(device_id) REFERENCES devices(id)
);

-- Messages
CREATE TABLE messages (
  id TEXT PRIMARY KEY,
  from_address TEXT NOT NULL,
  to_address TEXT NOT NULL,
  encrypted_payload TEXT NOT NULL,
  timestamp DATETIME NOT NULL,
  delivered BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_to_address (to_address),
  INDEX idx_delivered (delivered)
);

-- Sessions
CREATE TABLE sessions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  token TEXT NOT NULL,
  expires_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES users(id)
);
```

## Security

- TLS 1.3 only
- bcrypt password hashing
- JWT session tokens
- Rate limiting on all endpoints
- Input validation
- SQL injection prevention (parameterized queries)
- XSS prevention (no HTML rendering)

## License

[To be determined]
