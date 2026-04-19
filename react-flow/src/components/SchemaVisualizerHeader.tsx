import React, { useState } from "react";
import {
  Box,
  Stack,
  Avatar,
  IconButton,
  Tooltip,
  useColorModeValue,
  useBreakpointValue,
  useDisclosure,
  Image,
} from "@chakra-ui/react";
import { MdHistory } from "react-icons/md";
import { AiOutlineUsergroupAdd } from "react-icons/ai";
import { HiChevronDown, HiOutlineDotsVertical } from "react-icons/hi";
import { AccountGroup } from "./AccountGroup";
import { FloatingUnitButton } from "./page/FloatingUnitButton";
import { ThemeToggle } from "./page/ThemeToggle";
import { ManageMembersDialog } from "./ManageMembersDialog";
import { HistoryListDialog } from "./HistoryListDialog";
import { useUser } from "../contexts/UserContext";
import { useParams } from "react-router-dom";

interface SchemaVisualizerHeaderProps {
  onChatToggle?: () => void;
  isChatOpen?: boolean;
  onlineUsernames?: string[];
  isHistoryView?: boolean; // ⭐ NEW: Flag để biết có phải history view không
}

export const SchemaVisualizerHeader: React.FC<SchemaVisualizerHeaderProps> = ({
  onChatToggle,
  isChatOpen = false,
  onlineUsernames = [],
  isHistoryView = false, // ⭐ NEW
}) => {
  const { user } = useUser();
  const { diagramId } = useParams<{ diagramId: string }>();
  const { isOpen, onOpen, onClose } = useDisclosure();
  const {
    isOpen: isHistoryOpen,
    onOpen: onHistoryOpen,
    onClose: onHistoryClose,
  } = useDisclosure();

  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("white", "#333");
  const iconColor = useColorModeValue("gray.700", "white");

  const tooltipPlacement = useBreakpointValue<
    "top" | "bottom" | "left" | "right"
  >({
    base: "left",
    md: "bottom",
  });

  const [menuOpen, setMenuOpen] = useState(false);

  const buttonStyle = {
    bg: bgColor,
    color: iconColor,
    border: "1px solid",
    borderColor: borderColor,
    size: "md" as const,
    borderRadius: "50%",
    transition: "all 0.2s ease",
  };

  return (
    <Box position="absolute" top={4} right={4} zIndex={1000}>
      <Stack
        direction={{ base: "column-reverse", sm: "row" }}
        align="center"
        spacing={2}
      >
        {/* Trên mobile: nút 3 chấm */}
        <Box display={{ base: "block", sm: "none" }}>
          <IconButton
            aria-label="Toggle menu"
            icon={<HiChevronDown />}
            {...buttonStyle}
            onClick={() => setMenuOpen(!menuOpen)}
            sx={{
              transition: "transform 0.2s ease",
              transform: menuOpen ? "rotate(180deg)" : "rotate(0deg)",
            }}
          />
        </Box>

        {/* Các nút menu khác */}
        <Stack
          direction={{ base: "column", sm: "row" }}
          spacing={{ base: 2, sm: 1.5 }}
          display={{
            base: menuOpen ? "flex" : "none",
            sm: "flex",
          }}
          align="center"
        >
          {/* ⭐ Chỉ hiển thị các button này nếu KHÔNG phải history view */}
          {!isHistoryView && (
            <>
              <AccountGroup onlineUsernames={onlineUsernames} />
              <Tooltip label="Add member" placement={tooltipPlacement} hasArrow>
                <IconButton
                  borderRadius="50%"
                  aria-label="Add member"
                  icon={<AiOutlineUsergroupAdd size={20} />}
                  {...buttonStyle}
                  onClick={onOpen}
                />
              </Tooltip>
              <Tooltip
                label="Activity history"
                placement={tooltipPlacement}
                hasArrow
              >
                <IconButton
                  aria-label="Activity history"
                  icon={<MdHistory size={20} />}
                  {...buttonStyle}
                  onClick={onHistoryOpen}
                />
              </Tooltip>
              <FloatingUnitButton onClick={onChatToggle} />
            </>
          )}

          {/* ⭐ Theme toggle luôn hiển thị */}
          <ThemeToggle />
        </Stack>

        {/* Avatar luôn trên cùng */}
        <Tooltip
          label={user.username + "@gmail.com"}
          placement={tooltipPlacement}
          hasArrow
        >
          <Image
            src={user.picture}
            alt={user.username}
            boxSize="40px"
            borderRadius="full"
            objectFit="cover"
            borderWidth="2px"
            borderColor={borderColor}
            borderStyle={"solid"}
          />
        </Tooltip>
      </Stack>

      {/* ⭐ Dialogs - chỉ hiện khi không phải history view */}
      {!isHistoryView && (
        <>
          <ManageMembersDialog isOpen={isOpen} onClose={onClose} />
          <HistoryListDialog
            isOpen={isHistoryOpen}
            onClose={onHistoryClose}
            diagramId={diagramId || ""}
          />
        </>
      )}
    </Box>
  );
};
