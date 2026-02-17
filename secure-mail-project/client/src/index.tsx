import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import './styles/index.css';

// Initialize application
async function initializeApp() {
  // Initialize crypto
  await import('./crypto/signal');
  
  // Initialize database
  await import('./db/schema');
  
  console.log('✓ Secure Mail Client initialized');
}

initializeApp().then(() => {
  const root = ReactDOM.createRoot(
    document.getElementById('root') as HTMLElement
  );
  
  root.render(
    <React.StrictMode>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </React.StrictMode>
  );
});
