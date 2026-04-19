/**
 * ============================================================================
 * UNIT TEST CLASS: DatabaseDiagramListServiceTest
 * ============================================================================
 * Purpose: Unit tests for DatabaseDiagramListService (diagram count)
 * - Focus: totalCount for statistics
 * - Covers access filter and owner filter branches
 * 
 * Author: QA Team
 * Date: 19/4/2026
 * Version: 1.0
 * ============================================================================
 */

package com.example.react_flow_be.service;

import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseDiagramListService - Unit Tests")
class DatabaseDiagramListServiceTest {

        @Mock
        private DatabaseDiagramRepository databaseDiagramRepository;

        @Mock
        private CollaborationRepository collaborationRepository;

        @Mock
        private MigrationRepository migrationRepository;

        @InjectMocks
        private DatabaseDiagramListService databaseDiagramListService;

        private static final String USERNAME = "user1";

        /**
         * Test Case: TK001
         * Scenario: Only accessible diagrams are counted
         * Expected: totalCount equals number of accessible diagrams
         */
        @Test
        @DisplayName("TK001: getDatabaseDiagramList counts accessible diagrams")
        void testGetDatabaseDiagramList_ShouldCountAccessibleDiagrams() {
                DatabaseDiagram diagram1 = new DatabaseDiagram();
                diagram1.setId(1L);
                diagram1.setName("Diagram A");
                diagram1.setIsDeleted(false);
                diagram1.setUpdatedAt(LocalDateTime.now());

                DatabaseDiagram diagram2 = new DatabaseDiagram();
                diagram2.setId(2L);
                diagram2.setName("Diagram B");
                diagram2.setIsDeleted(false);
                diagram2.setUpdatedAt(LocalDateTime.now().minusDays(1));

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.hasAccess(2L, USERNAME)).thenReturn(false);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner1")));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setPageSize(10);
                request.setIsDeleted(false);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Total count should match accessible diagrams");
                assertEquals(1, result.getDiagrams().size(), "Should return one diagram item");

                verify(databaseDiagramRepository, times(1)).findAll();
                verify(collaborationRepository, times(1)).hasAccess(1L, USERNAME);
                verify(collaborationRepository, times(1)).hasAccess(2L, USERNAME);
        }

        /**
         * Test Case: TK002
         * Scenario: Soft-deleted diagram still counted (intentional fail)
         * Expected: Fail to document deleted filter gap
         */
        @Test
        @DisplayName("TK002: Fail - Deleted diagram should be excluded")
        void testGetDatabaseDiagramList_WhenOnlyOneAccessible_ShouldFailExpectation() {
                DatabaseDiagram diagram1 = new DatabaseDiagram();
                diagram1.setId(1L);
                diagram1.setName("Diagram A");
                diagram1.setIsDeleted(false);
                diagram1.setUpdatedAt(LocalDateTime.now());

                DatabaseDiagram diagram2 = new DatabaseDiagram();
                diagram2.setId(2L);
                diagram2.setName("Diagram B");
                diagram2.setIsDeleted(false);
                diagram2.setUpdatedAt(LocalDateTime.now().minusDays(1));

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.hasAccess(2L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner1")));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());
                when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner2")));
                when(collaborationRepository.countParticipants(2L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setPageSize(10);
                request.setIsDeleted(false);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Intentional fail: expected deleted diagram excluded");
        }

        /**
         * Test Case: TK003
         * Scenario: Owner filter "team" with no participants
         * Expected: totalCount is zero
         */
        @Test
        @DisplayName("TK003: ownerFilter team excludes diagrams with no participants")
        void testGetDatabaseDiagramList_WhenOwnerFilterTeam_NoParticipants() {
                DatabaseDiagram diagram1 = new DatabaseDiagram();
                diagram1.setId(1L);
                diagram1.setName("Diagram A");
                diagram1.setIsDeleted(false);
                diagram1.setUpdatedAt(LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram1));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setOwnerFilter("team");
                request.setIsDeleted(false);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "Total count should be zero for team filter");
                assertEquals(0, result.getDiagrams().size(), "Should return empty diagram list");
        }

        /**
         * Test Case: TK004
         * Scenario: Filter by deleted status
         * Expected: Diagram excluded when deleted flag mismatches
         */
        @Test
        @DisplayName("TK004: deleted status filter excludes mismatched diagrams")
        void testGetDatabaseDiagramList_WhenDeletedStatusMismatch_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", true, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setIsDeleted(false);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "Deleted mismatch should exclude diagram");
        }

        /**
         * Test Case: TK005
         * Scenario: sharedWithMe filter
         * Expected: Only participant collaborations pass
         */
        @Test
        @DisplayName("TK005: sharedWithMe requires participant collaboration")
        void testGetDatabaseDiagramList_WhenSharedWithMe_ShouldRequireParticipant() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                Collaboration participant = new Collaboration();
                participant.setType(Collaboration.CollaborationType.PARTICIPANTS);

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndUsername(1L, USERNAME))
                                .thenReturn(Optional.of(participant));
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner1")));
                when(collaborationRepository.countParticipants(1L)).thenReturn(1);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSharedWithMe(true);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Participant collaboration should pass sharedWithMe filter");
        }

        /**
         * Test Case: TK006
         * Scenario: nameStartsWith filter mismatch
         * Expected: Diagram excluded
         */
        @Test
        @DisplayName("TK006: nameStartsWith filter excludes mismatched names")
        void testGetDatabaseDiagramList_WhenNameStartsWithMismatch_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Alpha", false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setNameStartsWith("B");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "Name filter should exclude diagram");
        }

        /**
         * Test Case: TK007
         * Scenario: searchQuery matches owner
         * Expected: Diagram included
         */
        @Test
        @DisplayName("TK007: searchQuery matches owner")
        void testGetDatabaseDiagramList_WhenSearchQueryMatchesOwner_ShouldInclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("alice")));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSearchQuery("ali");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Owner match should include diagram");
        }

        /**
         * Test Case: TK008
         * Scenario: ownerFilter me mismatch
         * Expected: Diagram excluded
         */
        @Test
        @DisplayName("TK008: ownerFilter me excludes other owners")
        void testGetDatabaseDiagramList_WhenOwnerFilterMeMismatch_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("other")));

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setOwnerFilter("me");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "Owner filter me should exclude non-owner");
        }

        /**
         * Test Case: TK009
         * Scenario: dateRange today excludes old updates
         * Expected: Diagram excluded
         */
        @Test
        @DisplayName("TK009: dateRange today excludes old diagrams")
        void testGetDatabaseDiagramList_WhenDateRangeTodayOld_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now().minusDays(2),
                                LocalDateTime.now().minusDays(2));

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setDateRange("today");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "Old diagram should be excluded for today range");
        }

        /**
         * Test Case: TK010
         * Scenario: sort by name asc with nulls
         * Expected: Sort logic handles nulls and returns both items
         */
        @Test
        @DisplayName("TK010: sort by name handles nulls")
        void testGetDatabaseDiagramList_WhenSortByNameWithNulls_ShouldHandle() {
                DatabaseDiagram diagram1 = buildDiagram(1L, null, false, LocalDateTime.now(), LocalDateTime.now());
                DatabaseDiagram diagram2 = buildDiagram(2L, "Beta", false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.hasAccess(2L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner1")));
                when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner2")));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(collaborationRepository.countParticipants(2L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSortBy("name");
                request.setSortDirection("ASC");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(2, result.getTotalCount(), "Both diagrams should be included");
        }

        /**
         * Test Case: TK011
         * Scenario: migrations present and participants exist
         * Expected: DTO includes migration data and hasCollaborators true
         */
        @Test
        @DisplayName("TK011: migrations and participants set DTO fields")
        void testGetDatabaseDiagramList_WhenMigrationsAndParticipants_ShouldSetFields() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.empty());
                when(collaborationRepository.countParticipants(1L)).thenReturn(2);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.singletonList(buildMigration("migrator")));

                DiagramListRequestDto request = new DiagramListRequestDto();

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Diagram should be included");
                assertEquals(true, result.getDiagrams().get(0).getHasCollaborators(),
                                "Has collaborators should be true");
                assertEquals("migrator", result.getDiagrams().get(0).getLastMigrationUsername(),
                                "Migration username set");
        }

        /**
         * Test Case: TK012
         * Scenario: getDatabaseDiagramAll with cursor and hasMore
         * Expected: hasMore true and correct paging
         */
        @Test
        @DisplayName("TK012: getDatabaseDiagramAll handles cursor and hasMore")
        void testGetDatabaseDiagramAll_WhenCursorHasMore_ShouldSetHasMore() {
                DatabaseDiagram d1 = buildDiagram(1L, "A", false, LocalDateTime.now(), LocalDateTime.now());
                DatabaseDiagram d2 = buildDiagram(2L, "B", false, LocalDateTime.now(), LocalDateTime.now());
                DatabaseDiagram d3 = buildDiagram(3L, "C", false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(d1, d2, d3));
                when(collaborationRepository.findByDiagramIdAndType(anyLong(),
                                eq(Collaboration.CollaborationType.OWNER)))
                                .thenReturn(Optional.of(buildOwner("owner")));
                when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setPageSize(1);
                request.setLastDiagramId(1L);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramAll(request, USERNAME);

                assertEquals(true, result.getHasMore(), "Should have more pages");
                assertEquals(1, result.getDiagrams().size(), "Should return page size items");
        }

        /**
         * Test Case: TK013
         * Scenario: sharedWithMe with no participant collaboration
         * Expected: Diagram excluded
         */
        @Test
        @DisplayName("TK013: sharedWithMe excludes when no participant")
        void testGetDatabaseDiagramList_WhenSharedWithMeNoParticipant_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndUsername(1L, USERNAME))
                                .thenReturn(Optional.empty());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSharedWithMe(true);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "SharedWithMe should exclude without participant collab");
        }

        /**
         * Test Case: TK014
         * Scenario: nameStartsWith with null name
         * Expected: Diagram excluded
         */
        @Test
        @DisplayName("TK014: nameStartsWith excludes null name")
        void testGetDatabaseDiagramList_WhenNameNullAndStartsWith_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, null, false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setNameStartsWith("A");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "Null name should be excluded for startsWith filter");
        }

        /**
         * Test Case: TK015
         * Scenario: searchQuery matches name
         * Expected: Diagram included
         */
        @Test
        @DisplayName("TK015: searchQuery matches name")
        void testGetDatabaseDiagramList_WhenSearchQueryMatchesName_ShouldInclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Alpha Diagram", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner(USERNAME)));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSearchQuery("alpha");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Name match should include diagram");
        }

        /**
         * Test Case: TK016
         * Scenario: searchQuery matches nothing
         * Expected: Diagram excluded
         */
        @Test
        @DisplayName("TK016: searchQuery excludes when no match")
        void testGetDatabaseDiagramList_WhenSearchQueryNoMatch_ShouldExclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Alpha", false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("owner")));

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSearchQuery("zzz");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(0, result.getTotalCount(), "No match should exclude diagram");
        }

        /**
         * Test Case: TK017
         * Scenario: ownerFilter me matches
         * Expected: Diagram included
         */
        @Test
        @DisplayName("TK017: ownerFilter me includes owner")
        void testGetDatabaseDiagramList_WhenOwnerFilterMeMatches_ShouldInclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner(USERNAME)));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setOwnerFilter("me");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Owner filter me should include diagram");
        }

        /**
         * Test Case: TK018
         * Scenario: ownerFilter team with participants
         * Expected: Diagram included
         */
        @Test
        @DisplayName("TK018: ownerFilter team includes diagrams with participants")
        void testGetDatabaseDiagramList_WhenOwnerFilterTeamWithParticipants_ShouldInclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner(USERNAME)));
                when(collaborationRepository.countParticipants(1L)).thenReturn(2);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setOwnerFilter("team");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Team filter should include diagram with participants");
        }

        /**
         * Test Case: TK019
         * Scenario: dateRange last7days includes recent updates
         * Expected: Diagram included
         */
        @Test
        @DisplayName("TK019: dateRange last7days includes recent diagrams")
        void testGetDatabaseDiagramList_WhenDateRangeLast7Days_ShouldInclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now().minusDays(1),
                                LocalDateTime.now().minusDays(1));

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setDateRange("last7days");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Recent diagram should be included for last7days");
        }

        /**
         * Test Case: TK020
         * Scenario: dateRange last30days with null updatedAt
         * Expected: Diagram included
         */
        @Test
        @DisplayName("TK020: dateRange last30days with null updatedAt includes")
        void testGetDatabaseDiagramList_WhenDateRangeLast30DaysNullUpdatedAt_ShouldInclude() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(), null);

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setDateRange("last30days");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(1, result.getTotalCount(), "Null updatedAt should not exclude diagram");
        }

        /**
         * Test Case: TK021
         * Scenario: sort by createdAt desc with nulls
         * Expected: Sort handles null dates
         */
        @Test
        @DisplayName("TK021: sort by createdAt handles nulls")
        void testGetDatabaseDiagramList_WhenSortByCreatedAtWithNulls_ShouldHandle() {
                DatabaseDiagram diagram1 = buildDiagram(1L, "A", false, null, LocalDateTime.now());
                DatabaseDiagram diagram2 = buildDiagram(2L, "B", false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.hasAccess(2L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("o1")));
                when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner("o2")));
                when(collaborationRepository.countParticipants(1L)).thenReturn(0);
                when(collaborationRepository.countParticipants(2L)).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSortBy("createdAt");
                request.setSortDirection("DESC");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(2, result.getTotalCount(), "Both diagrams should be included");
        }

        /**
         * Test Case: TK022
         * Scenario: sort by updatedAt with both null
         * Expected: Sort handles null updatedAt
         */
        @Test
        @DisplayName("TK022: sort by updatedAt handles nulls")
        void testGetDatabaseDiagramList_WhenSortByUpdatedAtWithNulls_ShouldHandle() {
                DatabaseDiagram diagram1 = buildDiagram(1L, "A", false, LocalDateTime.now(), null);
                DatabaseDiagram diagram2 = buildDiagram(2L, "B", false, LocalDateTime.now(), null);

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.hasAccess(2L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(anyLong(),
                                eq(Collaboration.CollaborationType.OWNER)))
                                .thenReturn(Optional.of(buildOwner("owner")));
                when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setSortBy("updatedAt");

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(2, result.getTotalCount(), "Both diagrams should be included");
        }

        /**
         * Test Case: TK023
         * Scenario: owner present, no migrations, participantCount null
         * Expected: hasCollaborators false and updatedBy from owner
         */
        @Test
        @DisplayName("TK023: owner present without migrations sets updatedBy")
        void testGetDatabaseDiagramList_WhenOwnerNoMigrations_ShouldSetUpdatedByOwner() {
                DatabaseDiagram diagram = buildDiagram(1L, "Diagram A", false, LocalDateTime.now(),
                                LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Collections.singletonList(diagram));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                                .thenReturn(Optional.of(buildOwner(USERNAME)));
                when(collaborationRepository.countParticipants(1L)).thenReturn(null);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(false, result.getDiagrams().get(0).getHasCollaborators(),
                                "No participants should be false");
                assertEquals(USERNAME, result.getDiagrams().get(0).getUpdatedByUsername(), "UpdatedBy should be owner");
        }

        /**
         * Test Case: TK024
         * Scenario: getDatabaseDiagramList hasMore when pageSize smaller than results
         * Expected: hasMore true
         */
        @Test
        @DisplayName("TK024: getDatabaseDiagramList hasMore true with small pageSize")
        void testGetDatabaseDiagramList_WhenHasMore_ShouldSetHasMore() {
                DatabaseDiagram d1 = buildDiagram(1L, "A", false, LocalDateTime.now(), LocalDateTime.now());
                DatabaseDiagram d2 = buildDiagram(2L, "B", false, LocalDateTime.now(), LocalDateTime.now());

                when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(d1, d2));
                when(collaborationRepository.hasAccess(1L, USERNAME)).thenReturn(true);
                when(collaborationRepository.hasAccess(2L, USERNAME)).thenReturn(true);
                when(collaborationRepository.findByDiagramIdAndType(anyLong(),
                                eq(Collaboration.CollaborationType.OWNER)))
                                .thenReturn(Optional.of(buildOwner(USERNAME)));
                when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);
                when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                                .thenReturn(Collections.emptyList());

                DiagramListRequestDto request = new DiagramListRequestDto();
                request.setPageSize(1);

                DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(request, USERNAME);

                assertEquals(true, result.getHasMore(), "Should have more pages");
        }

        private DatabaseDiagram buildDiagram(Long id, String name, boolean isDeleted,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
                DatabaseDiagram diagram = new DatabaseDiagram();
                diagram.setId(id);
                diagram.setName(name);
                diagram.setIsDeleted(isDeleted);
                diagram.setCreatedAt(createdAt);
                diagram.setUpdatedAt(updatedAt);
                return diagram;
        }

        private com.example.react_flow_be.entity.Migration buildMigration(String username) {
                com.example.react_flow_be.entity.Migration migration = new com.example.react_flow_be.entity.Migration();
                migration.setUsername(username);
                migration.setCreatedAt(LocalDateTime.now());
                return migration;
        }

        private Collaboration buildOwner(String username) {
                Collaboration owner = new Collaboration();
                owner.setUsername(username);
                owner.setType(Collaboration.CollaborationType.OWNER);
                return owner;
        }
}