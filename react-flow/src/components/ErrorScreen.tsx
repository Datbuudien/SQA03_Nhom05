// src/components/ErrorScreen.tsx
import React from "react";
import {
  Box,
  Alert,
  AlertIcon,
  Button,
  VStack,
  HStack,
  useColorModeValue,
} from "@chakra-ui/react";

interface ErrorScreenProps {
  error: string;
  onRetry: () => void;
  onInitialize: () => void;
}

export const ErrorScreen: React.FC<ErrorScreenProps> = ({
  error,
  onRetry,
  onInitialize,
}) => {
  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const textColor = useColorModeValue("#24292f", "#e6edf3");

  return (
    <Box
      height="100vh"
      width="100vw"
      bg={bgColor}
      display="flex"
      alignItems="center"
      justifyContent="center"
    >
      <VStack spacing={4} maxWidth="500px">
        <Alert status="error">
          <AlertIcon />
          Error loading schema data: {error}
        </Alert>
        {/* <HStack spacing={4}>
          <Button colorScheme="blue" onClick={onRetry}>
            Retry
          </Button>
          <Button colorScheme="green" onClick={onInitialize}>
            Initialize Sample Data
          </Button>
        </HStack> */}
      </VStack>
    </Box>
  );
};
