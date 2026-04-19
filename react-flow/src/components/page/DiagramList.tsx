import {
  Box,
  SimpleGrid,
  VStack,
  Flex,
  Text,
  useColorModeValue,
} from "@chakra-ui/react";
import { DiagramCard } from "./DiagramCard";
import { DiagramListItem } from "./DiagramListItem";

export interface Diagram {
  id: string;
  name: string;
  owner: {
    name: string;
    avatar?: string;
  };
  updatedAt: string;
  updatedBy: {
    name: string;
    avatar?: string;
  };
  image: string;
  createdAt: string;
  isStarred?: boolean;
}

interface DiagramListProps {
  diagrams: Diagram[];
  view: "grid" | "list";
  onOpen: (id: string) => void;
  onShare: (id: string) => void;
  onRename: (id: string) => void;
  onToggleStar: (id: string) => void;
  onDuplicate: (id: string) => void;
  onDownload: (id: string) => void;
  onHistory: (id: string) => void;
  onDelete: (id: string) => void;
}

export function DiagramList({
  diagrams,
  view,
  onOpen,
  onShare,
  onRename,
  onToggleStar,
  onDuplicate,
  onDownload,
  onHistory,
  onDelete,
}: DiagramListProps) {
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const mutedText = useColorModeValue("#57606a", "#8b949e");

  if (view === "grid") {
    return (
      <SimpleGrid columns={{ base: 1, md: 2, lg: 3, xl: 4 }} spacing={6}>
        {diagrams.map((diagram) => (
          <DiagramCard
            key={diagram.id}
            {...diagram}
            onOpen={onOpen}
            onShare={onShare}
            onRename={onRename}
            onToggleStar={onToggleStar}
            onDuplicate={onDuplicate}
            onDownload={onDownload}
            onHistory={onHistory}
            onDelete={onDelete}
          />
        ))}
      </SimpleGrid>
    );
  }

  // List view
  return (
    <Box
      bg={cardBg}
      borderColor={borderColor}
      border="1px"
      borderRadius="md"
      //   overflow="hidden"
    >
      {/* Header - only visible on large screens */}
      <Flex
        display={{ base: "none", md: "flex" }}
        bg={cardBg}
        borderBottom="2px"
        borderColor={borderColor}
        // p={3}
        // align="start"
        // gap={3}
        fontWeight="600"
        fontSize="sm"
        color={mutedText}
      >
        {/* Icon spacer */}
        {/* <Box w="36px" /> */}

        {/* Headers */}
        <Box flex="0 0 25%">Name</Box>
        <Box flex="0 0 25%">Last Modified</Box>
        <Box flex="0 0 10%">Created</Box>
        <Box flex="0 0 10%">Owner</Box>
        <Box flex="0 0 30%" textAlign="left">
          Actions
        </Box>
      </Flex>

      {/* List items */}
      <VStack spacing={0} align="stretch">
        {diagrams.map((diagram) => (
          <DiagramListItem
            key={diagram.id}
            {...diagram}
            onOpen={onOpen}
            onShare={onShare}
            onRename={onRename}
            onToggleStar={onToggleStar}
            onDuplicate={onDuplicate}
            onDownload={onDownload}
            onHistory={onHistory}
            onDelete={onDelete}
          />
        ))}
      </VStack>
    </Box>
  );
}
