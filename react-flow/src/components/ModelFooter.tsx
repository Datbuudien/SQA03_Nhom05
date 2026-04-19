// src/components/ModelFooter.tsx
import React from "react";
import {
  Box,
  Flex,
  IconButton,
  Tooltip,
  useColorModeValue,
} from "@chakra-ui/react";
import { Plus } from "lucide-react";
import { Model } from "../SchemaVisualizer/SchemaVisualizer.types";

interface ModelFooterProps {
  model: Model;
  onAddAttribute: (modelId: string) => void;
}

export const ModelFooter: React.FC<ModelFooterProps> = ({
  model,
  onAddAttribute,
}) => {
  // 🌟 THÊM THEME COLORS
  const footerBg = useColorModeValue("#4a90e2", "#3d5787");
  const addHoverBg = useColorModeValue("#377dc9ff", "#1f4180ff");

  const attributeCount = model.attributes?.length;
  const primaryKeys = model.attributes.filter(
    (attr) => attr.isPrimaryKey
  ).length;
  const foreignKeys = model.attributes.filter(
    (attr) => attr.isForeignKey
  ).length;

  return (
    <Tooltip label="Add new attribute" fontSize="xs">
      <Box onClick={() => onAddAttribute(model.id)}>
        <Flex
          bg={footerBg} // 🌟 THAY ĐỔI
          px={2}
          py={1}
          fontSize="10px"
          color="rgba(255,255,255,0.9)"
          justifyContent="center"
          alignItems="center"
          borderRadius="0 0 6px 6px"
          position="relative"
          _hover={{
            cursor: "pointer",
            bg: addHoverBg,
          }}
        >
          {/* <Flex alignItems="center" gap={2}>
          <Box>{attributeCount} fields</Box>
          {primaryKeys > 0 && <Box>🔑 {primaryKeys}</Box>}
          {foreignKeys > 0 && <Box>🔗 {foreignKeys}</Box>}
        </Flex> */}

          <IconButton
            aria-label="Add attribute"
            icon={<Plus size={13} strokeWidth={3} />}
            mr={1}
            size="xs"
            variant="ghost"
            minWidth="16px"
            height="16px"
            p={0}
            color="white"
            bg={"transparent"}
            _hover={{
              bg: "transparent",
            }}
          />
        </Flex>
      </Box>
    </Tooltip>
  );
};
