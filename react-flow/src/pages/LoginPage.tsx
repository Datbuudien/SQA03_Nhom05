import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Box,
  Button,
  Container,
  Heading,
  Text,
  VStack,
  Icon,
  Alert,
  AlertIcon,
  AlertDescription,
  useColorModeValue,
} from "@chakra-ui/react";
import { ThemeToggle } from "../components/page/ThemeToggle";
import { FcGoogle } from "react-icons/fc";
import { DiDatabase } from "react-icons/di";

export function LoginPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [searchParams] = useSearchParams();
  const error = searchParams.get("error");
  const token = searchParams.get("token");

  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");

  useEffect(() => {
    // Check if already logged in
    const jwtToken = document.cookie
      .split("; ")
      .find((row) => row.startsWith("jwt="))
      ?.split("=")[1];

    if (jwtToken || token) {
      navigate("/home");
    }
  }, [token, navigate]);

  const handleGoogleLogin = () => {
    window.location.href = "http://localhost:8080/oauth2/authorization/google";
  };

  const getErrorMessage = (errorCode: string | null) => {
    switch (errorCode) {
      case "account_not_found":
        return "Account not found. Please contact administrator.";
      case "server_error":
        return "Server error occurred. Please try again later.";
      default:
        return null;
    }
  };

  const errorMessage = getErrorMessage(error);

  return (
    <Box minH="100vh" bg={bgColor} position="relative">
      <Box position="absolute" top={4} right={4}>
        <ThemeToggle />
      </Box>

      <Container maxW="450px" centerContent pt="15vh">
        <VStack spacing={4} w="full">
          {/* Logo */}
          <Box bg={bgColor} borderColor={borderColor}>
            <Icon as={DiDatabase} boxSize={16} color={textColor} />
          </Box>

          {/* Heading */}
          <Heading size="md" color={textColor} fontWeight="500" pb="20px">
            Sign in to Database Diagram
          </Heading>

          {/* Card */}
          <Box w="full" bg={cardBg} p={4}>
            <VStack spacing={3}>
              {errorMessage && (
                <Alert status="error" borderRadius="6px" fontSize="sm">
                  <AlertIcon boxSize={4} />
                  <AlertDescription>{errorMessage}</AlertDescription>
                </Alert>
              )}

              <Button
                onClick={handleGoogleLogin}
                w="full"
                h="50px"
                bg={cardBg}
                border="1px"
                borderColor={borderColor}
                color={textColor}
                fontSize="16px"
                fontWeight="500"
                leftIcon={<Icon as={FcGoogle} boxSize={6} />}
                _hover={{
                  bg: useColorModeValue("#f3f4f6", "#1c2128"),
                  borderColor: useColorModeValue("#1b1f2326", "#8b949e"),
                }}
                _active={{
                  bg: useColorModeValue("#ebecf0", "#272e3a"),
                }}
              >
                {loading ? "..." : "Continue with Google"}
              </Button>
            </VStack>
          </Box>

          {/* Footer text */}
          <Text fontSize="xs" color="gray.500" textAlign="center" maxW="350px">
            By signing in, you agree to our Terms of Service and Privacy Policy
          </Text>
        </VStack>
      </Container>
    </Box>
  );
}
