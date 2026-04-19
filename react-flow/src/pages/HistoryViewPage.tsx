// src/pages/HistoryViewPage.tsx - EXACT COPY FROM useSchemaVisualizer
import React, { useState, useEffect, useMemo, useRef } from "react";
import { Badge, Box, Text, useColorModeValue, VStack } from "@chakra-ui/react";
import { useParams } from "react-router-dom";
import { useNodesState, useEdgesState, Node, Edge } from "reactflow";

import { LoadingScreen } from "../components/LoadingScreen";
import { ReactFlowCanvas } from "../components/ReactFlowCanvas";
import { SchemaVisualizerHeader } from "../components/SchemaVisualizerHeader";
import { HistoryControlPanel } from "../components/HistoryControlPanel";
import { CustomControls } from "../components/CustomControls";
import { PermissionProvider } from "../contexts/PermissionContext";

import { migrationApiService } from "../services/migrationApiService";
import { convertToReactFlowData } from "../utils/schemaUtils";
import { calculateOptimalHandlePositions } from "../utils/handlePositioning";
import {
  SchemaData,
  Attribute,
} from "../SchemaVisualizer/SchemaVisualizer.types";

export const HistoryViewPage = () => {
  const { migrationId } = useParams<{ migrationId: string }>();
  const [historyData, setHistoryData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [schemaInfo, setSchemaInfo] = useState<SchemaData | null>(null);

  const [reactFlowNodes, setReactFlowNodes, onNodesChange] = useNodesState([]);
  const [reactFlowEdges, setReactFlowEdges, onEdgesChange] = useEdgesState([]);

  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);

  const canvasBg = useColorModeValue("#f5f5f5", "#1C1c1c");

  const stableCallbacks = useMemo(
    () => ({
      onFieldNameUpdate: () => {},
      onFieldTypeUpdate: () => {},
      onToggleKeyType: () => {},
      onAddAttribute: () => {},
      onDeleteAttribute: () => {},
      onForeignKeyTargetSelect: () => {},
      onForeignKeyDisconnect: () => {},
      onModelNameUpdate: () => {},
      onDeleteModel: () => {},
    }),
    []
  );

  useEffect(() => {
    if (migrationId) {
      loadHistorySnapshot();
    }
  }, [migrationId]);

  const loadHistorySnapshot = async () => {
    setLoading(true);
    setError(null);

    try {
      const detail = await migrationApiService.getMigrationDetail(
        Number(migrationId)
      );
      setHistoryData(detail);

      const snapshot = JSON.parse(detail.snapshotJson);

      const schemaData: SchemaData = {
        id: snapshot.diagramId,
        permission: "VIEW",
        name: snapshot.diagramName,
        description: "",
        databaseType: snapshot.databaseType,
        version: snapshot.version,
        charset: snapshot.charset,
        collation: snapshot.collation,
        isPublic: false,
        isTemplate: false,
        zoomLevel: 1.0,
        panX: 0,
        panY: 0,
        models: snapshot.models.map((model: any) => ({
          id: model.id,
          nodeId: model.id,
          name: model.name,
          modelType: "TABLE",
          positionX: model.positionX,
          positionY: model.positionY,
          width: 280,
          height: 200,
          backgroundColor: "#f1f5f9",
          borderColor: "#e2e8f0",
          borderWidth: 2,
          borderRadius: 8,
          zindex: 100,
          attributes: model.attributes.map((attr: any) => ({
            id: attr.id,
            name: attr.name,
            dataType: attr.dataType,
            length: attr.length,
            isNullable: attr.isNullable,
            isPrimaryKey: attr.isPrimaryKey,
            isForeignKey: attr.isForeignKey,
            isUnique: false,
            isAutoIncrement: false,
            attributeOrder: attr.attributeOrder,
            connection: attr.connection
              ? {
                  id: `conn-${model.id}-${attr.id}`,
                  connectionType: "FOREIGN_KEY",
                  targetModelId: attr.connection.targetModelId,
                  targetAttributeId: attr.connection.targetAttributeId,
                  foreignKeyName: attr.connection.foreignKeyName,
                  isEnforced: true,
                  strokeColor: "#4A90E2",
                  strokeWidth: 2,
                  isAnimated: true,
                  targetArrowType: "arrow",
                }
              : undefined,
          })),
        })),
      };

      setSchemaInfo(schemaData);

      const reactFlowData = convertToReactFlowData(schemaData, stableCallbacks);

      console.log("📊 History converted:", {
        nodes: reactFlowData.nodes.length,
        edges: reactFlowData.edges.length,
      });

      setNodes(reactFlowData.nodes);
      setEdges(reactFlowData.edges);
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to load history snapshot"
      );
      console.error("❌ Error:", err);
    } finally {
      setLoading(false);
    }
  };

  // Node synchronization
  const nodesFingerprint = useMemo(() => {
    return nodes.map((node) => ({
      id: node.id,
      name: node.id,
      attributesHash:
        node.data.attributes
          ?.map(
            (attr: any) =>
              `${attr.id}-${attr.name}-${attr.dataType}-${attr.isPrimaryKey}-${
                attr.isForeignKey
              }-${
                attr.connection
                  ? `${attr.connection.targetModelId}.${attr.connection.targetAttributeId}`
                  : "none"
              }`
          )
          .join("|") || "",
      positionHash: `${node.position.x}-${node.position.y}`,
    }));
  }, [nodes]);

  const lastFingerprintRef = useRef<string>("");

  useEffect(() => {
    if (nodes.length === 0) {
      setReactFlowNodes([]);
      return;
    }

    const currentFingerprint = JSON.stringify(nodesFingerprint);
    if (currentFingerprint === lastFingerprintRef.current) return;

    lastFingerprintRef.current = currentFingerprint;

    setReactFlowNodes((currentNodes) => {
      const positionMap = new Map();
      currentNodes.forEach((node) => {
        positionMap.set(node.id, node.position);
      });

      const newNodes = nodes.map((node) => ({
        ...node,
        position: positionMap.get(node.id) || node.position,
        data: {
          ...node.data,
          ...stableCallbacks,
        },
      }));

      newNodes.forEach((node) => {
        node.data.reactFlowNodes = newNodes;
      });

      return newNodes;
    });
  }, [nodesFingerprint, stableCallbacks, nodes]);

  // Edge calculation
  const edgesFingerprint = useMemo(() => {
    if (reactFlowNodes.length === 0) return "empty";

    const connections: string[] = [];
    reactFlowNodes.forEach((node) => {
      const attributes = node.data.attributes || [];
      attributes.forEach((attribute: any) => {
        if (attribute.connection) {
          connections.push(
            `${node.id}:${attribute.id}->${attribute.connection.targetModelId}:${attribute.connection.targetAttributeId}`
          );
        }
      });
    });

    return connections.sort().join("|");
  }, [reactFlowNodes]);

  const edgeColor = useColorModeValue("#3e76b7ff", "#4A90E2");

  const edgesData = useMemo(() => {
    if (reactFlowNodes.length === 0) return [];

    const nodeMap = new Map(reactFlowNodes.map((node) => [node.id, node]));
    const newEdges: Edge[] = [];

    reactFlowNodes.forEach((node) => {
      const attributes: Attribute[] = node.data.attributes || [];
      attributes.forEach((attribute: Attribute) => {
        if (!attribute.connection) return;

        const connection = attribute.connection;
        const sourceNode = nodeMap.get(node.id);
        const targetNode = nodeMap.get(connection.targetModelId);

        if (!sourceNode || !targetNode) {
          console.warn(
            `⚠️ Missing node for edge: ${node.id} -> ${connection.targetModelId}`
          );
          return;
        }

        try {
          // ⭐ Use calculateOptimalHandlePositions
          const handlePositions = calculateOptimalHandlePositions(
            sourceNode,
            targetNode,
            attribute.id,
            connection.targetAttributeId
          );

          const edgeId = `${node.id}-${attribute.id}-${connection.targetModelId}`;

          newEdges.push({
            id: edgeId,
            source: node.id,
            target: connection.targetModelId,
            sourceHandle: handlePositions.sourceHandleId,
            targetHandle: handlePositions.targetHandleId,
            animated: connection.isAnimated || true,
            type: "step",
            style: {
              strokeWidth: 2,
              stroke: connection.strokeColor || edgeColor,
            },
            label: connection.foreignKeyName,
            labelStyle: {
              fontSize: "10px",
              fontWeight: "bold",
              fill: connection.strokeColor || "#4A90E2",
            },
            labelBgStyle: {
              fill: "rgba(255, 255, 255, 0.8)",
              fillOpacity: 0.8,
            },
          });
        } catch (error) {
          console.error("Error calculating handle positions:", error);
        }
      });
    });

    console.log(`🔗 Generated ${newEdges.length} edges`);
    return newEdges;
  }, [edgesFingerprint, reactFlowNodes, edgeColor]);

  useEffect(() => {
    setReactFlowEdges(edgesData);
  }, [edgesData, setReactFlowEdges]);

  if (loading) {
    return <LoadingScreen message="Loading history snapshot..." />;
  }

  if (error || !historyData || !schemaInfo) {
    return (
      <Box
        height="100vh"
        display="flex"
        justifyContent="center"
        alignItems="center"
      >
        <VStack spacing={4}>
          <Text color="red.500" fontSize="lg">
            {error || "Failed to load history"}
          </Text>
          <Text color="gray.500" fontSize="sm">
            Migration ID: {migrationId}
          </Text>
        </VStack>
      </Box>
    );
  }

  return (
    <PermissionProvider permission="VIEW">
      <Box height="100vh" width="100vw" bg={canvasBg} position="relative">
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
              HISTORY VIEW
            </Badge>
            <Text fontSize="sm">
              You are viewing a historical snapshot (read-only)
            </Text>
          </VStack>
        </Box>

        <SchemaVisualizerHeader isHistoryView={true} onlineUsernames={[]} />
        <HistoryControlPanel
          schemaName={historyData.diagramName}
          historyCreatedAt={historyData.createdAt}
          historyUsername={historyData.username}
          historyId={historyData.id}
        />
        <CustomControls />

        <ReactFlowCanvas
          nodes={reactFlowNodes}
          edges={reactFlowEdges}
          onNodesChange={() => {}}
          onEdgesChange={() => {}}
          onConnect={() => {}}
          onNodeDragStart={() => {}}
          onNodeDrag={() => {}}
          onNodeDragStop={() => {}}
          schemaInfo={schemaInfo}
        />
      </Box>
    </PermissionProvider>
  );
};
