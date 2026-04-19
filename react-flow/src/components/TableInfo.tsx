// src/components/TableListDialog/TableInfo.tsx
import React, { useState, useMemo } from "react";
import {
  Box,
  VStack,
  HStack,
  Text,
  IconButton,
  Button,
  Input,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  TableContainer,
  Badge,
  Tooltip,
  useColorModeValue,
} from "@chakra-ui/react";
import { Edit2, Trash2, Link as LinkIcon } from "lucide-react";
import { useStore } from "reactflow";
import type { ReactFlowState } from "reactflow";
import { Model, Attribute } from "../SchemaVisualizer/SchemaVisualizer.types";

interface TableInfoProps {
  table: Model;
  onFieldNameUpdate?: (attributeId: string, newName: string) => void;
  onFieldTypeUpdate?: (attributeId: string, newType: string) => void;
  onToggleKeyType?: (
    modelId: string,
    attributeId: string,
    keyType: "NORMAL" | "PRIMARY" | "FOREIGN"
  ) => void;
  onDeleteAttribute?: (modelId: string, attributeId: string) => void;
  onAddAttribute?: (modelId: string) => void;
  onDeleteModel?: (modelId: string) => void;
  onModelNameUpdate?: (
    modelId: string,
    oldName: string,
    newName: string
  ) => void;
}

export const TableInfo: React.FC<TableInfoProps> = ({
  table,
  onFieldNameUpdate,
  onFieldTypeUpdate,
  onToggleKeyType,
  onDeleteAttribute,
  onAddAttribute,
  onDeleteModel,
  onModelNameUpdate,
}) => {
  const [editingField, setEditingField] = useState<string | null>(null);
  const [editingValue, setEditingValue] = useState("");
  const [editingModelName, setEditingModelName] = useState(false);
  const [modelNameValue, setModelNameValue] = useState(table.name);

  // 🌟 THEME COLORS
  const bgColor = useColorModeValue("white", "gray.800");
  const borderColor = useColorModeValue("#d0d7de", "gray.700");
  const textColor = useColorModeValue("#24292f", "white");
  const mutedText = useColorModeValue("#57606a", "gray.400");
  const headerBg = useColorModeValue("#f6f8fa", "gray.700");
  const rowHoverBg = useColorModeValue("#f6f8fa", "gray.700");
  const inputBg = useColorModeValue("white", "gray.700");
  const pkColor = useColorModeValue("#bf8700", "#FFD700");
  const fkColor = useColorModeValue("#0969da", "#87CEEB");

  // ✅ Get all nodes from ReactFlow store
  const allNodes = useStore((state: ReactFlowState) => state.nodeInternals);

  // ✅ Function to get connection display name
  const getConnectionDisplayName = (connection: Attribute["connection"]) => {
    if (!connection) return null;

    const nodesArray = Array.from(allNodes.values());

    // Find target model
    const targetNode = nodesArray.find(
      (node) => node.data.id === connection.targetModelId
    );

    if (!targetNode?.data) {
      return `${connection.targetModelId}.${connection.targetAttributeId}`;
    }

    // Find target attribute
    const targetAttribute = targetNode.data.attributes?.find(
      (attr: Attribute) => attr.id === connection.targetAttributeId
    );

    if (!targetAttribute) {
      return `${targetNode.data.name}.${connection.targetAttributeId}`;
    }

    return `${targetNode.data.name}.${targetAttribute.name}`;
  };

  const handleFieldEdit = (fieldId: string, currentValue: string) => {
    setEditingField(fieldId);
    setEditingValue(currentValue);
  };

  const handleFieldSave = (attributeId: string, type: "name" | "dataType") => {
    if (!editingValue.trim()) return;

    if (type === "name" && onFieldNameUpdate) {
      onFieldNameUpdate(attributeId, editingValue);
    } else if (type === "dataType" && onFieldTypeUpdate) {
      onFieldTypeUpdate(attributeId, editingValue);
    }

    setEditingField(null);
    setEditingValue("");
  };

  const handleModelNameSave = () => {
    if (!modelNameValue.trim()) return;
    if (modelNameValue !== table.name && onModelNameUpdate) {
      onModelNameUpdate(table.id, table.name, modelNameValue);
    }
    setEditingModelName(false);
  };

  const handleToggleKey = (
    attributeId: string,
    currentType: "NORMAL" | "PRIMARY" | "FOREIGN"
  ) => {
    const nextType: "NORMAL" | "PRIMARY" | "FOREIGN" =
      currentType === "NORMAL"
        ? "PRIMARY"
        : currentType === "PRIMARY"
        ? "FOREIGN"
        : "NORMAL";

    if (onToggleKeyType) {
      onToggleKeyType(table.id, attributeId, nextType);
    }
  };

  const getKeyIcon = (attr: Attribute) => {
    if (attr.isPrimaryKey)
      return { icon: "🔑", color: pkColor, label: "Primary Key" };
    if (attr.isForeignKey)
      return { icon: "🔗", color: fkColor, label: "Foreign Key" };
    return { icon: "○", color: mutedText, label: "Normal Field" };
  };

  return (
    <VStack spacing={4} align="stretch">
      {/* Table Header */}
      <Box
        p={4}
        bg={bgColor}
        borderRadius="md"
        border="1px solid"
        borderColor={borderColor}
      >
        <HStack justify="space-between" mb={2}>
          {editingModelName ? (
            <HStack flex={1}>
              <Input
                value={modelNameValue}
                onChange={(e) => setModelNameValue(e.target.value)}
                size="md"
                autoFocus
                bg={inputBg}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleModelNameSave();
                  if (e.key === "Escape") setEditingModelName(false);
                }}
              />
              <Button
                size="sm"
                colorScheme="green"
                onClick={handleModelNameSave}
              >
                Save
              </Button>
              <Button size="sm" onClick={() => setEditingModelName(false)}>
                Cancel
              </Button>
            </HStack>
          ) : (
            <>
              <HStack>
                <Text fontSize="2xl" fontWeight="bold" color={textColor}>
                  {table.name}
                </Text>
                <IconButton
                  aria-label="Edit table name"
                  icon={<Edit2 size={16} />}
                  size="sm"
                  variant="ghost"
                  onClick={() => setEditingModelName(true)}
                />
              </HStack>
              <Tooltip label="Delete table">
                <IconButton
                  aria-label="Delete table"
                  icon={<Trash2 size={16} />}
                  size="sm"
                  colorScheme="red"
                  variant="ghost"
                  onClick={() => {
                    if (
                      onDeleteModel &&
                      window.confirm(`Delete table "${table.name}"?`)
                    ) {
                      onDeleteModel(table.id);
                    }
                  }}
                />
              </Tooltip>
            </>
          )}
        </HStack>

        <HStack spacing={4} fontSize="sm" color={mutedText}>
          <Text>ID: {table.id}</Text>
          <Text>Type: {table.modelType}</Text>
          <Text>Fields: {table.attributes?.length || 0}</Text>
        </HStack>
      </Box>

      {/* Fields Section */}
      <Box>
        <HStack justify="space-between" mb={3}>
          <Text fontSize="lg" fontWeight="bold" color={textColor}>
            Fields
          </Text>
          {onAddAttribute && (
            <Button
              size="sm"
              colorScheme="green"
              onClick={() => onAddAttribute(table.id)}
            >
              Add Field
            </Button>
          )}
        </HStack>

        <TableContainer
          border="1px solid"
          borderColor={borderColor}
          borderRadius="md"
          overflowX="auto"
        >
          <Table
            variant="simple"
            size="sm"
            sx={{ tableLayout: "fixed", width: "100%" }}
          >
            <Thead bg={headerBg}>
              <Tr>
                <Th width="60px" color={textColor}>
                  Type
                </Th>
                <Th width="200px" color={textColor}>
                  Name
                </Th>
                <Th width="150px" color={textColor}>
                  Data Type
                </Th>
                <Th width="250px" color={textColor}>
                  Connection
                </Th>
                <Th width="200px" color={textColor}>
                  Attributes
                </Th>
                <Th width="80px" color={textColor}>
                  Actions
                </Th>
              </Tr>
            </Thead>
            <Tbody>
              {table.attributes?.map((attr) => {
                const keyInfo = getKeyIcon(attr);
                return (
                  <Tr key={attr.id} _hover={{ bg: rowHoverBg }}>
                    {/* Type Column */}
                    <Td>
                      <Tooltip label={keyInfo.label}>
                        <Box
                          fontSize="16px"
                          cursor="pointer"
                          onClick={() => {
                            const currentType = attr.isPrimaryKey
                              ? "PRIMARY"
                              : attr.isForeignKey
                              ? "FOREIGN"
                              : "NORMAL";
                            handleToggleKey(attr.id, currentType);
                          }}
                          display="flex"
                          justifyContent="center"
                          _hover={{ transform: "scale(1.2)" }}
                          transition="all 0.2s"
                        >
                          {keyInfo.icon}
                        </Box>
                      </Tooltip>
                    </Td>

                    {/* Name Column */}
                    <Td width="200px">
                      {editingField === `${attr.id}-name` ? (
                        <HStack>
                          <Input
                            value={editingValue}
                            onChange={(e) => setEditingValue(e.target.value)}
                            size="sm"
                            autoFocus
                            bg={inputBg}
                            onKeyDown={(e) => {
                              if (e.key === "Enter")
                                handleFieldSave(attr.id, "name");
                              if (e.key === "Escape") setEditingField(null);
                            }}
                          />
                          <Button
                            size="xs"
                            colorScheme="green"
                            onClick={() => handleFieldSave(attr.id, "name")}
                          >
                            ✓
                          </Button>
                        </HStack>
                      ) : (
                        <HStack>
                          <Text fontWeight="bold" color={keyInfo.color}>
                            {attr.name}
                          </Text>
                          <IconButton
                            aria-label="Edit field name"
                            icon={<Edit2 size={12} />}
                            size="xs"
                            variant="ghost"
                            onClick={() =>
                              handleFieldEdit(`${attr.id}-name`, attr.name)
                            }
                          />
                        </HStack>
                      )}
                    </Td>

                    {/* Data Type Column */}
                    <Td>
                      {editingField === `${attr.id}-type` ? (
                        <HStack>
                          <Input
                            value={editingValue}
                            onChange={(e) => setEditingValue(e.target.value)}
                            size="sm"
                            autoFocus
                            bg={inputBg}
                            onKeyDown={(e) => {
                              if (e.key === "Enter")
                                handleFieldSave(attr.id, "dataType");
                              if (e.key === "Escape") setEditingField(null);
                            }}
                          />
                          <Button
                            size="xs"
                            colorScheme="green"
                            onClick={() => handleFieldSave(attr.id, "dataType")}
                          >
                            ✓
                          </Button>
                        </HStack>
                      ) : (
                        <HStack>
                          <Text color="blue.400">{attr.dataType}</Text>
                          <IconButton
                            aria-label="Edit field type"
                            icon={<Edit2 size={12} />}
                            size="xs"
                            variant="ghost"
                            onClick={() =>
                              handleFieldEdit(`${attr.id}-type`, attr.dataType)
                            }
                          />
                        </HStack>
                      )}
                    </Td>

                    {/* Connection Column */}
                    <Td>
                      {attr.connection ? (
                        <HStack>
                          <Text color="blue.400" fontSize="sm">
                            → {getConnectionDisplayName(attr.connection)}
                          </Text>
                        </HStack>
                      ) : (
                        <Text color={mutedText} fontSize="sm">
                          —
                        </Text>
                      )}
                    </Td>

                    {/* Attributes Column */}
                    <Td>
                      <HStack spacing={1} flexWrap="wrap">
                        {attr.isNullable && (
                          <Badge colorScheme="gray" fontSize="xs">
                            Nullable
                          </Badge>
                        )}
                        {attr.isUnique && (
                          <Badge colorScheme="purple" fontSize="xs">
                            Unique
                          </Badge>
                        )}
                        {attr.isAutoIncrement && (
                          <Badge colorScheme="green" fontSize="xs">
                            Auto Inc
                          </Badge>
                        )}
                        {attr.hasIndex && (
                          <Badge colorScheme="orange" fontSize="xs">
                            Indexed
                          </Badge>
                        )}
                      </HStack>
                    </Td>

                    {/* Actions Column */}
                    <Td>
                      <IconButton
                        aria-label="Delete field"
                        icon={<Trash2 size={14} />}
                        size="xs"
                        colorScheme="red"
                        variant="ghost"
                        onClick={() => {
                          if (
                            onDeleteAttribute &&
                            window.confirm(`Delete field "${attr.name}"?`)
                          ) {
                            onDeleteAttribute(table.id, attr.id);
                          }
                        }}
                      />
                    </Td>
                  </Tr>
                );
              })}
            </Tbody>
          </Table>
        </TableContainer>
      </Box>
    </VStack>
  );
};
