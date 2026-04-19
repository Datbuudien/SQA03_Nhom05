import { useState, useRef } from "react";
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Input,
  VStack,
  HStack,
  Text,
  useColorModeValue,
  Icon,
  Box,
  Radio,
  RadioGroup,
  Stack,
  useToast,
} from "@chakra-ui/react";
import { Upload, FileJson } from "lucide-react";

interface NewDiagramDialogProps {
  isOpen: boolean;
  onClose: () => void;
  onCreateDiagram: (name: string, jsonFile?: File) => void;
}

export function NewDiagramDialog({
  isOpen,
  onClose,
  onCreateDiagram,
}: NewDiagramDialogProps) {
  const [diagramName, setDiagramName] = useState("Untitled Diagram");
  const [creationType, setCreationType] = useState<"blank" | "import">("blank");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const toast = useToast();

  const bgColor = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const mutedText = useColorModeValue("#57606a", "#8b949e");
  const hoverBg = useColorModeValue("#f6f8fa", "#1c2128");

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      if (file.type === "application/json" || file.name.endsWith(".json")) {
        setSelectedFile(file);
        // Extract diagram name from JSON if possible
        const reader = new FileReader();
        reader.onload = (e) => {
          try {
            const json = JSON.parse(e.target?.result as string);
            if (json.schema?.name) {
              setDiagramName(json.schema.name);
            }
          } catch (err) {
            console.error("Error parsing JSON:", err);
          }
        };
        reader.readAsText(file);
      } else {
        toast({
          title: "Invalid file type",
          description: "Please select a JSON file",
          status: "error",
          duration: 3000,
          isClosable: true,
        });
      }
    }
  };

  const handleCreate = () => {
    if (creationType === "import" && !selectedFile) {
      toast({
        title: "No file selected",
        description: "Please select a JSON file to import",
        status: "warning",
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    onCreateDiagram(
      diagramName,
      creationType === "import" ? selectedFile : undefined
    );
    handleClose();
  };

  const handleClose = () => {
    setDiagramName("Untitled Diagram");
    setCreationType("blank");
    setSelectedFile(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} size="lg" isCentered>
      <ModalOverlay backdropFilter="blur(4px)" />
      <ModalContent bg={bgColor} borderColor={borderColor} borderWidth="1px">
        <ModalHeader color={textColor} fontSize="xl" fontWeight="600">
          Create New Diagram
        </ModalHeader>

        <ModalBody>
          <VStack spacing={5} align="stretch">
            {/* Diagram Name Input */}
            <Box>
              <Text fontSize="sm" fontWeight="500" color={textColor} mb={2}>
                Diagram Name
              </Text>
              <Input
                value={diagramName}
                onChange={(e) => setDiagramName(e.target.value)}
                placeholder="Enter diagram name"
                size="lg"
                bg={bgColor}
                borderColor={borderColor}
                color={textColor}
                _hover={{ borderColor: mutedText }}
                _focus={{
                  borderColor: "blue.500",
                  boxShadow: "0 0 0 1px blue.500",
                }}
              />
            </Box>

            {/* Creation Type Selection */}
            <Box>
              {/* <Text fontSize="sm" fontWeight="500" color={textColor} mb={3}>
                Creation Type
              </Text> */}
              <RadioGroup
                value={creationType}
                onChange={(value) =>
                  setCreationType(value as "blank" | "import")
                }
              >
                <Stack spacing={3}>
                  <Radio
                    value="blank"
                    colorScheme="blue"
                    size="lg"
                    borderColor={borderColor}
                  >
                    <Text color={textColor} fontSize="md">
                      Start with blank diagram
                    </Text>
                  </Radio>
                  <Radio
                    value="import"
                    colorScheme="blue"
                    size="lg"
                    borderColor={borderColor}
                  >
                    <Text color={textColor} fontSize="md">
                      Import from JSON file
                    </Text>
                  </Radio>
                </Stack>
              </RadioGroup>
            </Box>

            {/* File Upload Section (shown when import is selected) */}
            {creationType === "import" && (
              <Box
                p={4}
                borderWidth="2px"
                borderStyle="dashed"
                borderColor={selectedFile ? "blue.500" : borderColor}
                borderRadius="md"
                bg={hoverBg}
                cursor="pointer"
                transition="all 0.2s"
                _hover={{ borderColor: "blue.400" }}
                onClick={() => fileInputRef.current?.click()}
              >
                <VStack spacing={2}>
                  <Icon
                    as={selectedFile ? FileJson : Upload}
                    boxSize={8}
                    color={selectedFile ? "blue.500" : mutedText}
                  />
                  {selectedFile ? (
                    <>
                      <Text color={textColor} fontSize="sm" fontWeight="500">
                        {selectedFile.name}
                      </Text>
                      <Text color={mutedText} fontSize="xs">
                        Click to change file
                      </Text>
                    </>
                  ) : (
                    <>
                      <Text color={textColor} fontSize="sm" fontWeight="500">
                        Click to select JSON file
                      </Text>
                      <Text color={mutedText} fontSize="xs">
                        or drag and drop
                      </Text>
                    </>
                  )}
                </VStack>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".json,application/json"
                  onChange={handleFileSelect}
                  style={{ display: "none" }}
                />
              </Box>
            )}
          </VStack>
        </ModalBody>

        <ModalFooter>
          <HStack spacing={3}>
            <Button
              variant="ghost"
              onClick={handleClose}
              color={textColor}
              _hover={{ bg: hoverBg }}
            >
              Cancel
            </Button>
            <Button
              colorScheme="blue"
              onClick={handleCreate}
              isDisabled={!diagramName.trim()}
            >
              Create Diagram
            </Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
}
