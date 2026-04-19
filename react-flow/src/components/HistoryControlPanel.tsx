import React from "react";
import {
  Box,
  Icon,
  Text,
  HStack,
  useColorModeValue,
  Tooltip,
  Badge,
} from "@chakra-ui/react";
import { DiDatabase } from "react-icons/di";
import { MdHistory } from "react-icons/md";

interface HistoryControlPanelProps {
  schemaName: string;
  historyCreatedAt: string;
  historyUsername: string;
  historyId: number;
}

export const HistoryControlPanel: React.FC<HistoryControlPanelProps> = ({
  schemaName,
  historyCreatedAt,
  historyUsername,
  historyId,
}) => {
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const textColor = useColorModeValue("#2d3748", "white");

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <Box
      position="absolute"
      left={4}
      top={4}
      zIndex={1000}
      display="flex"
      flexDirection="row"
      alignItems="center"
      gap={2}
      bg="transparent"
    >
      {/* Icon database */}
      <Tooltip label="Home" placement="bottom" hasArrow>
        <Box
          display="flex"
          alignItems="center"
          justifyContent="center"
          bg={bgColor}
          border="1px solid"
          borderColor={borderColor}
          borderRadius="full"
          p={2}
          onClick={() => (window.location.href = "http://localhost:5173/home")}
          style={{ cursor: "pointer" }}
        >
          <Icon as={DiDatabase} boxSize={6} color={iconColor} />
        </Box>
      </Tooltip>

      {/* Schema name (read-only) */}
      <HStack
        bg={bgColor}
        border="1px solid"
        borderColor={borderColor}
        borderRadius="xl"
        px={3}
        py={1}
        alignItems="center"
        spacing={2}
      >
        <Text
          fontSize="md"
          fontWeight={600}
          color={textColor}
          whiteSpace="nowrap"
          userSelect="none"
        >
          {schemaName}
        </Text>

        {/* <Badge colorScheme="blue" fontSize="xs">
          #{historyId}
        </Badge> */}
      </HStack>

      {/* User & Time info */}
      <HStack
        bg={bgColor}
        border="1px solid"
        borderColor={borderColor}
        borderRadius="xl"
        px={3}
        py={1}
        alignItems="center"
        spacing={2}
        display={{ base: "none", md: "flex" }}
      >
        <Text fontSize="xs" color={textColor} fontWeight={500}>
          by {historyUsername}@gmail.com
        </Text>
        {/* <Text fontSize="xs" color="gray.500">
          •
        </Text> */}
        <Text fontSize="xs" color="gray.500">
          {formatDateTime(historyCreatedAt)}
        </Text>
      </HStack>
    </Box>
  );
};
