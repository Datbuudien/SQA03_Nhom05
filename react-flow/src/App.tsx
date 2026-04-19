// src/App.tsx
import React from "react";
import { ChakraProvider, ColorModeScript } from "@chakra-ui/react";
import { WebSocketProvider } from "./contexts/WebSocketContext";
import { UserProvider } from "./contexts/UserContext";
import { SchemaVisualizer } from "./SchemaVisualizer/SchemaVisualizer";
import { HistoryViewPage } from "./pages/HistoryViewPage";
import { ReactFlowProvider } from "reactflow";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "./pages/LoginPage";
import { HomePage } from "./pages/HomePage";
import { MyDiagramsPage } from "./pages/MyDiagramsPage";
import { SharedDiagramsPage } from "./pages/SharedDiagramsPage";
import { TrashPage } from "./pages/TrashPage";
import { ProfilePage } from "./pages/ProfilePage";
import theme from "./theme";
import { StarPage } from "./pages/StarPage";
import { StatisticsPage } from "./pages/StatisticsPage";

function App() {
  return (
    <>
      <ColorModeScript initialColorMode={theme.config.initialColorMode} />
      <ChakraProvider theme={theme}>
        <BrowserRouter>
          <UserProvider>
            <WebSocketProvider>
              <ReactFlowProvider>
                <Routes>
                  <Route path="/login" element={<LoginPage />} />
                  <Route path="/home" element={<HomePage />}>
                    <Route index element={<MyDiagramsPage />} />
                    <Route path="shared" element={<SharedDiagramsPage />} />
                    <Route path="trash" element={<TrashPage />} />
                    <Route path="statistics" element={<StatisticsPage />} />
                    <Route path="star" element={<StarPage />} />
                    <Route path="profile" element={<ProfilePage />} />
                  </Route>
                  <Route path="/:diagramId" element={<SchemaVisualizer />} />
                  {/* ⭐ NEW: History view route */}
                  <Route
                    path="/history/:migrationId"
                    element={<HistoryViewPage />}
                  />
                  <Route path="/" element={<Navigate to="/home" replace />} />
                </Routes>
              </ReactFlowProvider>
            </WebSocketProvider>
          </UserProvider>
        </BrowserRouter>
      </ChakraProvider>
    </>
  );
}

export default App;
