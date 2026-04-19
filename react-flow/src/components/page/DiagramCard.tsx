import { useEffect, useRef } from "react";
import {
  Box,
  Card,
  CardBody,
  Heading,
  Text,
  VStack,
  HStack,
  IconButton,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  MenuDivider,
  Avatar,
  useColorModeValue,
  Tooltip,
  Portal,
  Image,
} from "@chakra-ui/react";
import {
  Share2,
  Edit2,
  Copy,
  Download,
  History,
  Trash2,
  MoreVertical,
} from "lucide-react";
import { PiStar, PiStarFill } from "react-icons/pi";
import * as jdenticon from "jdenticon";

interface DiagramCardProps {
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

export function DiagramCard({
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
}: DiagramCardProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const mutedText = useColorModeValue("#57606a", "#8b949e");
  const hoverBg = useColorModeValue("#f6f8fa", "#323b47ff");
  const bgColor = useColorModeValue("#faf9f9ff", "#2b3039ff");

  // Generate jdenticon when component mounts or name changes
  useEffect(() => {
    if (canvasRef.current) {
      jdenticon.update(canvasRef.current, name, {
        padding: 0.08,
        saturation: {
          color: 0.5,
          grayscale: 0.0,
        },
        lightness: {
          color: [0.4, 0.8],
          grayscale: [0.3, 0.9],
        },
      });
    }
  }, [name]);

  const handleCardClick = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest("button")) {
      return;
    }
    onOpen(id);
  };

  // Format display date - show migration date if exists, otherwise created date
  const formatDisplayDate = () => {
    const date = new Date(updatedAt);
    return date.toLocaleDateString();
  };

  const getDisplayText = () => {
    // If updatedAt is different from createdAt, it means there was a migration
    const updated = new Date(updatedAt);
    const created = new Date(createdAt);

    if (updated.getTime() !== created.getTime()) {
      return `Last modified ${formatDisplayDate()} by ${updatedBy.name}`;
    } else {
      return `Created ${new Date(createdAt).toLocaleDateString()} by ${
        owner.name
      }`;
    }
  };

  const getJdenticonSvg = (value: string, size: number = 200) => {
    const svg = jdenticon.toSvg(value, size, {
      padding: 0.08,
      saturation: {
        color: 0.5,
        grayscale: 0.0,
      },
      lightness: {
        color: [0.4, 0.8],
        grayscale: [0.3, 0.9],
      },
    });

    // Convert SVG to base64 data URL
    return `data:image/svg+xml;base64,${btoa(svg)}`;
  };

  return (
    <Card
      bg={bgColor}
      border={"1px"}
      borderColor={borderColor}
      cursor="pointer"
      onClick={handleCardClick}
      _hover={{
        bg: bgColor,
        transform: "translateY(-2px)",
        shadow: "md",
      }}
      transition="all 0.2s"
      position="relative"
      p={0}
      borderRadius={"10px"}
    >
      <CardBody p={3}>
        <VStack align="stretch" spacing={1}>
          {/* Jdenticon Canvas */}
          <Box
            w="full"
            h="200px"
            borderRadius="md"
            overflow="hidden"
            bg={cardBg}
            display="flex"
            alignItems="center"
            justifyContent="center"
          >
            <Image
              src={getJdenticonSvg(name, 200)}
              alt={name}
              w="full"
              h="200px"
              objectFit="contain"
              borderRadius="md"
            />
          </Box>

          <Box
            display={"flex"}
            flexFlow={"row"}
            alignItems="center"
            justifyContent="space-between"
            pt={2}
            pb={1}
          >
            {/* Name */}
            <Heading
              size="xs"
              color={textColor}
              fontWeight="600"
              noOfLines={1}
              textOverflow="ellipsis"
              whiteSpace="nowrap"
              overflow="hidden"
            >
              {name}
            </Heading>
            <HStack justify="space-between" zIndex={100}>
              <Menu placement="top-start">
                <Tooltip label="More actions" placement="top">
                  <MenuButton
                    as={IconButton}
                    aria-label="Options"
                    icon={<MoreVertical size={18} />}
                    variant="ghost"
                    size="xs"
                    onClick={(e) => e.stopPropagation()}
                  />
                </Tooltip>
                <Portal>
                  <MenuList bg={cardBg} borderColor={borderColor} zIndex={999}>
                    <MenuItem
                      icon={<Share2 size={15} />}
                      onClick={(e) => {
                        e.stopPropagation();
                        onShare(id);
                      }}
                      bg={cardBg}
                      _hover={{ bg: hoverBg }}
                      fontSize={"sm"}
                      py={1}
                    >
                      Share
                    </MenuItem>
                    <MenuItem
                      icon={<Edit2 size={15} />}
                      onClick={(e) => {
                        e.stopPropagation();
                        onRename(id);
                      }}
                      bg={cardBg}
                      _hover={{ bg: hoverBg }}
                      fontSize={"sm"}
                      py={1}
                    >
                      Rename
                    </MenuItem>

                    <MenuItem
                      icon={<Download size={15} />}
                      onClick={(e) => {
                        e.stopPropagation();
                        onDownload(id);
                      }}
                      bg={cardBg}
                      _hover={{ bg: hoverBg }}
                      fontSize={"sm"}
                      py={1}
                    >
                      Download
                    </MenuItem>
                    <MenuItem
                      icon={<History size={15} />}
                      onClick={(e) => {
                        e.stopPropagation();
                        onHistory(id);
                      }}
                      bg={cardBg}
                      _hover={{ bg: hoverBg }}
                      fontSize={"sm"}
                      py={1}
                    >
                      Activity history
                    </MenuItem>
                    <MenuDivider />
                    <MenuItem
                      icon={<Trash2 size={15} />}
                      onClick={(e) => {
                        e.stopPropagation();
                        onDelete(id);
                      }}
                      // color="red.500"
                      bg={cardBg}
                      _hover={{ bg: hoverBg }}
                      fontSize={"sm"}
                      py={1}
                    >
                      Move to trash
                    </MenuItem>
                  </MenuList>
                </Portal>
              </Menu>
            </HStack>
          </Box>

          {/* Display info */}
          <HStack spacing={2} fontSize="xs" color={mutedText}>
            {/* <Avatar
              size="2xs"
              name={updatedBy.name || owner.name}
              src={updatedBy.avatar || owner.avatar}
            /> */}
            <Text noOfLines={1}>{getDisplayText()}</Text>
          </HStack>
        </VStack>
      </CardBody>
    </Card>
  );
}
