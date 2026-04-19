// src/components/ModelHeader.tsx
import React, { useState } from "react";
import {
  Box,
  Flex,
  IconButton,
  Tooltip,
  useColorModeValue,
} from "@chakra-ui/react";
import { X } from "lucide-react";
import { EditableField } from "./EditableField";
import { Model } from "../SchemaVisualizer/SchemaVisualizer.types";

interface ModelHeaderProps {
  model: Model;
  onModelNameUpdate?: (newName: string) => void;
  onDeleteModel?: () => void;
  canDelete?: boolean;
}

const HEADER_HEIGHT = 40;

export const ModelHeader: React.FC<ModelHeaderProps> = ({
  model,
  onModelNameUpdate,
  onDeleteModel,
  canDelete = true,
}) => {
  const [isHovered, setIsHovered] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // 🌟 THÊM THEME COLORS
  const headerBg = useColorModeValue("#4a90e2", "#3d5787");
  const deleteHoverBg = useColorModeValue("red.300", "red.600");

  const handleNameUpdate = (newName: string) => {
    if (onModelNameUpdate) {
      onModelNameUpdate(newName);
    }
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (onDeleteModel) {
      onDeleteModel();
    }
  };

  return (
    <Box
      p={3}
      borderRadius="6px 6px 0 0"
      bg={headerBg} // 🌟 THAY ĐỔI
      height={`${HEADER_HEIGHT}px`}
      display="flex"
      alignItems="center"
      justifyContent={"space-between"}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      fontWeight={"700"}
    >
      <Flex gap={2} width="100%">
        <Box flex={1}>
          <EditableField
            value={model.name}
            onSave={handleNameUpdate}
            placeholder="Table Name"
            color="white"
            minWidth="100px"
            maxWidth="200px"
            isEditing={isEditing}
            onEditingChange={setIsEditing}
          />
        </Box>
      </Flex>

      <Box zIndex={10} pb={1}>
        <Tooltip label="Delete table" fontSize="xs">
          <IconButton
            aria-label="Delete table"
            icon={<X size={16} strokeWidth={3} />}
            size="xs"
            variant="ghost"
            color="white"
            // color={"red.100"}
            onClick={handleDelete}
            minWidth="16px"
            height="16px"
            p={0}
            _hover={{ bg: deleteHoverBg }} // 🌟 THAY ĐỔI
          />
        </Tooltip>
      </Box>
    </Box>
  );
};
