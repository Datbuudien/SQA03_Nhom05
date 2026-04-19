import {
  Box,
  Button,
  IconButton,
  Tooltip,
  useColorModeValue,
} from "@chakra-ui/react";
import { Plus } from "lucide-react";

interface AddModelButtonProps {
  onAddModel: () => void;
  isConnected: boolean;
}

export const AddModelButton: React.FC<AddModelButtonProps> = ({
  onAddModel,
  isConnected,
}) => {
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const hoverBg = useColorModeValue("white", "#1c1c1c");
  return (
    <Tooltip
      label={isConnected ? "Add new table" : "Connect to add tables"}
      fontSize="sm"
      placement="left"
    >
      <IconButton
        aria-label="Add Table"
        icon={<Plus size={16} />}
        size="sm"
        onClick={onAddModel}
        isDisabled={!isConnected}
        bg={bgColor}
        color={iconColor}
        border="1px solid"
        borderColor={borderColor}
        _hover={{ bg: hoverBg }}
        _disabled={{
          opacity: 0.4,
          cursor: "not-allowed",
        }}
      />
    </Tooltip>
  );
};
