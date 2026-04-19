import { useState } from "react";
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
  CardHeader,
  CardBody,
  Flex,
  useColorModeValue,
  SimpleGrid,
  IconButton,
  Menu,
  MenuButton,
  MenuList,
  MenuItem,
} from "@chakra-ui/react";
import {
  Plus,
  FileText,
  Folder,
  Clock,
  Grid,
  List,
  ChevronDown,
} from "lucide-react";
import { CiGrid41 } from "react-icons/ci";
import { IoIosList } from "react-icons/io";
import { FaCaretDown } from "react-icons/fa";
import { LuShare2 } from "react-icons/lu";
import { PiStar, PiStarFill } from "react-icons/pi";

interface Diagram {
  id: string;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export function StarPage() {
  const navigate = useNavigate();
  const [diagrams, setDiagrams] = useState<Diagram[]>([]);
  const [view, setView] = useState<"grid" | "list">("grid");
  const [filterName, setFilterName] = useState<string>("All Names");
  const [filterOwner, setFilterOwner] = useState<string>("All Owners");
  const [filterLatest, setFilterLatest] = useState<string>("All Time");

  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const hoverBg = useColorModeValue("#f6f8fa", "#1c2128");
  const mutedText = useColorModeValue("#57606a", "#8b949e");

  const handleCreateDiagram = () => {
    const newId = Date.now().toString();
    navigate(`/${newId}`);
  };

  const handleOpenDiagram = (diagramId: string) => {
    navigate(`/${diagramId}`);
  };

  return (
    <Box w="full" maxW="100%" overflowX="hidden">
      <Flex justify="space-between" align="center" mb={4}>
        <Box>
          <Heading size="lg" color={textColor} fontWeight="400">
            Mark Star
          </Heading>
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

      <Flex>
        {/* Filters */}
        <HStack spacing={2}>
          {/* Name Filter */}
          <Menu>
            <MenuButton as={Button} rightIcon={<FaCaretDown />} size="sm">
              {filterName}
            </MenuButton>
            <MenuList>
              {["All Names", "Diagram A", "Diagram B"].map((name) => (
                <MenuItem key={name} onClick={() => setFilterName(name)}>
                  {name}
                </MenuItem>
              ))}
            </MenuList>
          </Menu>

          {/* Owner Filter */}
          <Menu>
            <MenuButton as={Button} rightIcon={<FaCaretDown />} size="sm">
              {filterOwner}
            </MenuButton>
            <MenuList>
              {["All Owners", "Me", "Team"].map((owner) => (
                <MenuItem key={owner} onClick={() => setFilterOwner(owner)}>
                  {owner}
                </MenuItem>
              ))}
            </MenuList>
          </Menu>

          {/* Latest Filter */}
          <Menu>
            <MenuButton as={Button} rightIcon={<FaCaretDown />} size="sm">
              {filterLatest}
            </MenuButton>
            <MenuList>
              {["All Time", "Today", "Last 7 Days", "Last 30 Days"].map(
                (time) => (
                  <MenuItem key={time} onClick={() => setFilterLatest(time)}>
                    {time}
                  </MenuItem>
                )
              )}
            </MenuList>
          </Menu>
        </HStack>
      </Flex>

      {/* Diagrams Grid */}
      {diagrams.length === 0 ? (
        <Card bg={"transparent"} textAlign="center" py={12} shadow={"none"}>
          <CardBody>
            <VStack spacing={4}>
              <Box bg={hoverBg} p={6} borderRadius="full">
                <Icon as={PiStar} boxSize={12} color={mutedText} />
              </Box>
              <Heading size="md" color={textColor} fontWeight="600">
                No diagrams yet
              </Heading>
            </VStack>
          </CardBody>
        </Card>
      ) : (
        <SimpleGrid columns={{ base: 1, md: 2, lg: 3 }} spacing={4}>
          {diagrams.map((diagram) => (
            <Card
              key={diagram.id}
              bg={cardBg}
              borderColor={borderColor}
              border="1px"
              cursor="pointer"
              onClick={() => handleOpenDiagram(diagram.id)}
              _hover={{
                bg: hoverBg,
                transform: "translateY(-2px)",
                shadow: "md",
              }}
              transition="all 0.2s"
            >
              <CardHeader pb={2}>
                <VStack align="start" spacing={3}>
                  <Box
                    bg="blue.100"
                    _dark={{ bg: "blue.900" }}
                    p={2}
                    borderRadius="md"
                  >
                    <Icon as={Folder} boxSize={5} color="blue.500" />
                  </Box>
                  <Heading size="sm" color={textColor} fontWeight="600">
                    {diagram.name}
                  </Heading>
                  <HStack spacing={1} fontSize="xs" color={mutedText}>
                    <Icon as={Clock} boxSize={3} />
                    <Text>
                      Updated:{" "}
                      {new Date(diagram.updatedAt).toLocaleDateString()}
                    </Text>
                  </HStack>
                </VStack>
              </CardHeader>
              <CardBody pt={0}>
                <Text fontSize="xs" color={mutedText}>
                  Created: {new Date(diagram.createdAt).toLocaleDateString()}
                </Text>
              </CardBody>
            </Card>
          ))}
        </SimpleGrid>
      )}
    </Box>
  );
}
