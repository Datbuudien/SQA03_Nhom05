package com.example.react_flow_be.service;

import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseDiagramService Unit Tests - Branch Coverage Level 2")
class DatabaseDiagramServiceTest {

    @Mock
    private DatabaseDiagramRepository mockDatabaseDiagramRepository;

    @Mock
    private CollaborationService mockCollaborationService;

    @InjectMocks
    private DatabaseDiagramService serviceUnderTest;

    /**
     * TestCaseID: UN_DDS_001
     * Mục tiêu:
     * - Xác minh getDatabaseDiagramById trả về diagram khi tồn tại.
     * CheckDB:
     * - Verify truy cập đúng repository.findById(id).
     * Rollback:
     * - Unit test dùng mock, không làm thay đổi DB.
     */
    @Test
    void getDatabaseDiagramById_shouldReturnDiagram_whenDiagramExists() {
        DatabaseDiagram existingDiagram = new DatabaseDiagram();
        existingDiagram.setId(10L);
        existingDiagram.setName("Existing Diagram");

        when(mockDatabaseDiagramRepository.findById(10L)).thenReturn(Optional.of(existingDiagram));

        DatabaseDiagram actualDiagram = serviceUnderTest.getDatabaseDiagramById(10L);

        assertSame(existingDiagram, actualDiagram);
        verify(mockDatabaseDiagramRepository, times(1)).findById(10L);
    }

    /**
     * TestCaseID: UN_DDS_002
     * Mục tiêu:
     * - Xác minh getDatabaseDiagramById ném EntityNotFoundException khi không tồn
     * tại.
     * CheckDB:
     * - Verify truy cập đúng repository.findById(id).
     * Rollback:
     * - Không có thay đổi DB.
     */
    @Test
    void getDatabaseDiagramById_shouldThrowEntityNotFoundException_whenDiagramDoesNotExist() {
        when(mockDatabaseDiagramRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> serviceUnderTest.getDatabaseDiagramById(99L));
        verify(mockDatabaseDiagramRepository, times(1)).findById(99L);
    }

    /**
     * TestCaseID: UN_DDS_003
     * Mục tiêu:
     * - Xác minh updateDiagramName trả true khi DB update thành công.
     * CheckDB:
     * - Verify repository.updateNameById được gọi với id và newName đúng.
     * Rollback:
     * - Unit test mock repository, không commit DB thật.
     */
    @Test
    void updateDiagramName_shouldReturnTrue_whenRepositoryUpdatesAtLeastOneRow() {
        when(mockDatabaseDiagramRepository.updateNameById(1L, "Renamed Diagram")).thenReturn(1);

        Boolean actualResult = serviceUnderTest.updateDiagramName(1L, "Renamed Diagram");

        assertTrue(actualResult);
        verify(mockDatabaseDiagramRepository, times(1)).updateNameById(1L, "Renamed Diagram");
    }

    /**
     * TestCaseID: UN_DDS_004
     * Mục tiêu:
     * - Xác minh updateDiagramName trả false khi không có bản ghi nào được cập
     * nhật.
     * CheckDB:
     * - Verify repository.updateNameById được gọi đúng tham số.
     * Rollback:
     * - Không thay đổi DB thật.
     */
    @Test
    void updateDiagramName_shouldReturnFalse_whenRepositoryUpdatesZeroRow() {
        when(mockDatabaseDiagramRepository.updateNameById(2L, "Unknown Diagram")).thenReturn(0);

        Boolean actualResult = serviceUnderTest.updateDiagramName(2L, "Unknown Diagram");

        assertFalse(actualResult);
        verify(mockDatabaseDiagramRepository, times(1)).updateNameById(2L, "Unknown Diagram");
    }

    /**
     * TestCaseID: UN_DDS_005
     * Mục tiêu:
     * - Xác minh createBlankDiagram thiết lập default đúng và gọi
     * collaborationService.createOwner.
     * CheckDB:
     * - Verify repository.save được gọi với đầy đủ giá trị mặc định.
     * - Verify createOwner(diagramId, username) được gọi đúng.
     * Rollback:
     * - Unit test dùng mock, không có thay đổi DB thật.
     */
    @Test
    void createBlankDiagram_shouldSetDefaultValuesAndCreateOwnerCollaboration() {
        when(mockDatabaseDiagramRepository.save(any(DatabaseDiagram.class))).thenAnswer(invocation -> {
            DatabaseDiagram toSave = invocation.getArgument(0);
            toSave.setId(123L);
            return toSave;
        });

        DatabaseDiagram createdDiagram = serviceUnderTest.createBlankDiagram("Blank ERD", "alice");

        assertEquals(123L, createdDiagram.getId());
        assertEquals("Blank ERD", createdDiagram.getName());
        assertEquals("", createdDiagram.getDescription());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, createdDiagram.getDatabaseType());
        assertEquals("8.0", createdDiagram.getVersion());
        assertEquals("utf8mb4", createdDiagram.getCharset());
        assertEquals("utf8mb4_unicode_ci", createdDiagram.getCollation());
        assertFalse(createdDiagram.getIsPublic());
        assertFalse(createdDiagram.getIsTemplate());
        assertEquals(1.0, createdDiagram.getZoomLevel());
        assertEquals(0.0, createdDiagram.getPanX());
        assertEquals(0.0, createdDiagram.getPanY());

        ArgumentCaptor<DatabaseDiagram> diagramCaptor = ArgumentCaptor.forClass(DatabaseDiagram.class);
        verify(mockDatabaseDiagramRepository, times(1)).save(diagramCaptor.capture());
        verify(mockCollaborationService, times(1)).createOwner(123L, "alice");

        DatabaseDiagram savedDiagram = diagramCaptor.getValue();
        assertEquals("Blank ERD", savedDiagram.getName());
    }

    /**
     * TestCaseID: UN_DDS_006
     * Mục tiêu:
     * - Xác minh createSampleDatabaseDiagram tạo mẫu với giá trị mặc định của
     * diagram mẫu.
     * CheckDB:
     * - Verify repository.save được gọi với isTemplate = true và thông tin mẫu.
     * Rollback:
     * - Unit test mock repository, không thay đổi DB thật.
     */
    @Test
    void createSampleDatabaseDiagram_shouldSaveSampleDiagramWithExpectedDefaults() {
        when(mockDatabaseDiagramRepository.save(any(DatabaseDiagram.class))).thenAnswer(invocation -> {
            DatabaseDiagram toSave = invocation.getArgument(0);
            toSave.setId(456L);
            return toSave;
        });

        DatabaseDiagram createdSampleDiagram = serviceUnderTest.createSampleDatabaseDiagram();

        assertEquals(456L, createdSampleDiagram.getId());
        assertEquals("Blog System", createdSampleDiagram.getName());
        assertEquals("Sample blog system database schema", createdSampleDiagram.getDescription());
        assertEquals(DatabaseDiagram.DatabaseType.MYSQL, createdSampleDiagram.getDatabaseType());
        assertEquals("8.0", createdSampleDiagram.getVersion());
        assertEquals("utf8mb4", createdSampleDiagram.getCharset());
        assertEquals("utf8mb4_unicode_ci", createdSampleDiagram.getCollation());
        assertFalse(createdSampleDiagram.getIsPublic());
        assertTrue(createdSampleDiagram.getIsTemplate());
        assertEquals(1.0, createdSampleDiagram.getZoomLevel());
        assertEquals(0.0, createdSampleDiagram.getPanX());
        assertEquals(0.0, createdSampleDiagram.getPanY());

        verify(mockDatabaseDiagramRepository, times(1)).save(any(DatabaseDiagram.class));
        verifyNoInteractions(mockCollaborationService);
    }
}
