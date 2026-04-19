// src/components/LoadingScreen.tsx
import React from "react";
import { Box, Spinner, useColorModeValue, VStack } from "@chakra-ui/react";

interface LoadingScreenProps {
  message?: string;
}

export const LoadingScreen: React.FC<LoadingScreenProps> = ({
  message = "Loading schema data...",
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
      <VStack spacing={4}>
        <Spinner size="xl" color={textColor} thickness="4px" />
        <Box color={textColor} fontSize="lg">
          {message}
        </Box>
      </VStack>
    </Box>
  );
};
