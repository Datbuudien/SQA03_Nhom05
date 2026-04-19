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
} from "@chakra-ui/react";
import { CiGrid41 } from "react-icons/ci";
import { IoIosList } from "react-icons/io";
import { FaCaretDown } from "react-icons/fa";
import { HiOutlineUsers } from "react-icons/hi2";
import { DiagramList, Diagram } from "../components/page/DiagramList";
import { diagramApi, DiagramListFilters } from "../api/diagramApi";

export function SharedDiagramsPage() {
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
          isDeleted: false,
          sharedWithMe: true, // Only get shared diagrams
          sortBy: "updatedAt",
          sortDirection: "DESC",
        };

        // Apply filters
        if (filterName !== "All") {
          filters.nameStartsWith = filterName;
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
    [loading, hasMore, lastDiagramId, filterName, filterLatest, toast]
  );

  // Load initial data
  useEffect(() => {
    loadDiagrams(true);
  }, [filterName, filterLatest]);

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
    navigate(`/${diagramId}`);
  };

  const handleShare = (diagramId: string) => {
    toast({
      title: "Share dialog",
      description: `Sharing diagram ${diagramId}`,
      status: "info",
      duration: 2000,
      isClosable: true,
    });
  };

  const handleRename = (diagramId: string) => {
    toast({
      title: "Cannot rename",
      description: "You don't have permission to rename this diagram",
      status: "warning",
      duration: 2000,
      isClosable: true,
    });
  };

  const handleToggleStar = (diagramId: string) => {
    setDiagrams((prev) =>
      prev.map((d) =>
        d.id === diagramId ? { ...d, isStarred: !d.isStarred } : d
      )
    );
    const diagram = diagrams.find((d) => d.id === diagramId);
    toast({
      title: diagram?.isStarred ? "Removed from starred" : "Added to starred",
      status: "success",
      duration: 2000,
      isClosable: true,
    });
  };

  const handleDuplicate = (diagramId: string) => {
    toast({
      title: "Duplicating diagram",
      description: `Creating a copy of diagram ${diagramId}`,
      status: "info",
      duration: 2000,
      isClosable: true,
    });
  };

  const handleDownload = (diagramId: string) => {
    toast({
      title: "Downloading diagram",
      description: `Downloading diagram ${diagramId}`,
      status: "info",
      duration: 2000,
      isClosable: true,
    });
  };

  const handleHistory = (diagramId: string) => {
    toast({
      title: "Activity history",
      description: `Viewing history for diagram ${diagramId}`,
      status: "info",
      duration: 2000,
      isClosable: true,
    });
  };

  const handleDelete = (diagramId: string) => {
    toast({
      title: "Cannot delete",
      description: "Only the owner can delete this diagram",
      status: "warning",
      duration: 2000,
      isClosable: true,
    });
  };

  return (
    <Box w="full" maxW="100%" overflowX="hidden">
      <Flex justify="space-between" align="center" mb={4}>
        <Box>
          <Heading size="lg" color={textColor} fontWeight="400">
            Shared with Me
          </Heading>
          {/* <Text color={mutedText} fontSize="14px">
            Diagrams that others have shared with you
          </Text> */}
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
                <Icon as={HiOutlineUsers} boxSize={12} color={mutedText} />
              </Box>
              <Heading size="md" color={textColor} fontWeight="600">
                No shared diagrams
              </Heading>
              <Text color={mutedText} fontSize="14px">
                Diagrams shared with you will appear here
              </Text>
            </VStack>
          </CardBody>
        </Card>
      ) : (
        <>
          <DiagramList
            diagrams={diagrams}
            view={view}
            onOpen={handleOpenDiagram}
            onShare={handleShare}
            onRename={handleRename}
            onToggleStar={handleToggleStar}
            onDuplicate={handleDuplicate}
            onDownload={handleDownload}
            onHistory={handleHistory}
            onDelete={handleDelete}
          />

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
    </Box>
  );
}
