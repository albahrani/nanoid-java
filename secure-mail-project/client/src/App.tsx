import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/auth';
import LoginScreen from './components/Auth/LoginScreen';
import RegisterScreen from './components/Auth/RegisterScreen';
import ConversationList from './components/Conversations/ConversationList';
import MessageThread from './components/Conversations/MessageThread';
import SettingsPanel from './components/Settings/SettingsPanel';

function App() {
  const { isAuthenticated } = useAuthStore();

  return (
    <div className="app">
      <Routes>
        <Route
          path="/login"
          element={
            isAuthenticated ? <Navigate to="/" /> : <LoginScreen />
          }
        />
        <Route
          path="/register"
          element={
            isAuthenticated ? <Navigate to="/" /> : <RegisterScreen />
          }
        />
        <Route
          path="/"
          element={
            isAuthenticated ? <ConversationList /> : <Navigate to="/login" />
          }
        />
        <Route
          path="/conversation/:address"
          element={
            isAuthenticated ? <MessageThread /> : <Navigate to="/login" />
          }
        />
        <Route
          path="/settings"
          element={
            isAuthenticated ? <SettingsPanel /> : <Navigate to="/login" />
          }
        />
      </Routes>
    </div>
  );
}

export default App;
