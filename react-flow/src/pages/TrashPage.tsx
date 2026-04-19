import { useState, useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Heading,
  Text,
  VStack,
  HStack,
  Icon,
  Card,
  CardBody,
  Flex,
  useColorModeValue,
  IconButton,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
  useToast,
  Spinner,
  AlertDialog,
  AlertDialogOverlay,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogBody,
  AlertDialogFooter,
  useDisclosure,
} from "@chakra-ui/react";
import { CiGrid41 } from "react-icons/ci";
import { IoIosList } from "react-icons/io";
import { FaCaretDown } from "react-icons/fa";
import { PiTrashSimple } from "react-icons/pi";
import { DiagramList, Diagram } from "../components/page/DiagramList";
import { diagramApi, DiagramListFilters } from "../api/diagramApi";

export function TrashPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [view, setView] = useState<"grid" | "list">("grid");

  // Filters
  const [filterName, setFilterName] = useState<string>("All");
  const [filterOwner, setFilterOwner] = useState<string>("All");
  const [filterLatest, setFilterLatest] = useState<string>("All Time");

  // Data & Pagination
  const [diagrams, setDiagrams] = useState<Diagram[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [lastDiagramId, setLastDiagramId] = useState<number | null>(null);

  // Delete confirmation
  const { isOpen, onOpen, onClose } = useDisclosure();
  const [diagramToDelete, setDiagramToDelete] = useState<string | null>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  // Infinite scroll
  const observerRef = useRef<IntersectionObserver>();
  const lastDiagramRef = useRef<HTMLDivElement>(null);

  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const hoverBg = useColorModeValue("#f6f8fa", "#323b47ff");
  const mutedText = useColorModeValue("#57606a", "#8b949e");

  // Alphabet for name filter
  const alphabet = [
    "All",
    ...Array.from({ length: 26 }, (_, i) => String.fromCharCode(65 + i)),
  ];

  const loadDiagrams = useCallback(
    async (reset = false) => {
      if (loading) return;
      if (!reset && !hasMore) return;

      setLoading(true);
      try {
        const filters: DiagramListFilters = {
          lastDiagramId: reset ? undefined : lastDiagramId || undefined,
          pageSize: 20,
          isDeleted: true, // Only get deleted diagrams
          sharedWithMe: false,
          sortBy: "updatedAt",
          sortDirection: "DESC",
        };

        // Apply filters
        if (filterName !== "All") {
          filters.nameStartsWith = filterName;
        }

        if (filterOwner === "Me") {
          filters.ownerFilter = "me";
        } else if (filterOwner === "Team") {
          filters.ownerFilter = "team";
        }

        if (filterLatest === "Today") {
          filters.dateRange = "today";
        } else if (filterLatest === "Last 7 Days") {
          filters.dateRange = "last7days";
        } else if (filterLatest === "Last 30 Days") {
          filters.dateRange = "last30days";
        }

        const response = await diagramApi.getList(filters);

        const newDiagrams = response.diagrams.map((d) => ({
          id: d.id.toString(),
          name: d.name,
          owner: {
            name: d.ownerFullName,
            avatar: d.ownerAvatar,
          },
          updatedAt: d.lastMigrationDate || d.updatedAt,
          updatedBy: {
            name: d.lastMigrationUsername || d.updatedByFullName,
            avatar: d.updatedByAvatar,
          },
          createdAt: d.createdAt,
          isStarred: d.isStarred,
          image: "",
        }));

        if (reset) {
          setDiagrams(newDiagrams);
        } else {
          setDiagrams((prev) => [...prev, ...newDiagrams]);
        }

        setHasMore(response.hasMore);
        setLastDiagramId(response.lastDiagramId);
      } catch (error) {
        console.error("Error loading diagrams:", error);
        toast({
          title: "Error loading diagrams",
          status: "error",
          duration: 3000,
          isClosable: true,
        });
      } finally {
        setLoading(false);
      }
    },
    [
      loading,
      hasMore,
      lastDiagramId,
      filterName,
      filterOwner,
      filterLatest,
      toast,
    ]
  );

  // Load initial data
  useEffect(() => {
    loadDiagrams(true);
  }, [filterName, filterOwner, filterLatest]);

  // Infinite scroll observer
  useEffect(() => {
    if (loading) return;
    if (!hasMore) return;

    if (observerRef.current) observerRef.current.disconnect();

    observerRef.current = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        loadDiagrams();
      }
    });

    if (lastDiagramRef.current) {
      observerRef.current.observe(lastDiagramRef.current);
    }

    return () => {
      if (observerRef.current) {
        observerRef.current.disconnect();
      }
    };
  }, [loading, hasMore, loadDiagrams]);

  const handleOpenDiagram = (diagramId: string) => {
    toast({
      title: "Cannot open deleted diagram",
      description: "Please restore it first",
      status: "warning",
      duration: 3000,
      isClosable: true,
    });
  };

  const handleRestore = async (diagramId: string) => {
    try {
      await diagramApi.restoreDiagram(Number(diagramId));
      setDiagrams((prev) => prev.filter((d) => d.id !== diagramId));
      toast({
        title: "Diagram restored",
        status: "success",
        duration: 2000,
        isClosable: true,
      });
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to restore diagram",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    }
  };

  const handlePermanentDelete = (diagramId: string) => {
    setDiagramToDelete(diagramId);
    onOpen();
  };

  const confirmPermanentDelete = async () => {
    if (!diagramToDelete) return;

    try {
      await diagramApi.permanentlyDelete(Number(diagramToDelete));
      setDiagrams((prev) => prev.filter((d) => d.id !== diagramToDelete));
      toast({
        title: "Diagram permanently deleted",
        status: "success",
        duration: 2000,
        isClosable: true,
      });
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to delete diagram",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    } finally {
      setDiagramToDelete(null);
      onClose();
    }
  };

  // Override handlers for trash functionality
  const modifiedHandlers = {
    onOpen: handleOpenDiagram,
    onShare: () => {},
    onRename: () => {},
    onToggleStar: () => {},
    onDuplicate: handleRestore, // Use duplicate button for restore
    onDownload: () => {},
    onHistory: () => {},
    onDelete: handlePermanentDelete, // Delete = permanent delete
  };

  return (
    <Box w="full" maxW="100%" overflowX="hidden">
      <Flex justify="space-between" align="center" mb={4}>
        <Box>
          <Heading size="lg" color={textColor} fontWeight="400">
            Trash
          </Heading>
          <Text color={mutedText} fontSize="14px" mt={1}>
            Deleted diagrams are stored here for 30 days
          </Text>
        </Box>
        <HStack spacing={0} borderRadius="md" overflow="hidden" boxShadow="md">
          <IconButton
            aria-label="Grid view"
            icon={<CiGrid41 size={"22px"} />}
            colorScheme={view === "grid" ? "blue" : "gray"}
            onClick={() => setView("grid")}
            roundedRight={0}
            size="sm"
          />
          <IconButton
            aria-label="List view"
            icon={<IoIosList size={"25px"} />}
            colorScheme={view === "list" ? "blue" : "gray"}
            onClick={() => setView("list")}
            roundedLeft={0}
            size="sm"
          />
        </HStack>
      </Flex>

      <Flex mb={4}>
        <HStack spacing={2}>
          {/* Name Filter */}
          <Menu>
            <MenuButton as={Button} rightIcon={<FaCaretDown />} size="sm">
              {filterName === "All" ? "All Name" : filterName}
            </MenuButton>

            <MenuList bg={cardBg} maxH="300px" overflowY="auto">
              {alphabet.map((letter) => (
                <MenuItem
                  key={letter}
                  bg={cardBg}
                  _hover={{ bg: hoverBg }}
                  onClick={() => setFilterName(letter)}
                >
                  {letter}
                </MenuItem>
              ))}

              {/* Option để reset filter */}
              <MenuItem
                bg={cardBg}
                _hover={{ bg: hoverBg }}
                onClick={() => setFilterName("All")} // giá trị vẫn là "All"
              >
                All Name
              </MenuItem>
            </MenuList>
          </Menu>

          {/* Latest Filter */}
          <Menu>
            <MenuButton as={Button} rightIcon={<FaCaretDown />} size="sm">
              {filterLatest}
            </MenuButton>
            <MenuList bg={cardBg}>
              {["All Time", "Today", "Last 7 Days", "Last 30 Days"].map(
                (time) => (
                  <MenuItem
                    bg={cardBg}
                    _hover={{ bg: hoverBg }}
                    key={time}
                    onClick={() => setFilterLatest(time)}
                  >
                    {time}
                  </MenuItem>
                )
              )}
            </MenuList>
          </Menu>
        </HStack>
      </Flex>

      {/* Diagrams List */}
      {diagrams.length === 0 && !loading ? (
        <Card bg={"transparent"} textAlign="center" py={12} shadow={"none"}>
          <CardBody>
            <VStack spacing={4}>
              <Box bg={hoverBg} p={6} borderRadius="full">
                <Icon as={PiTrashSimple} boxSize={12} color={mutedText} />
              </Box>
              <Heading size="md" color={textColor} fontWeight="600">
                Trash is empty
              </Heading>
              <Text color={mutedText} fontSize="14px">
                Deleted diagrams will appear here
              </Text>
            </VStack>
          </CardBody>
        </Card>
      ) : (
        <>
          <DiagramList diagrams={diagrams} view={view} {...modifiedHandlers} />

          {loading && (
            <Flex justify="center" py={4}>
              <Spinner size="md" />
            </Flex>
          )}

          {hasMore && !loading && (
            <Box ref={lastDiagramRef} h="20px" w="full" />
          )}

          {!hasMore && diagrams.length > 0 && (
            <Text
              textAlign="center"
              my={4}
              py={4}
              color={mutedText}
              fontSize="xs"
            >
              No more diagrams to load
            </Text>
          )}
        </>
      )}

      {/* Permanent Delete Confirmation Dialog */}
      <AlertDialog
        isOpen={isOpen}
        leastDestructiveRef={cancelRef}
        onClose={onClose}
      >
        <AlertDialogOverlay>
          <AlertDialogContent bg={cardBg}>
            <AlertDialogHeader
              fontSize="lg"
              fontWeight="bold"
              color={textColor}
            >
              Permanently Delete Diagram
            </AlertDialogHeader>

            <AlertDialogBody color={textColor}>
              Are you sure? This action cannot be undone. The diagram and all
              its data will be permanently deleted.
            </AlertDialogBody>

            <AlertDialogFooter>
              <Button ref={cancelRef} onClick={onClose}>
                Cancel
              </Button>
              <Button colorScheme="red" onClick={confirmPermanentDelete} ml={3}>
                Delete Permanently
              </Button>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialogOverlay>
      </AlertDialog>
    </Box>
  );
}
