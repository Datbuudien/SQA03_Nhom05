import { useState, useEffect } from "react";
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
  Avatar,
  useColorModeValue,
  Flex,
  Input,
  FormControl,
  FormLabel,
  Divider,
  Badge,
  useToast,
  Spinner,
} from "@chakra-ui/react";
import { User, Mail, Calendar, Phone, IdCard, Shield } from "lucide-react";

interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  phoneNumber?: string;
  birthDay?: string;
  cccd?: string;
  picture?: string;
  role: string;
  provider?: string;
  createdAt: string;
}

export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const hoverBg = useColorModeValue("#f6f8fa", "#1c2128");
  const mutedText = useColorModeValue("#57606a", "#8b949e");
  const inputBg = useColorModeValue("#f6f8fa", "#0d1117");

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await fetch("http://localhost:8080/account/me", {
        credentials: "include",
      });

      if (response.ok) {
        const data = await response.json();
        setProfile(data);
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to load profile",
        status: "error",
        duration: 3000,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    if (!profile) return;

    setSaving(true);
    try {
      const response = await fetch(
        `http://localhost:8080/account/${profile.id}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify(profile),
        }
      );

      if (response.ok) {
        toast({
          title: "Success",
          description: "Profile updated successfully",
          status: "success",
          duration: 3000,
        });
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to update profile",
        status: "error",
        duration: 3000,
      });
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Flex justify="center" align="center" minH="400px">
        <Spinner size="xl" />
      </Flex>
    );
  }

  if (!profile) {
    return (
      <Card bg={cardBg} borderColor={borderColor} border="1px">
        <CardBody>
          <Text color={textColor}>Failed to load profile</Text>
        </CardBody>
      </Card>
    );
  }

  return (
    <Box w="full" maxW="100%" overflowX="hidden">
      <Box mb={8}>
        <Heading size="xl" mb={2} color={textColor} fontWeight="600">
          Profile Settings
        </Heading>
        <Text color={mutedText} fontSize="14px">
          Manage your account information and preferences
        </Text>
      </Box>

      {/* Profile Header Card */}
      <Card bg={cardBg} borderColor={borderColor} mb={6} shadow={"none"}>
        <CardBody>
          <HStack spacing={6}>
            <Avatar
              size="2xl"
              name={`${profile.firstName} ${profile.lastName}`}
              src={profile.picture}
            />
            <VStack align="start" spacing={2}>
              <Heading size="lg" color={textColor} fontWeight="600">
                {profile.firstName} {profile.lastName}
              </Heading>
              <HStack spacing={2}>
                <Text fontSize="md" color={mutedText}>
                  @{profile.username}
                </Text>
                <Badge colorScheme="blue">{profile.role}</Badge>
                {profile.provider && (
                  <Badge colorScheme="green">{profile.provider}</Badge>
                )}
              </HStack>
              <Text fontSize="sm" color={mutedText}>
                Member since {new Date(profile.createdAt).toLocaleDateString()}
              </Text>
            </VStack>
          </HStack>
        </CardBody>
      </Card>

      {/* Personal Information */}
      <Card bg={cardBg} shadow={"none"} mb={6}>
        <CardBody>
          <VStack spacing={4} align="stretch">
            <HStack spacing={4}>
              <FormControl>
                <FormLabel fontSize="sm" color={mutedText}>
                  First Name
                </FormLabel>
                <Input
                  value={profile.firstName}
                  onChange={(e) =>
                    setProfile({ ...profile, firstName: e.target.value })
                  }
                  bg={inputBg}
                  borderColor={borderColor}
                  color={textColor}
                />
              </FormControl>
              <FormControl>
                <FormLabel fontSize="sm" color={mutedText}>
                  Last Name
                </FormLabel>
                <Input
                  value={profile.lastName}
                  onChange={(e) =>
                    setProfile({ ...profile, lastName: e.target.value })
                  }
                  bg={inputBg}
                  borderColor={borderColor}
                  color={textColor}
                />
              </FormControl>
            </HStack>

            <FormControl>
              <FormLabel fontSize="sm" color={mutedText}>
                <HStack>
                  <Icon as={Mail} boxSize={4} />
                  <Text>Email</Text>
                </HStack>
              </FormLabel>
              <Input
                value={profile.email}
                isReadOnly
                bg={inputBg}
                borderColor={borderColor}
                color={textColor}
                opacity={0.7}
              />
            </FormControl>

            <FormControl>
              <FormLabel fontSize="sm" color={mutedText}>
                <HStack>
                  <Icon as={Phone} boxSize={4} />
                  <Text>Phone Number</Text>
                </HStack>
              </FormLabel>
              <Input
                value={profile.phoneNumber || ""}
                onChange={(e) =>
                  setProfile({ ...profile, phoneNumber: e.target.value })
                }
                placeholder="Enter phone number"
                bg={inputBg}
                borderColor={borderColor}
                color={textColor}
              />
            </FormControl>

            <FormControl>
              <FormLabel fontSize="sm" color={mutedText}>
                <HStack>
                  <Icon as={Calendar} boxSize={4} />
                  <Text>Birthday</Text>
                </HStack>
              </FormLabel>
              <Input
                type="date"
                value={profile.birthDay ? profile.birthDay.split("T")[0] : ""}
                onChange={(e) =>
                  setProfile({ ...profile, birthDay: e.target.value })
                }
                bg={inputBg}
                borderColor={borderColor}
                color={textColor}
              />
            </FormControl>
          </VStack>
        </CardBody>
      </Card>

      {/* Save Button */}
      <Flex justify="flex-start" marginStart={5}>
        <Button
          colorScheme="blue"
          onClick={handleSave}
          isLoading={saving}
          loadingText="Saving..."
        >
          Save Changes
        </Button>
      </Flex>
    </Box>
  );
}
