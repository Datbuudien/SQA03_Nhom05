// src/contexts/WebSocketContext.tsx
import React, { createContext, useContext, useState, useCallback } from "react";

interface WebSocketContextType {
  isConnected: boolean;
  setIsConnected: (connected: boolean) => void;
  sessionId: string | null;
  setSessionId: (id: string | null) => void;
  onlineUsernames: string[]; // ✅ Thêm
  setOnlineUsernames: (usernames: string[]) => void; // ✅ Thêm
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(
  undefined
);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [isConnected, setIsConnected] = useState(false);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [onlineUsernames, setOnlineUsernames] = useState<string[]>([]); // ✅ Thêm

  return (
    <WebSocketContext.Provider
      value={{
        isConnected,
        setIsConnected,
        sessionId,
        setSessionId,
        onlineUsernames,
        setOnlineUsernames,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocketContext = () => {
  const context = useContext(WebSocketContext);
  if (context === undefined) {
    throw new Error(
      "useWebSocketContext must be used within WebSocketProvider"
    );
  }
  return context;
};
