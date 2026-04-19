// src/components/TableListDialog.tsx - REFACTORED WITH LIGHT MODE
import React, { useState, useMemo } from "react";
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalCloseButton,
  Box,
  HStack,
  Text,
  Badge,
  Flex,
  VStack,
  useColorModeValue,
} from "@chakra-ui/react";
import { useStore } from "reactflow";
import type { ReactFlowState } from "reactflow";
import { Table } from "lucide-react";
import { Model } from "../SchemaVisualizer/SchemaVisualizer.types";
import { TableListSidebar } from "./TableListSidebar";
import { TableInfo } from "./TableInfo";

interface TableListDialogProps {
  isOpen: boolean;
  onClose: () => void;
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

export const TableListDialog: React.FC<TableListDialogProps> = ({
  isOpen,
  onClose,
  onFieldNameUpdate,
  onFieldTypeUpdate,
  onToggleKeyType,
  onDeleteAttribute,
  onAddAttribute,
  onDeleteModel,
  onModelNameUpdate,
}) => {
  const [selectedTableId, setSelectedTableId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState("");

  // 🌟 THEME COLORS
  const bgColor = useColorModeValue("white", "gray.900");
  const textColor = useColorModeValue("#24292f", "white");
  const borderColor = useColorModeValue("#d0d7de", "gray.700");
  const emptyTextColor = useColorModeValue("#6e7781", "gray.500");
  const mainBg = useColorModeValue("#fafbfc", "gray.900");

  // Get all nodes from ReactFlow store
  const allNodes = useStore((state: ReactFlowState) => state.nodeInternals);

  // Convert to array and filter
  const tables = useMemo(() => {
    const nodesArray = Array.from(allNodes.values());
    return nodesArray
      .filter((node) => node.data?.attributes)
      .map((node) => node.data as Model)
      .filter((model) =>
        model.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
  }, [allNodes, searchQuery]);

  // Get selected table details
  const selectedTable = useMemo(() => {
    return tables.find((table) => table.id === selectedTableId);
  }, [tables, selectedTableId]);

  const handleTableSelect = (tableId: string) => {
    setSelectedTableId(tableId);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="6xl">
      <ModalOverlay />
      <ModalContent
        border={"1px solid"}
        borderColor={borderColor}
        bg={bgColor}
        color={textColor}
        maxH="90vh"
      >
        <ModalHeader borderBottom="1px solid" borderColor={borderColor}>
          <HStack>
            {/* <Table size={20} /> */}
            <Text>Table Manager</Text>
            <Badge colorScheme="blue" ml={2}>
              {tables.length} tables
            </Badge>
          </HStack>
        </ModalHeader>
        <ModalCloseButton />

        <ModalBody p={0}>
          <HStack align="stretch" spacing={0} h="70vh">
            {/* Left Panel - Table List */}
            <TableListSidebar
              tables={tables}
              selectedTableId={selectedTableId}
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              onTableSelect={handleTableSelect}
            />

            {/* Right Panel - Table Details */}
            <Box flex={1} overflowY="auto" bg={mainBg} p={4}>
              {selectedTable ? (
                <TableInfo
                  table={selectedTable}
                  onFieldNameUpdate={onFieldNameUpdate}
                  onFieldTypeUpdate={onFieldTypeUpdate}
                  onToggleKeyType={onToggleKeyType}
                  onDeleteAttribute={onDeleteAttribute}
                  onAddAttribute={onAddAttribute}
                  onDeleteModel={(modelId) => {
                    if (onDeleteModel) {
                      onDeleteModel(modelId);
                      setSelectedTableId(null);
                    }
                  }}
                  onModelNameUpdate={onModelNameUpdate}
                />
              ) : (
                <Flex
                  h="100%"
                  align="center"
                  justify="center"
                  color={emptyTextColor}
                >
                  <VStack>
                    <Table size={48} opacity={0.3} />
                    <Text fontSize="lg">Select a table to view details</Text>
                  </VStack>
                </Flex>
              )}
            </Box>
          </HStack>
        </ModalBody>
      </ModalContent>
    </Modal>
  );
};
