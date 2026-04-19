import {
  Button,
  IconButton,
  Tooltip,
  useColorModeValue,
} from "@chakra-ui/react";
import { List, Pencil } from "lucide-react";

interface TableListButtonProps {
  onClick: () => void;
}

export const TableListButton: React.FC<TableListButtonProps> = ({
  onClick,
}) => {
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");
  const hoverBg = useColorModeValue("white", "#1c1c1c");
  return (
    <Tooltip label="View all tables" fontSize="sm" placement="left">
      <IconButton
        aria-label="List Tables"
        icon={<Pencil size={16} />}
        size="sm"
        onClick={onClick}
        bg={bgColor}
        color={iconColor}
        border="1px solid"
        borderColor={borderColor}
        _hover={{ bg: hoverBg }}
      >
        Tables
      </IconButton>
    </Tooltip>
  );
};
