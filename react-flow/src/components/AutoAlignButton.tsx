// src/components/AutoAlignButton.tsx
import {
  IconButton,
  Tooltip,
  useColorModeValue,
  useToast,
} from "@chakra-ui/react";
import { Workflow } from "lucide-react";
import { useReactFlow, useStore, Node } from "reactflow";
import { calculateAutoLayout } from "../utils/autoLayout";
import { useWebSocketContext } from "../contexts/WebSocketContext";
import { createNodePositionUpdate } from "../utils/schemaUtils";

interface AutoAlignButtonProps {
  setReactFlowNodes: React.Dispatch<React.SetStateAction<Node[]>>;
  sendNodePositionUpdate: (update: any) => void;
}

export const AutoAlignButton: React.FC<AutoAlignButtonProps> = ({
  setReactFlowNodes,
  sendNodePositionUpdate,
}) => {
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const hoverBg = useColorModeValue("white", "#1c1c1c");

  const toast = useToast();
  const reactFlowInstance = useReactFlow();
  const allNodes = useStore((state) => state.nodeInternals);
  const { isConnected } = useWebSocketContext();

  const handleAutoAlign = () => {
    if (!allNodes || allNodes.size === 0) {
      toast({
        title: "No nodes to align",
        description: "Add some models to the diagram first.",
        status: "info",
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    if (!isConnected) {
      toast({
        title: "Not connected",
        description: "Please wait for connection to be established.",
        status: "warning",
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    try {
      console.log("🎯 Auto-aligning nodes...");

      // Tính toán vị trí mới
      const newPositions = calculateAutoLayout(allNodes, {
        horizontalSpacing: 400,
        verticalSpacing: 80,
        startX: 100,
        startY: 200,
      });

      let updatedCount = 0;

      // Update local state giống như trong handleNodePositionUpdate
      setReactFlowNodes((currentNodes: Node[]) => {
        return currentNodes.map((node: Node) => {
          const newPos = newPositions.get(node.id);

          if (newPos) {
            updatedCount++;

            // Gửi WebSocket update cho từng node
            if (isConnected) {
              const update = createNodePositionUpdate({
                ...node,
                position: { x: newPos.x, y: newPos.y },
              });

              console.log(`📤 Sending position update for ${node.id}:`, newPos);
              sendNodePositionUpdate(update);
            }

            // Return updated node
            return {
              ...node,
              position: { x: newPos.x, y: newPos.y },
              data: {
                ...node.data,
                positionUpdatedAt: new Date().toISOString(),
              },
            };
          }

          return node;
        });
      });

      // Fit view để hiển thị toàn bộ diagram sau khi layout xong
      setTimeout(() => {
        reactFlowInstance.fitView({
          padding: 0.2,
          duration: 800,
        });
      }, 100);

      toast({
        title: "Layout applied",
        description: `Successfully aligned ${updatedCount} nodes.`,
        status: "success",
        duration: 3000,
        isClosable: true,
      });

      console.log(`✅ Auto-aligned ${updatedCount} nodes`);
    } catch (error) {
      console.error("❌ Error during auto-align:", error);
      toast({
        title: "Layout failed",
        description: "An error occurred while aligning nodes.",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    }
  };

  return (
    <Tooltip label={"Auto align"} fontSize="sm" placement="right">
      <IconButton
        aria-label="Auto align"
        icon={<Workflow size={16} />}
        size="sm"
        bg={bgColor}
        color={iconColor}
        border="1px solid"
        borderColor={borderColor}
        _hover={{ bg: hoverBg }}
        _disabled={{
          opacity: 0.4,
          cursor: "not-allowed",
        }}
        onClick={handleAutoAlign}
        isDisabled={!isConnected}
      />
    </Tooltip>
  );
};
