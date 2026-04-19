// src/SchemaVisualizer/SchemaVisualizerContent.tsx
import React, { useState } from "react";
import {
  Badge,
  Box,
  Text,
  useColorModeValue,
  useDisclosure,
  VStack,
} from "@chakra-ui/react";

import { LoadingScreen } from "../components/LoadingScreen";
import { ControlPanel } from "../components/ControlPanel";
import { ReactFlowCanvas } from "../components/ReactFlowCanvas";
import { AddModelButton } from "../components/AddModelButton";
import { TableListButton } from "../components/TableListButton";
import { TableListDialog } from "../components/TableListDialog";
import { SchemaVisualizerHeader } from "../components/SchemaVisualizerHeader";
import FloatingChat from "../components/page/FloatingChat";

import { useSchemaVisualizer } from "../hooks/useSchemaVisualizer";
import { ExportDiagramButton } from "../components/ExportDiagramButton";
import { CustomControls } from "../components/CustomControls";
import { AutoAlignButton } from "../components/AutoAlignButton";
import { usePermission } from "../hooks/usePermission";
import { useWebSocketContext } from "../contexts/WebSocketContext";
import { useWebSocketSender } from "../hooks/useWebSocketSender";

export const SchemaVisualizerContent = () => {
  const { sendNodePositionUpdate } = useWebSocketSender();
  const { isConnected } = useWebSocketContext();
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [isChatOpen, setIsChatOpen] = useState(false);
  const chatWidth = 340;
  const canvasBg = useColorModeValue("#f5f5f5", "#1C1c1c");

  const {
    // Data state
    loading,
    error,
    schemaInfo,
    onlineUsernames,

    // ReactFlow state
    reactFlowNodes,
    setReactFlowNodes,
    reactFlowEdges,
    onNodesChange,
    onEdgesChange,
    onConnect,

    // Enhanced drag handlers
    onNodeDragStart,
    onNodeDrag,
    onNodeDragStop,

    // Action handlers
    handleDeleteModel,
    handleAddModel,
    handleReset,
    handleInitialize,

    // Node handlers
    handleFieldNameUpdate,
    handleFieldTypeUpdate,
    handleToggleKeyType,
    handleAddAttribute,
    handleDeleteAttribute,
    handleModelNameUpdate,
    handleForeignKeyTargetSelect,
  } = useSchemaVisualizer();

  const { canEdit, permission } = usePermission();

  if (loading) {
    console.log("Rendering loading screen");
    return <LoadingScreen message="Loading schema data..." />;
  }

  return (
    <Box height="100vh" width="100vw" bg={canvasBg} position="relative">
      {/* ✅ Permission Banner */}
      {permission === "VIEW" && (
        <Box
          position="absolute"
          bottom={0}
          left="50%"
          transform="translate(-50%, 0)"
          zIndex={9999}
          pointerEvents="none"
        >
          <VStack bg="transparent" px={6} py={4} borderRadius="lg" spacing={2}>
            <Badge colorScheme="orange" fontSize="md" px={3} py={1}>
              VIEW ONLY MODE
            </Badge>
            <Text fontSize="sm">
              You have view-only permission to this diagram
            </Text>
          </VStack>
        </Box>
      )}
      {permission === "FULL_ACCESS" && !isConnected && (
        <Box
          position="absolute"
          bottom={0}
          left="50%"
          transform="translate(-50%, 0)"
          zIndex={9999}
          pointerEvents="none"
        >
          <VStack bg="transparent" px={6} py={4} borderRadius="lg" spacing={2}>
            <Badge colorScheme="orange" fontSize="md" px={3} py={1}>
              DISCONNECTED
            </Badge>
            <Text fontSize="sm">
              Please wait until reconnection is successful
            </Text>
          </VStack>
        </Box>
      )}

      {/* Header với Avatar, Theme Toggle, Chat, History, Add Member */}
      <SchemaVisualizerHeader
        onChatToggle={() => setIsChatOpen(!isChatOpen)}
        isChatOpen={isChatOpen}
        onlineUsernames={onlineUsernames}
        isHistoryView={false}
      />

      {/* Left side buttons */}
      <Box
        position="absolute"
        bottom="180px"
        left="10px"
        display="flex"
        flexDirection="column"
        gap="6px"
        bg="transparent"
        p="6px"
        zIndex={10}
      >
        {canEdit && (
          <>
            <AddModelButton
              isConnected={isConnected}
              onAddModel={handleAddModel}
            />
            <TableListButton onClick={onOpen} />
            <AutoAlignButton
              setReactFlowNodes={setReactFlowNodes}
              sendNodePositionUpdate={sendNodePositionUpdate}
            />
          </>
        )}

        <ExportDiagramButton schemaData={schemaInfo} />
      </Box>

      <CustomControls />

      <ControlPanel
        schemaName={schemaInfo.name}
        loading={loading}
        onReset={handleReset}
      />

      <ReactFlowCanvas
        nodes={reactFlowNodes}
        edges={reactFlowEdges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onNodeDragStart={onNodeDragStart}
        onNodeDrag={onNodeDrag}
        onNodeDragStop={onNodeDragStop}
        schemaInfo={schemaInfo}
      />

      {/* Table List Dialog */}
      <TableListDialog
        isOpen={isOpen}
        onClose={onClose}
        onFieldNameUpdate={handleFieldNameUpdate}
        onFieldTypeUpdate={handleFieldTypeUpdate}
        onToggleKeyType={handleToggleKeyType}
        onDeleteAttribute={handleDeleteAttribute}
        onAddAttribute={handleAddAttribute}
        onDeleteModel={handleDeleteModel}
        onModelNameUpdate={handleModelNameUpdate}
      />

      {/* Floating Chat Panel */}
      <FloatingChat
        isOpen={isChatOpen}
        width={chatWidth}
        onAddModel={handleAddModel}
        onModelNameUpdate={handleModelNameUpdate}
        onAddAttribute={handleAddAttribute}
        onForeignKeyConnect={handleForeignKeyTargetSelect}
        onFieldNameUpdate={handleFieldNameUpdate}
        onFieldTypeUpdate={handleFieldTypeUpdate}
        onToggleKeyType={handleToggleKeyType}
        onDeleteModel={handleDeleteModel}
        onDeleteAttribute={handleDeleteAttribute}
      />
    </Box>
  );
};
