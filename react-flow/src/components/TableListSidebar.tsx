// src/components/TableListDialog/TableListSidebar.tsx
import React from "react";
import {
  Box,
  VStack,
  HStack,
  Text,
  Input,
  Badge,
  useColorModeValue,
} from "@chakra-ui/react";
import { Search } from "lucide-react";
import { Model } from "../SchemaVisualizer/SchemaVisualizer.types";

interface TableListSidebarProps {
  tables: Model[];
  selectedTableId: string | null;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onTableSelect: (tableId: string) => void;
}

export const TableListSidebar: React.FC<TableListSidebarProps> = ({
  tables,
  selectedTableId,
  searchQuery,
  onSearchChange,
  onTableSelect,
}) => {
  // 🌟 THEME COLORS
  const bgColor = useColorModeValue("#f6f8fa", "gray.800");
  const borderColor = useColorModeValue("#d0d7de", "gray.700");
  const textColor = useColorModeValue("#24292f", "white");
  const mutedText = useColorModeValue("#57606a", "gray.400");
  const selectedBg = useColorModeValue("#ddf4ff", "blue.600");
  const hoverBg = useColorModeValue("#f6f8fa", "gray.700");
  const inputBg = useColorModeValue("white", "gray.900");
  const emptyTextColor = useColorModeValue("#6e7781", "gray.500");

  return (
    <Box
      w="200px"
      borderRight="1px solid"
      borderColor={borderColor}
      overflowY="auto"
      bg={bgColor}
    >
      {/* Search Box */}
      <Box p={3} borderBottom="1px solid" borderColor={borderColor}>
        <Input
          placeholder="Search tables..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          size="sm"
          bg={inputBg}
          borderColor={borderColor}
        />
      </Box>

      {/* Table List */}
      <VStack spacing={0} align="stretch">
        {tables.map((table) => (
          <Box
            key={table.id}
            p={3}
            cursor="pointer"
            bg={selectedTableId === table.id ? selectedBg : "transparent"}
            _hover={{
              bg: selectedTableId === table.id ? selectedBg : hoverBg,
            }}
            borderBottom="1px solid"
            borderColor={borderColor}
            onClick={() => onTableSelect(table.id)}
          >
            <HStack justify="space-between">
              <VStack align="start" spacing={0}>
                <Text fontWeight="bold" fontSize="sm" color={textColor}>
                  {table.name}
                </Text>
                <Text fontSize="xs" color={mutedText}>
                  {table.attributes?.length || 0} fields
                </Text>
              </VStack>
              {/* <HStack spacing={1}>
                {table.attributes?.some((a) => a.isPrimaryKey) && (
                  <Badge colorScheme="yellow" fontSize="xs">
                    PK
                  </Badge>
                )}
                {table.attributes?.some((a) => a.isForeignKey) && (
                  <Badge colorScheme="blue" fontSize="xs">
                    FK
                  </Badge>
                )}
              </HStack> */}
            </HStack>
          </Box>
        ))}

        {tables.length === 0 && (
          <Box p={6} textAlign="center" color={emptyTextColor}>
            <Text>No tables found</Text>
          </Box>
        )}
      </VStack>
    </Box>
  );
};
