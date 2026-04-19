import React, { useEffect, useRef, useState } from "react";
import {
  Box,
  Icon,
  Text,
  Input,
  HStack,
  useToast,
  Spinner,
  IconButton,
  useColorModeValue,
  Tooltip,
} from "@chakra-ui/react";
import { Circle, PencilLine, Star } from "lucide-react";
import { DiDatabase } from "react-icons/di";
import { useParams } from "react-router-dom";
import { websocketService } from "../services/websocketService";
import { useWebSocketSender } from "../hooks/useWebSocketSender";
import { useWebSocketContext } from "../contexts/WebSocketContext";

interface ControlPanelProps {
  schemaName: string;
  // isConnected: boolean;
  loading: boolean;
  onReset: () => void;
}

export const ControlPanel: React.FC<ControlPanelProps> = ({
  schemaName,
  // isConnected,
  loading,
  onReset,
}) => {
  const { isConnected } = useWebSocketContext();
  const { diagramId } = useParams();
  const { sendUpdateDiagramName } = useWebSocketSender();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(schemaName);
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const hoverBg = useColorModeValue("#63a3e2ff", "#888");
  const textColor = useColorModeValue("#2d3748", "white"); // 🌟 MÀU CHỮ
  const penColor = useColorModeValue("#2d3748", "white");
  const starColor = useColorModeValue("yellow.400", "yellow.400");
  const editableRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    setName(schemaName);
  }, [schemaName]);

  const handleStartEditing = () => {
    setEditing(true);

    // Đợi DOM render xong
    setTimeout(() => {
      const el = editableRef.current;
      if (el) {
        el.focus();

        // Đặt con trỏ vào cuối text
        const range = document.createRange();
        range.selectNodeContents(el);
        range.collapse(false); // false = cuối
        const sel = window.getSelection();
        if (sel) {
          sel.removeAllRanges();
          sel.addRange(range);
        }
      }
    }, 0);
  };

  const handleSave = async (newText: string) => {
    if (!newText.trim() || saving) return;
    setSaving(true);

    try {
      // ⭐ Gửi qua WebSocket thay vì API
      sendUpdateDiagramName({
        newName: newText,
      });

      setEditing(false);
    } catch (error) {
      console.error("Error updating diagram name:", error);
      toast({
        title: "Cập nhật thất bại",
        status: "error",
        duration: 2000,
        isClosable: true,
      });
    } finally {
      setSaving(false);
    }
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

      {/* Schema name (editable) */}
      <HStack
        bg={bgColor}
        border={editing ? "1px solid" : "1px solid"}
        borderColor={editing ? hoverBg : borderColor}
        borderRadius="xl"
        px={3}
        py={1}
        alignItems="center"
        spacing={1}
        transition="all 0.2s ease"
      >
        <Box
          as="span"
          ref={editableRef}
          contentEditable={editing}
          suppressContentEditableWarning={true}
          onBlur={(e) => {
            const newText = e.currentTarget.textContent?.trim() || "";
            if (newText && newText !== name) {
              setName(newText);
              handleSave(newText);
            }
            setEditing(false);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              e.currentTarget.blur();
            }
            if (e.key === "Escape") {
              setEditing(false);
            }
          }}
          cursor={editing ? "text" : "default"}
          outline="none"
          fontSize="md"
          fontWeight={600}
          color={textColor}
          whiteSpace="nowrap"
          minW="80px"
          userSelect={"none"}
        >
          {name}
        </Box>

        <IconButton
          aria-label="Edit name"
          icon={saving ? <Spinner size="xs" /> : <PencilLine size={16} />}
          size="xs"
          variant="ghost"
          bg={"transparent"}
          // colorScheme="whiteAlpha"
          color={"gray.400"}
          _hover={{ bg: "transparent", color: penColor }}
          onClick={handleStartEditing}
        />
      </HStack>

      {/* Connection status */}
      <HStack px={1} py={1} alignItems="center" spacing={2}>
        <Icon
          as={Circle}
          boxSize={2}
          color={isConnected ? "green.400" : "red.400"}
          fill={isConnected ? "green.400" : "red.400"}
        />
        <Text
          display={{ base: "none", md: "block" }}
          fontSize="sm"
          fontWeight={500}
          color={textColor}
          userSelect="none"
        >
          {isConnected ? "Connected" : "Disconnected"}
        </Text>
      </HStack>
    </Box>
  );
};
