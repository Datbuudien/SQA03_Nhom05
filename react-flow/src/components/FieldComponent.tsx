// src/components/FieldComponent.tsx - FIXED DRAG INTERFERENCE
import React, { useEffect, useRef, useState } from "react";
import {
  Box,
  Flex,
  IconButton,
  Tooltip,
  Button,
  useColorModeValue,
} from "@chakra-ui/react";
import { Handle, Position } from "reactflow";
import { EditableField } from "./EditableField";
import { ForeignKeyTargetSelector } from "./ForeignKeyTargetSelector";
import { Attribute, Model } from "../SchemaVisualizer/SchemaVisualizer.types";
import { X } from "lucide-react";

interface FieldComponentProps {
  attribute: Attribute;
  model: Model;
  fieldIndex: number;
  onFieldNameUpdate: (fieldIndex: number, newName: string) => void;
  onFieldTypeUpdate: (fieldIndex: number, newType: string) => void;
  onToggleKeyType: (
    modelId: string,
    attributeId: string,
    newKeyType: "NORMAL" | "PRIMARY" | "FOREIGN"
  ) => void;
  onDeleteAttribute: (attributeId: string) => void;
  onForeignKeyTargetSelect: (
    attributeId: string,
    targetModelId: string,
    targetAttributeId: string
  ) => void;
  onForeignKeyDisconnect: (attributeId: string) => void;
}

const ROW_HEIGHT = 32;
type KeyType = "NORMAL" | "PRIMARY" | "FOREIGN";

export const FieldComponent: React.FC<FieldComponentProps> = ({
  attribute,
  model,
  fieldIndex,
  onFieldNameUpdate,
  onFieldTypeUpdate,
  onToggleKeyType,
  onDeleteAttribute,
  onForeignKeyTargetSelect,
  onForeignKeyDisconnect,
}) => {
  const [isHovered, setIsHovered] = useState(false);
  const [showFKSelector, setShowFKSelector] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const fieldBg = useColorModeValue("#ffffff", "#2A2A2A");
  const fieldHoverBg = useColorModeValue("#f0f0f0", "#4A5568");
  const fieldBorder = useColorModeValue("#e2e8f0", "#4A5568");
  const textColor = useColorModeValue("#2d3748", "white");
  const iconBorderColor = useColorModeValue("#aeaeaeff", "#7d7d7dff");
  const iconHoverColor = useColorModeValue("#4A90E2", "#4A90E2");

  // ✅ Click outside handler
  useEffect(() => {
    const handleClickOutside = (event: PointerEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        console.log("🔒 Closing FK selector due to outside click");
        setShowFKSelector(false);
      }
    };

    if (showFKSelector) {
      document.addEventListener("pointerdown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("pointerdown", handleClickOutside);
    };
  }, [showFKSelector]);

  const isPK = attribute.isPrimaryKey;
  const isFK = attribute.isForeignKey;

  const getCurrentKeyType = (): KeyType => {
    if (attribute.isPrimaryKey) return "PRIMARY";
    if (attribute.isForeignKey) return "FOREIGN";
    return "NORMAL";
  };

  const getNextKeyType = (current: KeyType): KeyType => {
    switch (current) {
      case "NORMAL":
        return "PRIMARY";
      case "PRIMARY":
        return "FOREIGN";
      case "FOREIGN":
        return "NORMAL";
      default:
        return "NORMAL";
    }
  };

  const getFieldColor = () => {
    if (isPK) return useColorModeValue("#ffb300ff", "#FFD700");
    if (isFK) return useColorModeValue("rgba(34, 107, 232, 1)", "#87CEEB");
    return useColorModeValue("#2d3748", "white");
  };

  const getFieldIcon = () => {
    if (isPK) return "🔑";
    if (isFK) return "🔗";
    return null;
  };

  const getTooltipText = () => {
    const currentType = getCurrentKeyType();
    const nextType = getNextKeyType(currentType);

    switch (nextType) {
      case "PRIMARY":
        return "Click to set as Primary Key";
      case "FOREIGN":
        return "Click to set as Foreign Key";
      case "NORMAL":
        return "Click to remove key status";
      default:
        return "Click to toggle key type";
    }
  };

  const handleDeleteClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDeleteAttribute(attribute.id);
  };

  const handleIconClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    const currentType = getCurrentKeyType();
    const nextType = getNextKeyType(currentType);
    onToggleKeyType(model.id, attribute.id, nextType);
  };

  const handleRightClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    // Chỉ cho phép mở FK selector nếu là Foreign Key thuần túy (không phải PK)
    if (isFK && !isPK) {
      const newState = !showFKSelector;
      console.log("🔓 Opening FK selector via right click");
      setShowFKSelector(newState);
    }
  };

  const handleFKSelectorClose = () => {
    console.log("🔒 Manually closing FK selector");
    setShowFKSelector(false);
  };

  const handleForeignKeyTargetSelectLocal = (
    targetModelId: string,
    targetAttributeId: string
  ) => {
    console.log("🔗 FK target selected:", { targetModelId, targetAttributeId });
    onForeignKeyTargetSelect(attribute.id, targetModelId, targetAttributeId);
    setShowFKSelector(false);
  };

  const handleForeignKeyDisconnectLocal = () => {
    console.log("🔓 FK disconnected");
    onForeignKeyDisconnect(attribute.id);
    setShowFKSelector(false);
  };

  // ✅ Create handles
  const createHandles = () => {
    const handleBg = useColorModeValue("#fff", "#FFFFFFFF");
    const handleBorder = useColorModeValue("#cbd5e0", "#718096");
    const pkBg = useColorModeValue("#ff9100ff", "#FFD700");

    const baseStyle = {
      width: "8px",
      height: "8px",
      border: "1px solid",
      borderColor: handleBorder,
      borderRadius: "50%",
      opacity: 0.6,
      backgroundColor: handleBg,
    };

    const pkStyle = {
      ...baseStyle,
      opacity: 1,
      backgroundColor: isPK ? pkBg : isFK ? "#1770d6ff" : handleBg,
    };

    const baseHandleId = `${model.id}-${attribute.id}`;

    return (
      <>
        <Handle
          id={`${baseHandleId}-left`}
          position={Position.Left}
          type="source"
          style={{
            ...pkStyle,
            position: "absolute",
            left: "-5px",
            top: "50%",
            transform: "translateY(-50%)",
            zIndex: 1,
          }}
        />
        <Handle
          id={`${baseHandleId}-left-target`}
          position={Position.Left}
          type="target"
          style={{
            ...pkStyle,
            position: "absolute",
            left: "-5px",
            top: "50%",
            transform: "translateY(-50%)",
            zIndex: 1,
          }}
        />
        <Handle
          id={`${baseHandleId}-right`}
          position={Position.Right}
          type="source"
          style={{
            ...pkStyle,
            position: "absolute",
            right: "-5px",
            top: "50%",
            transform: "translateY(-50%)",
            zIndex: 1,
          }}
        />
        <Handle
          id={`${baseHandleId}-right-target`}
          position={Position.Right}
          type="target"
          style={{
            ...pkStyle,
            position: "absolute",
            right: "-5px",
            top: "50%",
            transform: "translateY(-50%)",
            zIndex: 1,
          }}
        />
      </>
    );
  };

  return (
    <Box
      position="relative"
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <Flex
        bg={fieldBg}
        // justifyContent="space-between"
        alignItems="center"
        p={2}
        color={textColor}
        height={`${ROW_HEIGHT}px`}
        borderBottom="1px solid"
        borderColor={fieldBorder}
        _hover={{ bg: fieldHoverBg }}
        position="relative"
      >
        {createHandles()}

        {/* Field Icon */}
        <Box width="20px" mr={2} ml={2} display="flex" justifyContent="center">
          {getFieldIcon() && (
            <Tooltip label={getTooltipText()} fontSize="xs">
              <Box
                fontSize="12px"
                cursor="pointer"
                onClick={handleIconClick}
                onContextMenu={isFK && !isPK ? handleRightClick : undefined}
                _hover={{ opacity: 0.7, transform: "scale(1.1)" }}
                transition="all 0.2s ease-in-out"
                display="flex"
                alignItems="center"
                justifyContent="center"
                width="16px"
                height="16px"
                title={
                  isFK && !isPK
                    ? "Left click: toggle type | Right click: FK selector"
                    : getTooltipText()
                }
              >
                {getFieldIcon()}
              </Box>
            </Tooltip>
          )}
          {!getFieldIcon() && (
            <Tooltip label={getTooltipText()} fontSize="xs">
              <Box
                width="12px"
                height="12px"
                border="1px dashed"
                borderColor={iconBorderColor}
                borderRadius="2px"
                cursor="pointer"
                onClick={handleIconClick}
                _hover={{
                  borderColor: iconHoverColor,
                  backgroundColor: "rgba(74, 144, 226, 0.1)",
                }}
                transition="all 0.2s ease-in-out"
              />
            </Tooltip>
          )}
        </Box>

        {/* Field Name */}
        <Box width="140px" mr={0}>
          <EditableField
            value={attribute.name}
            onSave={(newName) => onFieldNameUpdate(fieldIndex, newName)}
            placeholder="field_name"
            color={getFieldColor()}
            minWidth="140px"
            maxWidth="140px"
          />
        </Box>

        {/* Field Type */}
        <Box width="70px">
          <EditableField
            value={attribute.dataType}
            onSave={(newType) => onFieldTypeUpdate(fieldIndex, newType)}
            placeholder="type"
            color={useColorModeValue("#718096", "#B8B8B8")}
            minWidth="70px"
            maxWidth="70px"
          />
        </Box>

        {/* Delete Button */}
        {isHovered && (
          <Box position="absolute" right="2px" top="2px" zIndex={10} mr={1.5}>
            <Tooltip label="Delete attribute" fontSize="xs">
              <IconButton
                aria-label="Delete attribute"
                icon={<X size={12} />}
                size="xs"
                variant="ghost"
                colorScheme="red"
                onClick={handleDeleteClick}
                minWidth="14px"
                height="14px"
                p={0}
                _hover={{ bg: "red.300" }}
              />
            </Tooltip>
          </Box>
        )}
      </Flex>

      {/* FK Selector Popup - FIXED EVENT HANDLING */}
      {showFKSelector && isFK && !isPK && (
        <Box
          ref={dropdownRef}
          position="absolute"
          top="100%"
          left="0"
          zIndex={99999}
          bg={useColorModeValue("white", "gray.800")}
          border="2px solid"
          borderColor={useColorModeValue("#0969da", "blue.500")}
          borderRadius="md"
          p={2}
          minWidth="200px"
          boxShadow="0 10px 40px rgba(0,0,0,0.3)"
          onClick={(e) => {
            console.log("📍 FK Selector Box clicked");
            e.stopPropagation();
            e.preventDefault();
          }}
          onMouseDown={(e) => {
            console.log("📍 FK Selector Box mouseDown");
            e.stopPropagation();
            e.preventDefault();
          }}
          onMouseUp={(e) => {
            console.log("📍 FK Selector Box mouseUp");
            e.stopPropagation();
            e.preventDefault();
          }}
          onPointerDown={(e) => {
            console.log("📍 FK Selector Box pointerDown");
            e.stopPropagation();
            e.preventDefault();
          }}
          onPointerUp={(e) => {
            console.log("📍 FK Selector Box pointerUp");
            e.stopPropagation();
            e.preventDefault();
          }}
          onDragStart={(e) => {
            console.log("📍 FK Selector Box dragStart - PREVENTED");
            e.stopPropagation();
            e.preventDefault();
          }}
        >
          <ForeignKeyTargetSelector
            key={`fk-selector-${model.id}-${attribute.id}-${Date.now()}`}
            currentModelId={model.id}
            currentAttributeId={attribute.id}
            currentConnection={
              attribute.connection
                ? {
                    targetModelId: attribute.connection.targetModelId,
                    targetAttributeId: attribute.connection.targetAttributeId,
                  }
                : undefined
            }
            onTargetSelect={handleForeignKeyTargetSelectLocal}
            onDisconnect={handleForeignKeyDisconnectLocal}
            inline={true}
          />

          {/* <Button
            size="xs"
            position="absolute"
            top="2px"
            right="2px"
            variant="ghost"
            onClick={(e) => {
              console.log("❌ Close button clicked");
              e.stopPropagation();
              e.preventDefault();
              handleFKSelectorClose();
            }}
            onMouseDown={(e) => {
              console.log("❌ Close button mouseDown");
              e.stopPropagation();
              e.preventDefault();
            }}
          >
            ✕
          </Button> */}
        </Box>
      )}
    </Box>
  );
};
