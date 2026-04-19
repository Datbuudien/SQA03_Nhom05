// src/hooks/useNodeHandlers.ts - Fixed version
import { useCallback, useEffect, useRef } from "react";
import { generateAttributeId } from "../utils/uuid.utils";
import { getVietnamTime } from "../utils";

interface UseNodeHandlersProps {
  setReactFlowNodes: any;
  sendFieldNameUpdate: any;
  sendFieldTypeUpdate: any;
  sendToggleKeyType: any;
  sendAddAttribute: any;
  sendDeleteAttribute: any;
  sendForeignKeyConnect: any;
  sendForeignKeyDisconnect: any;
  reactFlowNodes: any;
}

export const useNodeHandlers = ({
  setReactFlowNodes, // ✅ Sử dụng prop này thay vì tạo mới
  sendFieldNameUpdate,
  sendFieldTypeUpdate,
  sendToggleKeyType,
  sendAddAttribute,
  sendDeleteAttribute,
  sendForeignKeyConnect,
  sendForeignKeyDisconnect,
  reactFlowNodes, // ✅ Sử dụng prop này
}: UseNodeHandlersProps) => {
  const reactFlowNodesRef = useRef<any[]>([]);

  // ✅ Cập nhật ref khi reactFlowNodes thay đổi
  useEffect(() => {
    reactFlowNodesRef.current = reactFlowNodes;
  }, [reactFlowNodes]);

  // Field update handler
  const handleFieldNameUpdate = useCallback(
    async (attributeId: string, attributeName: string) => {
      console.log("📤 Sending field update:", {
        attributeId,
        attributeName,
      });

      setReactFlowNodes((currentNodes: any) => {
        return currentNodes.map((node: any) => {
          const hasAttribute = node.data.attributes.some(
            (attr: any) => attr.id === attributeId
          );
          if (!hasAttribute) return node;

          const updatedAttributes = node.data.attributes.map((attr: any) => {
            if (attr.id === attributeId) {
              return {
                ...attr,
                name: attributeName,
                nameUpdatedAt: getVietnamTime(),
              };
            }
            return attr;
          });

          return {
            ...node,
            data: {
              ...node.data,
              attributes: updatedAttributes,
              // ✅ Force re-render
            },
          };
        });
      });
      // Send WebSocket update
      await sendFieldNameUpdate({ attributeId, attributeName });
    },
    [setReactFlowNodes]
  );

  const handleFieldTypeUpdate = useCallback(
    async (attributeId: string, attributeType: string) => {
      console.log("📤 Sending field update:", {
        attributeId,
        attributeType,
      });

      setReactFlowNodes((currentNodes: any) => {
        return currentNodes.map((node: any) => {
          const hasAttribute = node.data.attributes.some(
            (attr: any) => attr.id === attributeId
          );
          if (!hasAttribute) return node;

          const updatedAttributes = node.data.attributes.map((attr: any) => {
            if (attr.id === attributeId) {
              return {
                ...attr,
                dataType: attributeType,
                typeUpdatedAt: getVietnamTime(),
              };
            }
            return attr;
          });

          return {
            ...node,
            data: {
              ...node.data,
              attributes: updatedAttributes,
            },
          };
        });
      });
      // Send WebSocket update
      await sendFieldTypeUpdate({ attributeId, attributeType });
    },
    [setReactFlowNodes]
  );

  // Toggle key type handler
  const handleToggleKeyType = useCallback(
    async (
      modelId: string,
      attributeId: string,
      keyType: "NORMAL" | "PRIMARY" | "FOREIGN"
    ) => {
      console.log("📤 handleToggleKeyType called:", {
        modelId,
        attributeId,
        keyType,
      });

      setReactFlowNodes((currentNodes: any) => {
        const updatedNodes = currentNodes.map((node: any) => {
          if (node.id !== modelId) return node;

          const updatedAttributes = node.data.attributes.map((attr: any) => {
            if (attr.id !== attributeId) return attr;

            // ✅ Handle all 3 cases in one place
            switch (keyType) {
              case "PRIMARY":
                return {
                  ...attr,
                  isPrimaryKey: true,
                  isForeignKey: false,
                  connection: undefined,
                  keyTypeUpdatedAt: getVietnamTime(),
                };
              case "FOREIGN":
                return {
                  ...attr,
                  isPrimaryKey: false,
                  isForeignKey: true,
                  keyTypeUpdatedAt: getVietnamTime(),
                  // Keep existing connection if any
                };
              case "NORMAL":
                return {
                  ...attr,
                  isPrimaryKey: false,
                  isForeignKey: false,
                  connection: undefined,
                  keyTypeUpdatedAt: getVietnamTime(),
                };
              default:
                return attr;
            }
          });

          return {
            ...node,
            data: {
              ...node.data,
              attributes: updatedAttributes,
            },
          };
        });

        return updatedNodes;
      });

      // ✅ Single WebSocket call
      await sendToggleKeyType({ modelId, attributeId, keyType });
    },
    [setReactFlowNodes, sendToggleKeyType]
  );

  // ✅ FIX: Add attribute handler với proper state update
  const handleAddAttribute = useCallback(
    async (modelId: string) => {
      console.log("📤 Adding attribute to:", { modelId });
      const newAttributeId = generateAttributeId();

      const newAttribute = {
        id: newAttributeId,
        name: "new_field",
        dataType: "VARCHAR",
        isNullable: true,
        isPrimaryKey: false,
        isForeignKey: false,
      };

      console.log("🆕 Creating new attribute:", newAttribute);

      // ✅ IMMEDIATELY update UI with proper state management
      setReactFlowNodes((currentNodes: any[]) => {
        const updatedNodes = currentNodes.map((node) => {
          if (node.id !== modelId) return node;

          const updatedAttributes = [...node.data.attributes, newAttribute];
          console.log("updatedAttributes: ", updatedAttributes);
          return {
            ...node,
            data: {
              ...node.data,
              attributes: updatedAttributes,
              // ✅ Force component re-render with timestamp
              lastUpdate: Date.now(),
              lastFieldUpdate: Date.now(),
            },
          };
        });

        return updatedNodes;
      });

      // ✅ Send WebSocket AFTER UI update
      await sendAddAttribute({
        modelId,
        newAttributeId,
        attributeName: "new_field",
        dataType: "VARCHAR",
      });

      console.log("📤 Sent add attribute request to WebSocket");
      return newAttributeId;
    },
    [setReactFlowNodes, sendAddAttribute]
  );

  // Delete attribute handler
  const handleDeleteAttribute = useCallback(
    async (modelId: string, attributeId: string) => {
      // ✅ Đổi từ number sang string
      console.log("📤 Deleting attribute:", { modelId, attributeId });

      setReactFlowNodes((currentNodes: any) => {
        const updatedNodes = currentNodes.map((node: any) => {
          if (node.id !== modelId) return node;

          const filteredAttributes = node.data.attributes.filter(
            (attr: any) => attr.id !== attributeId
          );

          return {
            ...node,
            data: {
              ...node.data,
              attributes: filteredAttributes,
              lastUpdate: Date.now(),
            },
          };
        });

        // Send WebSocket after UI update

        return updatedNodes;
      });
      await sendDeleteAttribute({ modelId, attributeId });
    },

    [setReactFlowNodes, sendDeleteAttribute]
  );

  // Foreign key connection handlers
  const handleForeignKeyTargetSelect = useCallback(
    async (
      attributeId: string, // ✅ Đổi từ number sang string
      targetModelId: string,
      targetAttributeId: string // ✅ Đổi từ number sang string
    ) => {
      console.log("📤 Sending FK connect:", {
        attributeId,
        targetModelId,
        targetAttributeId,
      });

      const foreignKeyName = null;

      setReactFlowNodes((currentNodes: any) => {
        const updatedNodes = currentNodes.map((node: any) => {
          const updatedAttributes = node.data.attributes.map((attr: any) => {
            if (attr.id === attributeId) {
              return {
                ...attr,
                connection: {
                  id: attributeId,
                  targetModelId: targetModelId,
                  targetAttributeId: targetAttributeId,
                  foreignKeyName,
                  strokeColor: "#4A90E2",
                  strokeWidth: 2,
                  isAnimated: true,
                  targetArrowType: "ARROW",
                  connectionType: "MANY_TO_ONE",
                },
              };
            }
            return attr;
          });

          return {
            ...node,
            data: {
              ...node.data,
              attributes: updatedAttributes,
              lastConnectionUpdate: Date.now(),
            },
          };
        });

        return updatedNodes;
      });
      await sendForeignKeyConnect({
        attributeId,
        targetModelId,
        targetAttributeId,
        foreignKeyName,
      });
    },
    [setReactFlowNodes, sendForeignKeyConnect]
  );

  const handleForeignKeyDisconnect = useCallback(
    (attributeId: string) => {
      // ✅ Đổi từ number sang string
      console.log("📤 Sending FK disconnect:", { attributeId });

      setReactFlowNodes((currentNodes: any) => {
        const updatedNodes = currentNodes.map((node: any) => {
          const updatedAttributes = node.data.attributes.map((attr: any) => {
            if (attr.id === attributeId) {
              return {
                ...attr,
                connection: undefined,
              };
            }
            return attr;
          });

          return {
            ...node,
            data: {
              ...node.data,
              attributes: updatedAttributes,
              lastConnectionUpdate: Date.now(),
            },
          };
        });

        sendForeignKeyDisconnect({ attributeId });

        return updatedNodes;
      });
    },
    [setReactFlowNodes, sendForeignKeyDisconnect]
  );

  return {
    handleFieldNameUpdate,
    handleFieldTypeUpdate,
    handleToggleKeyType,
    handleAddAttribute,
    handleDeleteAttribute,
    handleForeignKeyTargetSelect,
    handleForeignKeyDisconnect,
    reactFlowNodesRef,
  };
};
