import {
  Box,
  Flex,
  Text,
  HStack,
  IconButton,
  Avatar,
  useColorModeValue,
  Tooltip,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  MenuDivider,
  useBreakpointValue,
} from "@chakra-ui/react";
import {
  Share2,
  Edit2,
  Star,
  Copy,
  Download,
  History,
  Trash2,
  MoreVertical,
} from "lucide-react";
import { BsDiagram3 } from "react-icons/bs";
import { PiStar, PiStarFill } from "react-icons/pi";

interface DiagramListItemProps {
  id: string;
  name: string;
  owner: {
    name: string;
    avatar?: string;
  };
  updatedAt: string;
  updatedBy: {
    name: string;
    avatar?: string;
  };
  createdAt: string;
  isStarred?: boolean;
  onOpen: (id: string) => void;
  onShare: (id: string) => void;
  onRename: (id: string) => void;
  onToggleStar: (id: string) => void;
  onDuplicate: (id: string) => void;
  onDownload: (id: string) => void;
  onHistory: (id: string) => void;
  onDelete: (id: string) => void;
}

export function DiagramListItem({
  id,
  name,
  owner,
  updatedAt,
  updatedBy,
  createdAt,
  isStarred = false,
  onOpen,
  onShare,
  onRename,
  onToggleStar,
  onDuplicate,
  onDownload,
  onHistory,
  onDelete,
}: DiagramListItemProps) {
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const mutedText = useColorModeValue("#57606a", "#8b949e");
  const hoverBg = useColorModeValue("#f6f8fa", "#1c2128");

  // Check if screen is smaller than lg
  const isSmallScreen = useBreakpointValue({ base: true, lg: false });

  const handleRowClick = (e: React.MouseEvent) => {
    // Prevent opening when clicking on buttons
    if ((e.target as HTMLElement).closest("button")) {
      return;
    }
    onOpen(id);
  };

  // Render compact version for small screens (same as grid)
  if (isSmallScreen) {
    return (
      <Box
        bg={cardBg}
        borderColor={borderColor}
        border="1px"
        borderRadius="md"
        p={4}
        cursor="pointer"
        onClick={handleRowClick}
        _hover={{
          bg: hoverBg,
        }}
        transition="all 0.2s"
      >
        <Flex justify="space-between" align="start" mb={3}>
          <HStack spacing={3}>
            <Box
              bg="blue.50"
              _dark={{ bg: "blue.900" }}
              p={2}
              borderRadius="md"
            >
              <BsDiagram3 size={20} color="#3182ce" />
            </Box>
            <Box flex={1}>
              <Text
                fontSize="sm"
                fontWeight="600"
                color={textColor}
                noOfLines={1}
                mb={1}
              >
                {name}
              </Text>
              <HStack spacing={2}>
                <Avatar size="xs" name={owner.name} src={owner.avatar} />
                <Text fontSize="xs" color={mutedText} noOfLines={1}>
                  {owner.name}
                </Text>
              </HStack>
            </Box>
          </HStack>
          <Menu>
            <MenuButton
              as={IconButton}
              aria-label="Options"
              icon={<MoreVertical size={18} />}
              variant="ghost"
              size="sm"
              onClick={(e) => e.stopPropagation()}
            />
            <MenuList bg={cardBg} borderColor={borderColor}>
              <MenuItem
                icon={<Share2 size={16} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onShare(id);
                }}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Share
              </MenuItem>
              <MenuItem
                icon={<Edit2 size={16} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onRename(id);
                }}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Rename
              </MenuItem>
              <MenuItem
                icon={
                  isStarred ? <PiStarFill size={16} /> : <PiStar size={16} />
                }
                onClick={(e) => {
                  e.stopPropagation();
                  onToggleStar(id);
                }}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                {isStarred ? "Remove star" : "Add star"}
              </MenuItem>
              <MenuDivider />
              <MenuItem
                icon={<Copy size={16} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onDuplicate(id);
                }}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Make a copy
              </MenuItem>
              <MenuItem
                icon={<Download size={16} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onDownload(id);
                }}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Download
              </MenuItem>
              <MenuItem
                icon={<History size={16} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onHistory(id);
                }}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Activity history
              </MenuItem>
              <MenuDivider />
              <MenuItem
                icon={<Trash2 size={16} />}
                onClick={(e) => {
                  e.stopPropagation();
                  onDelete(id);
                }}
                color="red.500"
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Move to trash
              </MenuItem>
            </MenuList>
          </Menu>
        </Flex>
        <HStack spacing={2} fontSize="xs" color={mutedText}>
          <Avatar size="2xs" name={updatedBy.name} src={updatedBy.avatar} />
          <Text noOfLines={1}>
            Updated {new Date(updatedAt).toLocaleDateString()} by{" "}
            {updatedBy.name}
          </Text>
        </HStack>
      </Box>
    );
  }

  // Full list view for large screens
  return (
    <Flex
      bg={cardBg}
      borderColor={borderColor}
      borderBottom="1px"
      p={3}
      cursor="pointer"
      onClick={handleRowClick}
      _hover={{
        bg: hoverBg,
      }}
      transition="all 0.2s"
      align="center"
      gap={3}
    >
      {/* Icon */}
      {/* <Box bg="blue.50" _dark={{ bg: "blue.900" }} p={2} borderRadius="md">
        <BsDiagram3 size={20} color="#3182ce" />
      </Box> */}

      {/* Name - 30% */}
      <Box flex="0 0 25%" minW={0}>
        <Text fontSize="sm" fontWeight="600" color={textColor} noOfLines={1}>
          {name}
        </Text>
      </Box>

      {/* Updated - 25% */}
      <Box flex="0 0 25%" minW={0}>
        <HStack spacing={2}>
          <Avatar size="xs" name={updatedBy.name} src={updatedBy.avatar} />
          <Box minW={0}>
            <Text fontSize="xs" color={textColor} noOfLines={1}>
              {updatedBy.name}
            </Text>
            <Text fontSize="xs" color={mutedText}>
              {new Date(updatedAt).toLocaleDateString()}
            </Text>
          </Box>
        </HStack>
      </Box>

      {/* Created - 15% */}
      <Box flex="0 0 10%" minW={0}>
        <Text fontSize="xs" color={mutedText}>
          {new Date(createdAt).toLocaleDateString()}
        </Text>
      </Box>

      {/* Owner - 15% */}
      <Box flex="0 0 10%" minW={0}>
        <HStack spacing={2}>
          <Avatar size="xs" name={owner.name} src={owner.avatar} />
          <Text fontSize="xs" color={mutedText} noOfLines={1}>
            {owner.name}
          </Text>
        </HStack>
      </Box>

      {/* Actions - 15% */}
      <HStack flex="0 0 30%" justify="flex-start" spacing={1}>
        <Tooltip label="Share" placement="top">
          <IconButton
            aria-label="Share"
            icon={<Share2 size={16} />}
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              onShare(id);
            }}
          />
        </Tooltip>
        <Tooltip label="Rename" placement="top">
          <IconButton
            aria-label="Rename"
            icon={<Edit2 size={16} />}
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              onRename(id);
            }}
          />
        </Tooltip>
        <Tooltip label={isStarred ? "Remove star" : "Add star"} placement="top">
          <IconButton
            aria-label="Star"
            icon={isStarred ? <PiStarFill size={16} /> : <PiStar size={16} />}
            variant="ghost"
            size="sm"
            color={isStarred ? "yellow.500" : undefined}
            onClick={(e) => {
              e.stopPropagation();
              onToggleStar(id);
            }}
          />
        </Tooltip>
        <Menu>
          <MenuButton
            as={IconButton}
            aria-label="More options"
            icon={<MoreVertical size={16} />}
            variant="ghost"
            size="sm"
            onClick={(e) => e.stopPropagation()}
          />
          <MenuList bg={cardBg} borderColor={borderColor}>
            <MenuItem
              icon={<Copy size={16} />}
              onClick={(e) => {
                e.stopPropagation();
                onDuplicate(id);
              }}
              bg={cardBg}
              _hover={{ bg: hoverBg }}
            >
              Make a copy
            </MenuItem>
            <MenuItem
              icon={<Download size={16} />}
              onClick={(e) => {
                e.stopPropagation();
                onDownload(id);
              }}
              bg={cardBg}
              _hover={{ bg: hoverBg }}
            >
              Download
            </MenuItem>
            <MenuItem
              icon={<History size={16} />}
              onClick={(e) => {
                e.stopPropagation();
                onHistory(id);
              }}
              bg={cardBg}
              _hover={{ bg: hoverBg }}
            >
              Activity history
            </MenuItem>
            <MenuDivider />
            <MenuItem
              icon={<Trash2 size={16} />}
              onClick={(e) => {
                e.stopPropagation();
                onDelete(id);
              }}
              color="red.500"
              bg={cardBg}
              _hover={{ bg: hoverBg }}
            >
              Move to trash
            </MenuItem>
          </MenuList>
        </Menu>
      </HStack>
    </Flex>
  );
}
