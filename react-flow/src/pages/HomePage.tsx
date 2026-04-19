import { useState, useEffect, useMemo, useRef } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import {
  Box,
  Button,
  Container,
  Heading,
  Text,
  VStack,
  HStack,
  Icon,
  Flex,
  Avatar,
  useColorModeValue,
  IconButton,
  Drawer,
  DrawerContent,
  DrawerOverlay,
  useDisclosure,
  Divider,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  Spinner,
  Image,
  useToast,
} from "@chakra-ui/react";
import { ThemeToggle } from "../components/page/ThemeToggle";
import {
  Plus,
  LogOut,
  Folder,
  User,
  Share2,
  Trash2,
  Menu as MenuIcon,
  ChevronRight,
  Settings,
} from "lucide-react";
import { DiDatabase } from "react-icons/di";
import {
  FaFolder,
  FaFolderOpen,
  FaTrash,
  FaUserFriends,
  FaUsers,
} from "react-icons/fa";
import { FaUserGroup } from "react-icons/fa6";
import { IoMdTrash, IoMdFolderOpen, IoMdFolder } from "react-icons/io";
import { HiUsers, HiOutlineUsers } from "react-icons/hi2";
import { PiTrashSimpleFill, PiTrashSimple } from "react-icons/pi";
import { HeaderWithSearch } from "../components/page/HeaderWithSearch";
import FloatingChat from "../components/page/FloatingChat";
import { HiOutlineDotsHorizontal } from "react-icons/hi";
import { PiStar, PiStarFill } from "react-icons/pi";
import { BsChatLeftDots, BsChatLeftDotsFill } from "react-icons/bs";
import { IoHelp } from "react-icons/io5";
import { RiBarChart2Fill, RiBarChart2Line } from "react-icons/ri";
import { useUser } from "../contexts/UserContext";
import { NewDiagramDialog } from "../components/page/NewDiagramDialog";

interface UserData {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  picture?: string;
  role: string;
}

export function HomePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, loading, logout } = useUser();
  const [isChatOpen, setIsChatOpen] = useState(false);
  const { isOpen, onOpen, onClose } = useDisclosure();
  const {
    isOpen: isDialogOpen,
    onOpen: onDialogOpen,
    onClose: onDialogClose,
  } = useDisclosure();
  const toast = useToast();

  const chatWidth = 300;

  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const hoverBg = useColorModeValue("#f6f8fa", "#1c2128");
  const activeNavBg = useColorModeValue("#cbe0f8ff", "#353c47ff");
  const mutedText = useColorModeValue("#57606a", "#8b949e");

  const handleCreateDiagram = async (diagramName: string, jsonFile?: File) => {
    try {
      const formData = new FormData();
      formData.append("name", diagramName);

      if (jsonFile) {
        formData.append("jsonFile", jsonFile);
      }

      const response = await fetch(
        "http://localhost:8080/api/diagrams/create",
        {
          method: "POST",
          body: formData,
          credentials: "include",
        }
      );

      if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.message || "Failed to create diagram");
      }

      const data = await response.json();

      if (data.success) {
        toast({
          title: jsonFile
            ? "Diagram imported successfully"
            : "Diagram created successfully",
          status: "success",
          duration: 3000,
          isClosable: true,
        });
        navigate(`/${data.diagramId}`);
      }
    } catch (error: any) {
      console.error("Error creating diagram:", error);
      toast({
        title: "Error creating diagram",
        description: error.message,
        status: "error",
        duration: 5000,
        isClosable: true,
      });
    }
  };

  const isActive = (path: string) => location.pathname === path;

  const NavItem = ({
    icon,
    activeIcon,
    label,
    path,
    onClick,
    business = false,
  }: {
    icon: any;
    activeIcon?: any;
    label: string;
    path?: string;
    onClick?: () => void;
    business?: boolean;
  }) => {
    const active = path && isActive(path);

    return (
      <Button
        w="full"
        justifyContent="flex-start"
        variant="ghost"
        leftIcon={
          <Icon
            as={active && activeIcon ? activeIcon : icon}
            boxSize={business ? 4 : 6}
            mr={business ? "6px" : "4px"}
          />
        }
        bg={active ? activeNavBg : "transparent"}
        color={textColor}
        fontWeight="400"
        fontSize={business ? "13px" : "15px"}
        h={business ? "35px" : "45px"}
        _hover={{ bg: activeNavBg }}
        onClick={() => {
          if (onClick) onClick();
          else if (path) navigate(path);
          onClose();
        }}
      >
        {label}
      </Button>
    );
  };

  const buttonRef = useRef<HTMLButtonElement>(null);

  const SidebarContent = () => (
    <VStack h="full" spacing={0} align="stretch">
      {/* Logo & Title */}
      <Box p={2} pt={4} pb={6}>
        <HStack spacing={1}>
          <Icon as={DiDatabase} p={0} boxSize={14} color={textColor} />
          <Box>
            <Heading size="md" color={textColor} fontWeight="600">
              Database Diagram
            </Heading>
            <Text fontSize="sm" color={mutedText}>
              Design & Visualize
            </Text>
          </Box>
        </HStack>
      </Box>

      <Box p={3} pt={0}>
        <Button
          w="full"
          colorScheme="blue"
          leftIcon={<Icon as={Plus} boxSize={6} />}
          onClick={onDialogOpen}
          size="lg"
          fontSize={"md"}
          boxShadow="0 5px 10px rgba(0,0,0,0.4)"
        >
          New Diagram
        </Button>
      </Box>

      {/* Navigation */}
      <VStack spacing={1} px={3} align="stretch" overflowY="auto">
        <NavItem
          icon={IoMdFolderOpen}
          activeIcon={IoMdFolder}
          label="My Diagrams"
          path="/home"
        />
        <NavItem
          icon={HiOutlineUsers}
          activeIcon={HiUsers}
          label="Shared with Me"
          path="/home/shared"
        />
        <NavItem
          icon={PiTrashSimple}
          activeIcon={PiTrashSimpleFill}
          label="Trash"
          path="/home/trash"
        />

        {/* Statistics - Only for ADMIN */}
        {user?.role === "ADMIN" && (
          <>
            <NavItem
              icon={RiBarChart2Line}
              activeIcon={RiBarChart2Fill}
              label="Statistics"
              path="/home/statistics"
            />
          </>
        )}
      </VStack>

      <Box flex={1} />

      <VStack spacing={1} my={1} mx={3}>
        <NavItem
          icon={BsChatLeftDots}
          activeIcon={BsChatLeftDotsFill}
          label="Send Feedback"
          business={true}
        />
        <NavItem
          icon={IoHelp}
          activeIcon={IoHelp}
          label="Get Help"
          business={true}
        />
      </VStack>

      {/* User Profile */}
      {loading ? (
        <Box p={4}>
          <Spinner size="sm" />
        </Box>
      ) : user ? (
        <Box
          p={3}
          borderTop="1px"
          borderColor={borderColor}
          _hover={{ bg: hoverBg }}
        >
          <Menu placement="top-start" matchWidth>
            <MenuButton
              as={Button}
              w="full"
              h="auto"
              p={2}
              variant="ghost"
              ref={buttonRef}
            >
              <HStack spacing={3}>
                <Image
                  src={user.picture}
                  alt={user.username}
                  boxSize="30px"
                  borderRadius="full"
                  objectFit="cover"
                  borderWidth="2px"
                  borderColor={borderColor}
                  borderStyle={"solid"}
                />
                <VStack
                  align="start"
                  spacing={0}
                  flex={1}
                  minW={0}
                  overflow="hidden"
                >
                  <Text
                    fontSize="sm"
                    fontWeight="600"
                    color={textColor}
                    noOfLines={1}
                    textAlign="left"
                    width="100%"
                  >
                    {user.firstName} {user.lastName}
                  </Text>
                  <Text
                    fontSize="xs"
                    color={mutedText}
                    noOfLines={1}
                    textAlign="left"
                    width="100%"
                  >
                    @{user.username}
                  </Text>
                </VStack>
                <IconButton
                  aria-label="More options"
                  icon={<HiOutlineDotsHorizontal />}
                  size="sm"
                  variant="ghost"
                />
              </HStack>
            </MenuButton>
            <MenuList bg={cardBg} borderColor={borderColor}>
              <MenuItem
                icon={<Icon as={User} boxSize={4} />}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
                onClick={() => navigate("/home/profile")}
              >
                Profile Settings
              </MenuItem>
              <MenuItem
                icon={<Icon as={Settings} boxSize={4} />}
                bg={cardBg}
                _hover={{ bg: hoverBg }}
              >
                Preferences
              </MenuItem>
              <Divider />
              <MenuItem
                icon={<Icon as={LogOut} boxSize={4} />}
                color="red.500"
                bg={cardBg}
                _hover={{ bg: hoverBg }}
                onClick={logout}
              >
                Logout
              </MenuItem>
            </MenuList>
          </Menu>
        </Box>
      ) : null}
    </VStack>
  );

  if (loading) {
    return (
      <Flex h="100vh" align="center" justify="center" bg={bgColor}>
        <Spinner size="xl" />
      </Flex>
    );
  }

  return (
    <Box minH="100vh" bg={bgColor}>
      {/* Sidebar - Desktop */}
      <Box
        display={{ base: "none", lg: "block" }}
        position="fixed"
        left={0}
        top={0}
        bottom={0}
        w="280px"
        bg={bgColor}
      >
        <SidebarContent />
      </Box>

      {/* Sidebar - Mobile Drawer */}
      <Drawer isOpen={isOpen} placement="left" onClose={onClose}>
        <DrawerOverlay />
        <DrawerContent bg={bgColor} maxW="280px">
          <SidebarContent />
        </DrawerContent>
      </Drawer>

      {/* Main Content */}
      <Box
        ml={{ base: 0, lg: "280px" }}
        minH="100vh"
        display="flex"
        flexDirection="column"
      >
        <Box display="flex" flexDirection="row" alignItems="center">
          {/* Mobile Menu Button */}
          <Box
            display={{ base: "block", lg: "none" }}
            bg={bgColor}
            zIndex={10}
            borderRadius={"full"}
            mx={2}
          >
            <IconButton
              aria-label="Open menu"
              icon={<Icon as={MenuIcon} boxSize={5} />}
              onClick={onOpen}
              bg={bgColor}
              borderRadius={"full"}
            />
          </Box>
          <HeaderWithSearch
            onChatToggle={() => setIsChatOpen(!isChatOpen)}
            isChatOpen={isChatOpen}
          />
        </Box>

        {/* Content Area - chat will overlay on top */}
        <Box
          flex="1"
          px={{ base: 4, md: 8 }}
          py={6}
          minH="0"
          borderTopRadius="30px"
          bg={cardBg}
          overflowY="auto"
          mr={isChatOpen ? chatWidth + 10 + "px" : "0"}
          transition="margin-right 0.3s cubic-bezier(0.4, 0, 0.2, 1)"
          zIndex={20}
          border={"1px"}
          borderColor={borderColor}
        >
          <Outlet />
        </Box>
      </Box>

      {/* Floating Chat Panel */}
      <FloatingChat isOpen={isChatOpen} width={chatWidth} />

      {/* New Diagram Dialog */}
      <NewDiagramDialog
        isOpen={isDialogOpen}
        onClose={onDialogClose}
        onCreateDiagram={handleCreateDiagram}
      />
    </Box>
  );
}
