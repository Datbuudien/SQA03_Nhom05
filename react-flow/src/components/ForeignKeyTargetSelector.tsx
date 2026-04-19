// ForeignKeyTargetSelector.tsx - WITH LIGHT MODE SUPPORT
import React, { useMemo } from "react";
import { useStore } from "reactflow";
import type { ReactFlowState } from "reactflow";
import {
  Popover,
  PopoverTrigger,
  PopoverContent,
  PopoverBody,
  VStack,
  Text,
  Button,
  Divider,
  useDisclosure,
  useColorModeValue,
} from "@chakra-ui/react";
import { ChevronDown, Link } from "lucide-react";
import { Attribute } from "../SchemaVisualizer/SchemaVisualizer.types";
import { HiOutlineTrash } from "react-icons/hi";

interface PrimaryKeyOption {
  modelId: string;
  modelName: string;
  attributeId: string;
  attributeName: string;
}

interface ForeignKeyTargetSelectorProps {
  currentModelId: string;
  currentAttributeId: string;
  currentConnection?: {
    targetModelId: string;
    targetAttributeId: string;
  };
  onTargetSelect: (targetModelId: string, targetAttributeId: string) => void;
  onDisconnect: () => void;
  inline?: boolean;
}

export const ForeignKeyTargetSelector: React.FC<
  ForeignKeyTargetSelectorProps
> = ({
  currentModelId,
  currentAttributeId,
  currentConnection,
  onTargetSelect,
  onDisconnect,
  inline = false,
}) => {
  const { isOpen, onOpen, onClose } = useDisclosure();

  // 🌟 THEME COLORS
  const bgColor = useColorModeValue("white", "gray.800");
  const borderColor = useColorModeValue("#d0d7de", "gray.600");
  const textColor = useColorModeValue("#24292f", "white");
  const mutedText = useColorModeValue("#57606a", "gray.400");
  // const headingColor = useColorModeValue("black", "blue.300");
  const buttonHoverBg = useColorModeValue("#f6f8fa", "gray.600");
  const activeButtonBg = useColorModeValue("#ddf4ff", "blue.500");
  const dividerColor = useColorModeValue("#d0d7de", "gray.600");
  const deleteButtonColor = useColorModeValue("#cf222e", "red.300");
  const deleteButtonHoverBg = useColorModeValue("#ffebe9", "red.600");
  const triggerButtonColor = useColorModeValue("#0969da", "blue.300");
  const triggerButtonHoverBg = useColorModeValue(
    "#ddf4ff",
    "rgba(74, 144, 226, 0.1)"
  );
  const triggerButtonHoverColor = useColorModeValue("#0550ae", "blue.200");

  const allNodes = useStore((state: ReactFlowState) => state.nodeInternals);

  const primaryKeyOptions: PrimaryKeyOption[] = useMemo(() => {
    const nodesArray = Array.isArray(allNodes)
      ? allNodes
      : Array.from(allNodes.values());

    if (!nodesArray || nodesArray.length === 0) {
      console.warn("⚠️ No nodes in ReactFlow store");
      return [];
    }

    const options: PrimaryKeyOption[] = [];

    nodesArray.forEach((node) => {
      const model = node.data;

      if (!model?.attributes || !Array.isArray(model.attributes)) {
        console.warn(`⚠️ Invalid node data:`, node);
        return;
      }

      model.attributes.forEach((attr: Attribute) => {
        if (attr?.isPrimaryKey === true) {
          options.push({
            modelId: model.id,
            modelName: model.name,
            attributeId: attr.id,
            attributeName: attr.name,
          });
        }
      });
    });

    return options;
  }, [allNodes]);

  const getCurrentTargetDisplay = () => {
    if (!currentConnection) return "Select target...";

    const nodesArray = Array.isArray(allNodes)
      ? allNodes
      : Array.from(allNodes.values());

    const targetNode = nodesArray.find(
      (node) => node.data.id === currentConnection.targetModelId
    );
    const targetAttribute = targetNode?.data.attributes?.find(
      (a: any) => a.id === currentConnection.targetAttributeId
    );

    if (targetNode?.data && targetAttribute) {
      return `${targetNode.data.name}.${targetAttribute.name}`;
    }

    return `${currentConnection.targetModelId}.${currentConnection.targetAttributeId}`;
  };

  const handleTargetSelect = (option: PrimaryKeyOption) => {
    console.log("🔗 Selecting:", `${option.modelName}.${option.attributeName}`);
    onTargetSelect(option.modelId, option.attributeId);
    if (!inline) onClose();
  };

  const handleDisconnect = () => {
    console.log("🔓 Disconnecting FK");
    onDisconnect();
    if (!inline) onClose();
  };

  // Inline mode
  if (inline) {
    return (
      <VStack spacing={2} align="stretch" w="100%">
        <Text fontWeight="600" color={textColor} fontSize="sm">
          Foreign Key Target ({primaryKeyOptions.length} available)
        </Text>

        <Divider borderColor={dividerColor} />

        {primaryKeyOptions.length === 0 ? (
          <Text color={mutedText} fontSize="sm" textAlign="center" py={2}>
            No primary keys available
          </Text>
        ) : (
          <VStack
            spacing={1}
            align="stretch"
            maxHeight="180px"
            overflowY="auto"
          >
            {primaryKeyOptions.map((option) => (
              <Button
                key={`${option.modelId}-${option.attributeId}`}
                size="sm"
                variant="ghost"
                justifyContent="flex-start"
                fontSize="sm"
                color={textColor}
                _hover={{ bg: buttonHoverBg }}
                onClick={() => handleTargetSelect(option)}
                isActive={
                  currentConnection?.targetModelId === option.modelId &&
                  currentConnection?.targetAttributeId === option.attributeId
                }
                _active={{ bg: activeButtonBg, color: textColor }}
              >
                🔑 {option.modelName}.{option.attributeName}
              </Button>
            ))}
          </VStack>
        )}

        {currentConnection && (
          <>
            <Divider borderColor={dividerColor} />
            <Button
              size="sm"
              variant="ghost"
              justifyContent="flex-start"
              fontSize="sm"
              color={deleteButtonColor}
              _hover={{ bg: deleteButtonHoverBg }}
              onClick={handleDisconnect}
              leftIcon={<HiOutlineTrash size={16} />}
            >
              Remove connection
            </Button>
          </>
        )}
      </VStack>
    );
  }

  // Popover mode
  return (
    <Popover
      isOpen={isOpen}
      onOpen={onOpen}
      onClose={onClose}
      placement="right"
    >
      <PopoverTrigger>
        <Button
          size="sm"
          variant="ghost"
          leftIcon={<Link size={12} />}
          rightIcon={<ChevronDown size={12} />}
          fontSize="sm"
          minWidth="140px"
          justifyContent="space-between"
          color={currentConnection ? triggerButtonColor : mutedText}
          _hover={{
            bg: triggerButtonHoverBg,
            color: triggerButtonHoverColor,
          }}
        >
          <Text noOfLines={1} fontSize="sm">
            {getCurrentTargetDisplay()}
          </Text>
        </Button>
      </PopoverTrigger>

      <PopoverContent
        bg={bgColor}
        borderColor={borderColor}
        color={textColor}
        fontSize="sm"
        minWidth="220px"
      >
        <PopoverBody p={3}>
          <VStack spacing={2} align="stretch">
            <Text fontWeight="bold" color={textColor} fontSize="sm">
              Select Primary Key ({primaryKeyOptions.length} available)
            </Text>

            <Divider borderColor={dividerColor} />

            {primaryKeyOptions.length === 0 ? (
              <Text color={mutedText} fontSize="sm" textAlign="center" py={2}>
                No primary keys available
              </Text>
            ) : (
              <VStack spacing={1} align="stretch">
                {primaryKeyOptions.map((option) => (
                  <Button
                    key={`${option.modelId}-${option.attributeId}`}
                    size="sm"
                    variant="ghost"
                    justifyContent="flex-start"
                    fontSize="sm"
                    color={textColor}
                    _hover={{ bg: buttonHoverBg }}
                    onClick={() => handleTargetSelect(option)}
                    isActive={
                      currentConnection?.targetModelId === option.modelId &&
                      currentConnection?.targetAttributeId ===
                        option.attributeId
                    }
                    _active={{ bg: activeButtonBg }}
                  >
                    🔑 {option.modelName}.{option.attributeName}
                  </Button>
                ))}
              </VStack>
            )}

            {currentConnection && (
              <>
                <Divider borderColor={dividerColor} />
                <Button
                  size="sm"
                  variant="ghost"
                  justifyContent="flex-start"
                  fontSize="sm"
                  color={deleteButtonColor}
                  _hover={{ bg: deleteButtonHoverBg }}
                  onClick={handleDisconnect}
                  leftIcon={<HiOutlineTrash size={16} />}
                >
                  Remove connection
                </Button>
              </>
            )}
          </VStack>
        </PopoverBody>
      </PopoverContent>
    </Popover>
  );
};
