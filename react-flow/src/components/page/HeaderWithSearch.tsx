import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Flex,
  HStack,
  Input,
  InputGroup,
  InputRightElement,
  IconButton,
  Button,
  useDisclosure,
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalCloseButton,
  ModalBody,
  FormControl,
  FormLabel,
  useColorModeValue,
  InputLeftElement,
  Tooltip,
  VStack,
  Text,
  Avatar,
  Spinner,
  useToast,
  ModalFooter,
} from "@chakra-ui/react";
import { Search } from "lucide-react";
import { ThemeToggle } from "./ThemeToggle";
import { TbFilterBolt } from "react-icons/tb";
import { FloatingUnitButton } from "./FloatingUnitButton";
import { diagramApi, DiagramListItem } from "../../api/diagramApi";

interface HeaderWithSearchProps {
  onChatToggle?: () => void;
  isChatOpen?: boolean;
}

export function HeaderWithSearch({
  onChatToggle,
  isChatOpen,
}: HeaderWithSearchProps) {
  const navigate = useNavigate();
  const toast = useToast();
  const { isOpen, onOpen, onClose } = useDisclosure();
  const {
    isOpen: isResultsOpen,
    onOpen: onResultsOpen,
    onClose: onResultsClose,
  } = useDisclosure();
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<DiagramListItem[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [isSearching, setIsSearching] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  // Advanced search filters
  const [advancedName, setAdvancedName] = useState("");
  const [advancedOwner, setAdvancedOwner] = useState("");
  const [advancedDateStart, setAdvancedDateStart] = useState("");
  const [advancedDateEnd, setAdvancedDateEnd] = useState("");
  const [advancedResults, setAdvancedResults] = useState<DiagramListItem[]>([]);
  const [isAdvancedSearching, setIsAdvancedSearching] = useState(false);

  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const mutedText = useColorModeValue("#57606a", "#8b949e");
  const hoverBg = useColorModeValue("#f6f8fa", "#1c2128");

  // Debounced search
  useEffect(() => {
    if (searchQuery.trim().length < 2) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    const timeoutId = setTimeout(async () => {
      setIsSearching(true);
      try {
        const response = await diagramApi.getList({
          searchQuery: searchQuery.trim(),
          pageSize: 10,
          isDeleted: false,
        });
        setSearchResults(response.diagrams);
        setShowResults(true);
      } catch (error) {
        console.error("Search error:", error);
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  // Click outside to close
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        searchRef.current &&
        !searchRef.current.contains(event.target as Node)
      ) {
        setShowResults(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const formatDisplayDate = (dateInput: string) => {
    const date = new Date(dateInput);
    return date.toLocaleDateString();
  };

  const handleResultClick = (diagramId: number) => {
    navigate(`/${diagramId}`);
    setShowResults(false);
    setSearchQuery("");
  };

  const handleAdvancedResultClick = (diagramId: number) => {
    navigate(`/${diagramId}`);
    onResultsClose();
    onClose();
  };

  const handleAdvancedSearch = async () => {
    setIsAdvancedSearching(true);
    try {
      const response = await diagramApi.getList({
        searchQuery: advancedName.trim() || undefined,
        ownerFilter: advancedOwner.trim()
          ? (advancedOwner.trim() as "me" | "team")
          : undefined,
        pageSize: 50,
        isDeleted: false,
      });

      setAdvancedResults(response.diagrams);
      onClose(); // Close advanced search modal
      onResultsOpen(); // Open results modal
    } catch (error) {
      console.error("Advanced search error:", error);
      toast({
        title: "Search Error",
        description: "Failed to search diagrams",
        status: "error",
        duration: 3000,
        isClosable: true,
      });
    } finally {
      setIsAdvancedSearching(false);
    }
  };

  const resetAdvancedSearch = () => {
    setAdvancedName("");
    setAdvancedOwner("");
    setAdvancedDateStart("");
    setAdvancedDateEnd("");
  };

  return (
    <>
      <Box
        w="full"
        bg={bgColor}
        position="sticky"
        top={0}
        zIndex={100}
        h="60px"
        my={2}
        display="grid"
        alignItems="center"
      >
        <Flex justify="space-between" align="center" h="full">
          {/* Left: Search */}
          <HStack
            spacing={2}
            py={2}
            flex={1}
            h="full"
            mr={2}
            ref={searchRef}
            position="relative"
          >
            <InputGroup h="full" maxW="800px">
              <InputLeftElement h="full" pr="2">
                <Tooltip label="Search" hasArrow placement="bottom">
                  <IconButton
                    aria-label="Search"
                    icon={
                      isSearching ? (
                        <Spinner size="sm" />
                      ) : (
                        <Search size={"20"} />
                      )
                    }
                    variant="ghost"
                    size="md"
                    ml={"12px"}
                    borderRadius="30px"
                    isDisabled
                  />
                </Tooltip>
              </InputLeftElement>
              <InputRightElement h="full" pl="2">
                <Tooltip label="Advanced filter" hasArrow placement="bottom">
                  <IconButton
                    aria-label="Filter"
                    icon={<TbFilterBolt size="22" />}
                    variant="ghost"
                    size="md"
                    mr={"12px"}
                    borderRadius="full"
                    onClick={onOpen}
                  />
                </Tooltip>
              </InputRightElement>
              <Input
                placeholder="Search diagrams by name or owner..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => searchResults.length > 0 && setShowResults(true)}
                bg={cardBg}
                borderRadius="30px"
                h="full"
                fontSize="md"
                pl={"50px"}
              />
            </InputGroup>

            {/* Search Results Dropdown */}
            {showResults && searchResults.length > 0 && (
              <Box
                position="absolute"
                top="100%"
                left={0}
                right={0}
                mt={0}
                p={1}
                maxW="800px"
                bg={cardBg}
                borderRadius="20px"
                border="1px"
                borderColor={borderColor}
                boxShadow="lg"
                maxH="400px"
                overflowY="auto"
                zIndex={9999}
              >
                <VStack align="stretch" spacing={0}>
                  {searchResults.map((result) => (
                    <Box
                      key={result.id}
                      p={3}
                      cursor="pointer"
                      _hover={{ bg: hoverBg }}
                      onClick={() => handleResultClick(result.id)}
                      borderBottom="1px"
                      borderColor={borderColor}
                      _last={{ borderBottom: "none" }}
                    >
                      <HStack spacing={3} align="start">
                        <Flex flex={1} minW={0} align="center" gap={3}>
                          {/* Name */}
                          <Text
                            fontSize="sm"
                            fontWeight="600"
                            color={textColor}
                            noOfLines={1}
                            overflow="hidden"
                            textOverflow="ellipsis"
                            whiteSpace="nowrap"
                            flex="1" // cho name co giãn khi dài
                            minW={0}
                          >
                            {result.name}
                          </Text>

                          {/* Owner */}
                          <Text
                            fontSize="sm"
                            color={mutedText}
                            whiteSpace="nowrap"
                          >
                            Owned by {result.ownerFullName}@gmail.com, created
                            at {formatDisplayDate(result.createdAt)}
                          </Text>
                        </Flex>
                      </HStack>
                    </Box>
                  ))}
                </VStack>
              </Box>
            )}

            {/* No results message */}
            {showResults &&
              searchResults.length === 0 &&
              !isSearching &&
              searchQuery.trim().length >= 2 && (
                <Box
                  position="absolute"
                  top="100%"
                  left={0}
                  right={0}
                  mt={0}
                  maxW="800px"
                  bg={cardBg}
                  borderRadius="20px"
                  border="1px"
                  borderColor={borderColor}
                  boxShadow="lg"
                  p={4}
                  zIndex={1000}
                >
                  <Text color={mutedText} fontSize="sm" textAlign="center">
                    No diagrams found
                  </Text>
                </Box>
              )}
          </HStack>

          {/* Right: Header Unit Button + Theme Toggle */}
          <HStack spacing={2} mr={2} h="full" align="center">
            <FloatingUnitButton onClick={onChatToggle} />
            <ThemeToggle />
          </HStack>
        </Flex>
      </Box>

      {/* Advanced Search Modal */}
      <Modal isOpen={isOpen} onClose={onClose} isCentered>
        <ModalOverlay />
        <ModalContent
          maxW={{ base: "90%", sm: "400px", md: "500px", lg: "600px" }}
          borderRadius="lg"
          bg={cardBg}
        >
          <ModalHeader color={textColor}>Advanced Search</ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            {/* Name */}
            <FormControl mb={3}>
              <FormLabel color={textColor}>Name</FormLabel>
              <Input
                placeholder="Diagram name"
                borderRadius="md"
                value={advancedName}
                onChange={(e) => setAdvancedName(e.target.value)}
              />
            </FormControl>

            {/* Owner */}
            <FormControl mb={3}>
              <FormLabel color={textColor}>Owner</FormLabel>
              <Input
                placeholder="Enter owner username or 'me' or 'team'"
                borderRadius="md"
                value={advancedOwner}
                onChange={(e) => setAdvancedOwner(e.target.value)}
              />
            </FormControl>

            {/* Date Range */}
            <FormControl mb={3}>
              <FormLabel color={textColor}>Date Range</FormLabel>
              <HStack spacing={3}>
                <Input
                  type="date"
                  placeholder="Start date"
                  borderRadius="md"
                  value={advancedDateStart}
                  onChange={(e) => setAdvancedDateStart(e.target.value)}
                />
                <Input
                  type="date"
                  placeholder="End date"
                  borderRadius="md"
                  value={advancedDateEnd}
                  onChange={(e) => setAdvancedDateEnd(e.target.value)}
                />
              </HStack>
            </FormControl>

            {/* Buttons */}
            <Flex justify="flex-end" pb={2} mt={6} gap={3}>
              <Button variant="outline" onClick={resetAdvancedSearch}>
                Reset
              </Button>
              <Button
                colorScheme="blue"
                onClick={handleAdvancedSearch}
                isLoading={isAdvancedSearching}
                loadingText="Searching..."
              >
                Search
              </Button>
            </Flex>
          </ModalBody>
        </ModalContent>
      </Modal>

      {/* Advanced Search Results Modal */}
      <Modal
        isOpen={isResultsOpen}
        onClose={onResultsClose}
        size="xl"
        scrollBehavior="inside"
        isCentered
      >
        <ModalOverlay />
        <ModalContent bg={cardBg} maxH="80vh" maxW={"700px"}>
          <ModalHeader color={textColor}>
            Search Results ({advancedResults.length})
          </ModalHeader>
          <ModalCloseButton />
          <ModalBody>
            {advancedResults.length === 0 ? (
              <VStack py={8} spacing={4}>
                <Text color={mutedText} fontSize="lg">
                  No diagrams found
                </Text>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => {
                    onResultsClose();
                    onOpen(); // Reopen search modal
                  }}
                >
                  Try Again
                </Button>
              </VStack>
            ) : (
              <VStack align="stretch" spacing={2}>
                {advancedResults.map((result) => (
                  <Box
                    key={result.id}
                    p={4}
                    cursor="pointer"
                    _hover={{ bg: hoverBg }}
                    onClick={() => handleAdvancedResultClick(result.id)}
                    borderRadius="md"
                    border="1px"
                    borderColor={borderColor}
                  >
                    <Flex
                      align="center"
                      justify="space-between"
                      minW={0}
                      gap={4} // giúp responsive
                    >
                      {/* Name */}
                      <Text
                        fontSize="sm"
                        fontWeight="600"
                        color={textColor}
                        noOfLines={1}
                        flex="1"
                        minW={0}
                        overflow="hidden"
                        textOverflow="ellipsis"
                        whiteSpace="nowrap"
                      >
                        {result.name}
                      </Text>

                      {/* Owner */}
                      <Text fontSize="sm" color={mutedText} whiteSpace="nowrap">
                        Owned by {result.ownerFullName}@gmail.com, created at{" "}
                        {formatDisplayDate(result.createdAt)}
                      </Text>
                    </Flex>
                  </Box>
                ))}
              </VStack>
            )}
          </ModalBody>
          <ModalFooter>
            <Button variant="ghost" onClick={onResultsClose}>
              Close
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </>
  );
}
