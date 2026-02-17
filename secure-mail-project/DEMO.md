# Demo Scripts

Scripts to help run the demo milestone setup.

## Running the Demo

### Option 1: Single Script

```bash
./run-demo.sh
```

This will:
1. Start 3 servers (server1, server2, server3)
2. Start 3 clients (one for each server)
3. Configure /etc/hosts entries (requires sudo)

### Option 2: Manual Setup

#### 1. Configure Hosts

Add to `/etc/hosts`:
```
127.0.0.1 server1.local
127.0.0.1 server2.local
127.0.0.1 server3.local
```

#### 2. Start Servers

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

#### 3. Start Clients

```bash
# Terminal 4 - Client for Alice on server1
cd client
REACT_APP_DEFAULT_SERVER=https://server1.local:3001 npm start

# Terminal 5 - Client for Bob on server2
cd client
REACT_APP_DEFAULT_SERVER=https://server2.local:3002 npm start

# Terminal 6 - Client for Carol on server3
cd client
REACT_APP_DEFAULT_SERVER=https://server3.local:3003 npm start
```

## Demo Test Scenario

1. **Register Users**
   - Client 1: Register `alice` on server1.local
   - Client 2: Register `bob` on server2.local
   - Client 3: Register `carol` on server3.local

2. **Send Messages**
   - Alice → Bob: "Hello from server1!"
   - Bob → Carol: "Hello from server2!"
   - Carol → Alice: "Hello from server3!"

3. **Verify Federation**
   - All messages should be delivered across servers
   - Check server logs for federation activity
   - Verify E2EE is working (servers can't read content)

## Monitoring

```bash
# Watch server logs
tail -f server/logs/server*.log

# Check message delivery
curl http://server1.local:3001/api/v1/health
curl http://server2.local:3002/api/v1/health
curl http://server3.local:3003/api/v1/health
```

## Cleanup

```bash
./cleanup-demo.sh
```

This will:
- Stop all servers
- Stop all clients
- Clean up temporary data
- Remove /etc/hosts entries (optional)
