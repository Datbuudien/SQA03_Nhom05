// src/components/ExportDiagramButton.tsx
import {
  Box,
  IconButton,
  Tooltip,
  useColorModeValue,
  useDisclosure,
} from "@chakra-ui/react";
import { ArrowUpFromLine } from "lucide-react";
import { ExportDialog } from "./ExportDialog";

interface ExportButtonProps {
  schemaData?: any;
}

export const ExportDiagramButton: React.FC<ExportButtonProps> = ({
  schemaData,
}) => {
  const { isOpen, onOpen, onClose } = useDisclosure();
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const hoverBg = useColorModeValue("white", "#1c1c1c");
  return (
    <>
      <Tooltip label="Export diagram" fontSize="sm" placement="left">
        <IconButton
          aria-label="Export"
          icon={<ArrowUpFromLine size={16} />}
          size="sm"
          onClick={onOpen}
          bg={bgColor}
          color={iconColor}
          border="1px solid"
          borderColor={borderColor}
          _hover={{ bg: hoverBg }}
        />
      </Tooltip>

      <ExportDialog isOpen={isOpen} onClose={onClose} schemaData={schemaData} />
    </>
  );
};
