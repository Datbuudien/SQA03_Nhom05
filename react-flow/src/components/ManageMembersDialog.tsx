// src/components/ManageMembersDialog.tsx
import React, { useState, useEffect } from "react";
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalCloseButton,
  VStack,
  HStack,
  Text,
  Input,
  Button,
  Avatar,
  Badge,
  Select,
  IconButton,
  Divider,
  Box,
  useColorModeValue,
  Spinner,
  Alert,
  AlertIcon,
  AlertDescription,
  Image,
} from "@chakra-ui/react";
import { FiSearch, FiTrash2, FiUserPlus } from "react-icons/fi";
import { useParams } from "react-router-dom";
import { collaborationApiService } from "../services/collaborationApiService";
import { CollaborationMember, AccountDTO } from "../types/collaboration.types";
import { usePermission } from "../hooks/usePermission";

interface ManageMembersDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ManageMembersDialog: React.FC<ManageMembersDialogProps> = ({
  isOpen,
  onClose,
}) => {
  const { diagramId } = useParams<{ diagramId: string }>();
  const { canEdit, permission } = usePermission();
  const [members, setMembers] = useState<CollaborationMember[]>([]);
  const [memberDetails, setMemberDetails] = useState<Map<string, AccountDTO>>(
    new Map()
  );
  const [loading, setLoading] = useState(false);
  const [searchEmail, setSearchEmail] = useState("");
  const [searchResult, setSearchResult] = useState<AccountDTO | null>(null);
  const [searching, setSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);

  const bgColor = useColorModeValue("white", "#2d3748");
  const borderColor = useColorModeValue("gray.200", "gray.600");
  const hoverBg = useColorModeValue("gray.50", "gray.700");

  const loadMemberDetails = async (members: CollaborationMember[]) => {
    try {
      const promises = members.map(async (member) => {
        try {
          const email = `${member.username}@gmail.com`;
          const account = await collaborationApiService.searchAccountByEmail(
            email
          );
          return { username: member.username, account };
        } catch (error) {
          console.error(`Failed to load details for ${member.username}`);
          return null;
        }
      });

      const results = await Promise.all(promises);

      const detailsMap = new Map<string, AccountDTO>();
      results.forEach((result) => {
        if (result) {
          detailsMap.set(result.username, result.account);
        }
      });

      setMemberDetails(detailsMap);
    } catch (error) {
      console.error("Failed to load member details", error);
    }
  };

  // Load collaborations - cập nhật để dùng loadMemberDetails mới
  const loadCollaborations = async () => {
    if (!diagramId) return;

    setLoading(true);
    setError(null);
    try {
      const data = await collaborationApiService.getCollaborations(diagramId);
      setMembers(data);

      // Load details for all members in parallel
      await loadMemberDetails(data);
      console.log("members: ", memberDetails);
    } catch (error: any) {
      setError(error.message || "Failed to load members");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      loadCollaborations();
      setSearchEmail("");
      setSearchResult(null);
      setSearchError(null);
    }
  }, [isOpen, diagramId]);

  // Search account by email
  const handleSearch = async () => {
    if (!searchEmail.trim()) return;

    setSearching(true);
    setSearchError(null);
    try {
      const account = await collaborationApiService.searchAccountByEmail(
        searchEmail
      );
      setSearchResult(account);
    } catch (error: any) {
      setSearchError(error.message || "Email not found");
      setSearchResult(null);
    } finally {
      setSearching(false);
    }
  };

  // Add collaborator
  const handleAddCollaborator = async (permission: "VIEW" | "FULL_ACCESS") => {
    if (!searchResult || !diagramId) return;

    setError(null);
    try {
      const response = await collaborationApiService.addCollaborator(
        diagramId,
        searchResult.username,
        permission
      );

      setSearchEmail("");
      setSearchResult(null);
      setSearchError(null);
      loadCollaborations();
    } catch (error: any) {
      setError(error.message || "Failed to add member");
    }
  };

  // Update permission
  const handleUpdatePermission = async (
    collaborationId: number,
    permission: "VIEW" | "FULL_ACCESS"
  ) => {
    if (!diagramId) return;

    setError(null);
    try {
      await collaborationApiService.updatePermission(
        diagramId,
        collaborationId,
        permission
      );
      loadCollaborations();
    } catch (error: any) {
      setError(error.message || "Failed to update permission");
    }
  };

  // Remove collaborator
  const handleRemoveCollaborator = async (collaborationId: number) => {
    if (!diagramId) return;

    setError(null);
    try {
      await collaborationApiService.removeCollaborator(
        diagramId,
        collaborationId
      );
      loadCollaborations();
    } catch (error: any) {
      setError(error.message || "Failed to remove member");
    }
  };

  const owner = members.find((m) => m.type === "OWNER");
  const participants = members.filter((m) => m.type === "PARTICIPANTS");

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      size="xl"
      scrollBehavior="inside"
      isCentered
    >
      <ModalOverlay />
      <ModalContent bg={bgColor}>
        <ModalHeader>Manage Members</ModalHeader>
        <ModalCloseButton />
        <ModalBody pb={6}>
          <VStack spacing={4} align="stretch">
            {/* Error Alert */}
            {error && (
              <Alert status="error" borderRadius="md">
                <AlertIcon />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            {/* ✅ CHỈ HIỆN Search Section khi có quyền edit */}
            {canEdit && (
              <Box>
                <Text fontWeight="medium" mb={2}>
                  Add new member
                </Text>
                <HStack>
                  <Input
                    placeholder="Enter email (e.g. username@gmail.com)"
                    value={searchEmail}
                    onChange={(e) => {
                      setSearchEmail(e.target.value);
                      setSearchError(null);
                    }}
                    onKeyPress={(e) => e.key === "Enter" && handleSearch()}
                    size="sm"
                  />
                  <IconButton
                    aria-label="Search"
                    icon={searching ? <Spinner size="sm" /> : <FiSearch />}
                    onClick={handleSearch}
                    isDisabled={!searchEmail.trim() || searching}
                    size="sm"
                  />
                </HStack>

                {searchError && (
                  <Text color="red.500" fontSize="sm" mt={2}>
                    {searchError}
                  </Text>
                )}

                {searchResult && (
                  <Box
                    mt={3}
                    p={3}
                    border="1px solid"
                    borderColor={borderColor}
                    borderRadius="md"
                    maxH={"200px"}
                    overflowY={"auto"}
                  >
                    <HStack justify="space-between">
                      <HStack>
                        <Image
                          boxSize="35px"
                          // name={searchResult.name}
                          src={searchResult.picture}
                        />
                        <VStack align="start" spacing={0}>
                          <Text fontSize="sm" fontWeight="medium">
                            {searchResult.name}
                          </Text>
                          <Text fontSize="xs" color="gray.500">
                            {searchResult.username}@gmail.com
                          </Text>
                        </VStack>
                      </HStack>
                      <HStack>
                        <Button
                          size="sm"
                          leftIcon={<FiUserPlus />}
                          variant="ghost"
                          onClick={() => handleAddCollaborator("VIEW")}
                        >
                          View
                        </Button>
                        <Button
                          size="sm"
                          leftIcon={<FiUserPlus />}
                          onClick={() => handleAddCollaborator("FULL_ACCESS")}
                        >
                          Full Access
                        </Button>
                      </HStack>
                    </HStack>
                  </Box>
                )}
              </Box>
            )}
            {canEdit && <Divider />}{" "}
            {/* ✅ CHỈ HIỆN Divider nếu có search section */}
            {/* Members List */}
            <Box>
              <Text fontWeight="medium" mb={3}>
                Members ({members.length})
              </Text>

              {loading ? (
                <Box textAlign="center" py={4}>
                  <Spinner />
                </Box>
              ) : (
                <VStack spacing={2} align="stretch">
                  {/* Owner */}
                  {owner && (
                    <Box
                      p={3}
                      border="1px solid"
                      borderColor={borderColor}
                      borderRadius="md"
                    >
                      <HStack justify="space-between">
                        <HStack>
                          <Avatar
                            size="sm"
                            name={memberDetails.get(owner.username)?.name}
                            src={memberDetails.get(owner.username)?.picture}
                          />
                          <VStack align="start" spacing={0}>
                            <Text fontSize="sm" fontWeight="medium">
                              {memberDetails.get(owner.username)?.name ||
                                owner.username}
                            </Text>
                            <Text fontSize="xs" color="gray.500">
                              {owner.username}@gmail.com
                            </Text>
                          </VStack>
                        </HStack>
                        <Badge fontSize="xs" px={2} py={1} textTransform="none">
                          Owner
                        </Badge>
                      </HStack>
                    </Box>
                  )}

                  {/* ✅ SỬA: Participants - hiện như Owner (chỉ badge) */}
                  {participants.map((member) => {
                    const details = memberDetails.get(member.username);
                    return (
                      <Box
                        key={member.id}
                        p={3}
                        border="1px solid"
                        borderColor={borderColor}
                        borderRadius="md"
                        _hover={{ bg: hoverBg }}
                      >
                        <HStack justify="space-between">
                          <HStack flex={1}>
                            <Avatar
                              size="sm"
                              name={details?.name}
                              src={details?.picture}
                            />
                            <VStack align="start" spacing={0}>
                              <Text fontSize="sm" fontWeight="medium">
                                {details?.name || member.username}
                              </Text>
                              <Text fontSize="xs" color="gray.500">
                                {member.username}@gmail.com
                              </Text>
                            </VStack>
                          </HStack>

                          {/* ✅ SỬA: Nếu VIEW mode thì chỉ hiện Badge, nếu FULL_ACCESS thì hiện controls */}
                          {canEdit ? (
                            <HStack>
                              <Select
                                size="sm"
                                value={member.permission}
                                onChange={(e) =>
                                  handleUpdatePermission(
                                    member.id,
                                    e.target.value as "VIEW" | "FULL_ACCESS"
                                  )
                                }
                                width="130px"
                                fontSize="sm"
                              >
                                <option value="VIEW">View</option>
                                <option value="FULL_ACCESS">Full Access</option>
                              </Select>
                              <IconButton
                                aria-label="Remove member"
                                icon={<FiTrash2 />}
                                size="sm"
                                variant="ghost"
                                onClick={() =>
                                  handleRemoveCollaborator(member.id)
                                }
                              />
                            </HStack>
                          ) : (
                            <Badge
                              fontSize="xs"
                              px={2}
                              py={1}
                              textTransform="none"
                              colorScheme={
                                member.permission === "FULL_ACCESS"
                                  ? "blue"
                                  : "gray"
                              }
                            >
                              {member.permission === "FULL_ACCESS"
                                ? "Full Access"
                                : "View"}
                            </Badge>
                          )}
                        </HStack>
                      </Box>
                    );
                  })}

                  {participants.length === 0 && (
                    <Text
                      textAlign="center"
                      color="gray.500"
                      py={4}
                      fontSize="sm"
                    >
                      No members yet
                    </Text>
                  )}
                </VStack>
              )}
            </Box>
          </VStack>
        </ModalBody>
      </ModalContent>
    </Modal>
  );
};
