import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import dotenv from 'dotenv';
import { initializeDatabase } from './db/schema';
import { authRouter } from './api/auth';
import { messagesRouter } from './api/messages';
import { keysRouter } from './api/keys';
import { federationRouter } from './api/federation';
import { wellKnownRouter } from './api/wellknown';
import { errorHandler } from './middleware/error';
import { requestLogger } from './middleware/logger';

// Load environment variables
dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;
const DOMAIN = process.env.DOMAIN || 'localhost';

// Middleware
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(requestLogger);

// Routes
app.use('/.well-known', wellKnownRouter);
app.use('/api/v1/auth', authRouter);
app.use('/api/v1/messages', messagesRouter);
app.use('/api/v1/keys', keysRouter);
app.use('/federation/v1', federationRouter);

// Health check
app.get('/api/v1/health', (req, res) => {
  res.json({
    status: 'healthy',
    version: '0.1.0',
    domain: DOMAIN,
    timestamp: new Date().toISOString()
  });
});

// Error handling
app.use(errorHandler);

// Initialize database and start server
async function start() {
  try {
    await initializeDatabase();
    
    app.listen(PORT, () => {
      console.log(`🚀 Secure Mail Server started`);
      console.log(`📍 Domain: ${DOMAIN}`);
      console.log(`🔌 Port: ${PORT}`);
      console.log(`🔐 TLS: ${process.env.NODE_ENV === 'production' ? 'Enabled' : 'Disabled (dev)'}`);
      console.log(`🌐 Federation: ${process.env.ENABLE_FEDERATION === 'true' ? 'Enabled' : 'Disabled'}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

// Graceful shutdown
process.on('SIGTERM', () => {
  console.log('SIGTERM received, shutting down gracefully...');
  process.exit(0);
});

process.on('SIGINT', () => {
  console.log('SIGINT received, shutting down gracefully...');
  process.exit(0);
});

start();
