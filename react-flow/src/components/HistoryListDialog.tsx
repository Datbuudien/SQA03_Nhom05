// src/components/HistoryListDialog.tsx
import React, { useEffect, useState } from "react";
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalCloseButton,
  VStack,
  HStack,
  Text,
  Button,
  useColorModeValue,
  Box,
  Spinner,
  Icon,
  Avatar,
  Divider,
  Badge,
} from "@chakra-ui/react";
import { MdHistory, MdVisibility } from "react-icons/md";
import {
  migrationApiService,
  MigrationHistoryDto,
} from "../services/migrationApiService";
import { formatDistanceToNow } from "date-fns";
import { vi } from "date-fns/locale";

interface HistoryListDialogProps {
  isOpen: boolean;
  onClose: () => void;
  diagramId: string;
}

export const HistoryListDialog: React.FC<HistoryListDialogProps> = ({
  isOpen,
  onClose,
  diagramId,
}) => {
  const [histories, setHistories] = useState<MigrationHistoryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const bgColor = useColorModeValue("white", "#2d3748");
  const borderColor = useColorModeValue("gray.200", "gray.600");
  const hoverBg = useColorModeValue("gray.50", "gray.700");
  const textColor = useColorModeValue("gray.700", "gray.200");
  const mutedColor = useColorModeValue("gray.500", "gray.400");

  useEffect(() => {
    if (isOpen && diagramId) {
      fetchHistories();
    }
  }, [isOpen, diagramId]);

  const fetchHistories = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await migrationApiService.getDiagramHistory(diagramId);
      setHistories(data);
    } catch (err) {
      setError("Failed to load history");
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewHistory = (migrationId: number) => {
    // Mở tab mới với URL history view
    const url = `/history/${migrationId}`;
    window.open(url, "_blank");
  };

  const formatDateTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString("vi-VN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const getRelativeTime = (dateString: string) => {
    try {
      return formatDistanceToNow(new Date(dateString), {
        addSuffix: true,
        locale: vi,
      });
    } catch {
      return "";
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="xl" isCentered>
      <ModalOverlay />
      <ModalContent bg={bgColor} maxH="80vh">
        <ModalHeader
          display="flex"
          alignItems="center"
          gap={2}
          //   borderBottom="1px solid"
          //   borderColor={borderColor}
        >
          {/* <Icon as={MdHistory} boxSize={6} /> */}
          <Text>Diagram History</Text>
        </ModalHeader>
        <ModalCloseButton />

        <ModalBody p={0}>
          {loading ? (
            <Box
              display="flex"
              justifyContent="center"
              alignItems="center"
              py={10}
            >
              <Spinner size="lg" />
            </Box>
          ) : error ? (
            <Box
              display="flex"
              justifyContent="center"
              alignItems="center"
              py={10}
            >
              <Text color="red.500">{error}</Text>
            </Box>
          ) : histories.length === 0 ? (
            <Box
              display="flex"
              flexDirection="column"
              justifyContent="center"
              alignItems="center"
              py={10}
              gap={2}
            >
              <Icon as={MdHistory} boxSize={12} color={mutedColor} />
              <Text color={mutedColor}>No history available</Text>
            </Box>
          ) : (
            <VStack
              p={4}
              pt={0}
              spacing={0}
              align="stretch"
              maxH="240px" // chỉ đủ chỗ cho 1 item
              overflowY="auto" // cuộn phần còn lại
            >
              {histories.map((history, index) => (
                <React.Fragment key={history.id}>
                  <HStack
                    px={2}
                    py={2}
                    spacing={3}
                    cursor="pointer"
                    transition="all 0.2s"
                    _hover={{ bg: hoverBg }}
                    borderTop={"1px"}
                    borderColor={borderColor}
                  >
                    {/* Info */}
                    <VStack align="start" flex={1} spacing={1}>
                      <HStack spacing={2} align="center">
                        <Text fontWeight={600} color={textColor} fontSize="sm">
                          edited by {history.username}@gmail.com
                        </Text>

                        {/* Time on same row */}
                        <Text fontSize="xs" color={mutedColor}>
                          {formatDateTime(history.createdAt)}
                        </Text>
                      </HStack>

                      <Text fontSize="xs" color={mutedColor} fontStyle="italic">
                        {getRelativeTime(history.createdAt)}
                      </Text>
                    </VStack>

                    {/* View Button */}
                    <Button
                      size="sm"
                      variant="ghost"
                      colorScheme="blue"
                      leftIcon={<MdVisibility />}
                      onClick={() => handleViewHistory(history.id)}
                    >
                      View
                    </Button>
                  </HStack>

                  {index < histories.length - 1 && <Divider />}
                </React.Fragment>
              ))}
            </VStack>
          )}
        </ModalBody>
      </ModalContent>
    </Modal>
  );
};
