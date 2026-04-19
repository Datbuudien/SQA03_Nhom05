// src/components/ReactFlowCanvas.tsx - Fixed drag detection
import React from "react";
import ReactFlow, {
  Background,
  BackgroundVariant,
  Controls,
  MiniMap,
  Node,
  Edge,
  Connection,
  NodeChange,
  EdgeChange,
} from "reactflow";
import "reactflow/dist/style.css";
import { ModelNode } from "../SchemaVisualizer/ModelNode";
import { SchemaData } from "../SchemaVisualizer/SchemaVisualizer.types";
import { CustomControls } from "./CustomControls";
import { useColorModeValue } from "@chakra-ui/react";
import { usePermission } from "../hooks/usePermission";

const modelTypes = {
  model: ModelNode,
};

interface ReactFlowCanvasProps {
  nodes: Node[];
  edges: Edge[];
  onNodesChange: (changes: NodeChange[]) => void;
  onEdgesChange: (changes: EdgeChange[]) => void;
  onConnect: (connection: Connection) => void;
  onNodeDragStart?: (event: React.MouseEvent, node: Node) => void;
  onNodeDrag?: (event: React.MouseEvent, node: Node) => void;
  onNodeDragStop: (event: React.MouseEvent, node: Node) => void;
  onNodeClick?: (event: React.MouseEvent, node: Node) => void;
  schemaInfo?: SchemaData | null;
}

export const ReactFlowCanvas: React.FC<ReactFlowCanvasProps> = ({
  nodes,
  edges,
  onNodesChange,
  onEdgesChange,
  onConnect,
  onNodeDragStart,
  onNodeDrag,
  onNodeDragStop,
  onNodeClick,
  schemaInfo,
}) => {
  const bgPatternColor = useColorModeValue("#cececeff", "#333");
  const { canEdit, permission } = usePermission();

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
      onNodeDragStart={onNodeDragStart}
      onNodeDrag={onNodeDrag}
      onNodeDragStop={onNodeDragStop}
      onNodeClick={onNodeClick}
      nodeTypes={modelTypes}
      fitView
      fitViewOptions={{ padding: 0.4 }}
      nodesDraggable={canEdit}
      nodesConnectable={false}
      elementsSelectable={canEdit}
      selectNodesOnDrag={false}
      panOnDrag={true}
      zoomOnScroll={true}
      zoomOnPinch={true}
      panOnScroll={false}
      // Prevent node selection on click to avoid visual jumps
      nodesFocusable={false}
      defaultViewport={{
        x: schemaInfo?.panX || 0,
        y: schemaInfo?.panY || 0,
        zoom: schemaInfo?.zoomLevel || 1,
      }}
      minZoom={0.1}
      maxZoom={4}
    >
      {/* <MiniMap /> */}

      <Background
        color={bgPatternColor} // 🌟 THAY ĐỔI TỪ "#333" THÀNH bgPatternColor
        variant={BackgroundVariant.Lines}
        gap={40}
      />
    </ReactFlow>
  );
};
