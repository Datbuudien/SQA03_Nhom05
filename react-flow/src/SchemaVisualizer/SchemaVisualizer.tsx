// src/SchemaVisualizer/SchemaVisualizer.tsx
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
import { PermissionProvider } from "../contexts/PermissionContext";
import { SchemaVisualizerContent } from "./SchemaVisualizerContent";

export const SchemaVisualizer = () => {
  const { schemaInfo, loading } = useSchemaVisualizer();

  if (loading) return <LoadingScreen />;

  return (
    <PermissionProvider permission={schemaInfo?.permission || null}>
      <SchemaVisualizerContent />
    </PermissionProvider>
  );
};
