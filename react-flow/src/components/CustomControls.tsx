// src/components/CustomControls.tsx
import { Box, IconButton, Tooltip, useColorModeValue } from "@chakra-ui/react";
import { MousePointer2, Maximize, ZoomIn, ZoomOut } from "lucide-react";
import { useReactFlow } from "reactflow";
import { useState, useEffect } from "react";
import { usePermission } from "../hooks/usePermission";
import { useWebSocketContext } from "../contexts/WebSocketContext";

export const CustomControls = () => {
  const { zoomIn, zoomOut, fitView, getNodes, setNodes } = useReactFlow();
  const { canEdit } = usePermission();
  const { isConnected } = useWebSocketContext(); // ✅ THÊM để theo dõi trạng thái connection

  const [isInteractive, setIsInteractive] = useState(canEdit);
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const hoverBg = useColorModeValue("white", "#1c1c1c");

  // ✅ Effect 1: Tự động disable khi VIEW mode
  useEffect(() => {
    if (!canEdit) {
      setIsInteractive(false);
      const updatedNodes = getNodes().map((node) => ({
        ...node,
        draggable: false,
        selectable: false,
        connectable: false,
      }));
      setNodes(updatedNodes);
    }
  }, [canEdit, getNodes, setNodes]);

  // ✅ Effect 2: Tự động disable/enable khi WebSocket disconnect/reconnect
  useEffect(() => {
    if (!isConnected && canEdit) {
      // Khi disconnect: tự động disable
      console.log("❌ WebSocket disconnected - disabling interactivity");
      setIsInteractive(false);
      const updatedNodes = getNodes().map((node) => ({
        ...node,
        draggable: false,
        selectable: false,
        connectable: false,
      }));
      setNodes(updatedNodes);
    } else if (isConnected && canEdit) {
      // Khi reconnect: tự động enable lại (nếu user có quyền edit)
      console.log("✅ WebSocket reconnected - enabling interactivity");
      setIsInteractive(true);
      const updatedNodes = getNodes().map((node) => ({
        ...node,
        draggable: true,
        selectable: true,
        connectable: true,
      }));
      setNodes(updatedNodes);
    }
  }, [isConnected, canEdit, getNodes, setNodes]);

  const toggleInteractivity = () => {
    if (!canEdit || !isConnected) return; // ✅ Không cho toggle nếu VIEW mode hoặc disconnected

    const newValue = !isInteractive;
    setIsInteractive(newValue);

    // Enable/disable drag/select/connect on all nodes
    const updatedNodes = getNodes().map((node) => ({
      ...node,
      draggable: newValue,
      selectable: newValue,
      connectable: newValue,
    }));

    setNodes(updatedNodes);
  };

  const buttonStyle = {
    bg: bgColor,
    color: iconColor,
    border: "1px solid",
    borderColor: borderColor,
    _hover: { bg: hoverBg },
    size: "sm" as const,
  };

  return (
    <Box
      position="absolute"
      bottom="10px"
      left="10px"
      display="flex"
      flexDirection="column"
      gap="6px"
      bg="transparent"
      p="6px"
      zIndex={10}
    >
      <Tooltip label="Zoom in" fontSize="sm" placement="left">
        <IconButton
          aria-label="Zoom in"
          icon={<ZoomIn size={16} />}
          onClick={() => zoomIn({ duration: 200 })}
          {...buttonStyle}
        />
      </Tooltip>

      <Tooltip label="Zoom out" fontSize="sm" placement="left">
        <IconButton
          aria-label="Zoom out"
          icon={<ZoomOut size={16} />}
          onClick={() => zoomOut({ duration: 200 })}
          {...buttonStyle}
        />
      </Tooltip>

      <Tooltip label="Fit view" fontSize="sm" placement="left">
        <IconButton
          aria-label="Fit view"
          icon={<Maximize size={16} />}
          onClick={() => fitView({ padding: 0.2, duration: 300 })}
          {...buttonStyle}
        />
      </Tooltip>

      {/* ✅ Toggle Interactivity Button */}
      <Tooltip
        label={
          !canEdit
            ? "View-only mode: editing disabled"
            : !isConnected
            ? "WebSocket disconnected: editing disabled"
            : "Toggle interactivity"
        }
        fontSize="sm"
        placement="left"
      >
        <IconButton
          aria-label="Toggle interactivity"
          icon={<MousePointer2 size={16} />}
          onClick={toggleInteractivity}
          {...buttonStyle}
          opacity={isInteractive ? 1 : 0.5}
          isDisabled={!canEdit || !isConnected} // ✅ Disable khi VIEW mode HOẶC disconnected
          cursor={!canEdit || !isConnected ? "not-allowed" : "pointer"}
        />
      </Tooltip>
    </Box>
  );
};
