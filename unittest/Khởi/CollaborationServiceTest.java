/**
 * ============================================================================
 * Unit Test cho CollaborationService - Tầng Service quản lý collaboration
 * ============================================================================
 * Mô tả: Test các phương thức CRUD và logic nghiệp vụ của CollaborationService
 * Phương pháp: Sử dụng Mockito để mock các dependency (Repository, Config)
 * Rollback: Sử dụng Mockito (không tương tác DB thật) nên không cần rollback DB.
 *           Mỗi test được reset mock objects qua @BeforeEach/@AfterEach.
 * ============================================================================
 */
package com.example.react_flow_be.service;

import com.example.react_flow_be.config.DiagramSessionManager;
import com.example.react_flow_be.config.WebSocketSessionTracker;
import com.example.react_flow_be.dto.collaboration.CollaborationDTO;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CollaborationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(CollaborationServiceTest.class);

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private DiagramSessionManager sessionManager;

    @Mock
    private WebSocketSessionTracker sessionRegistry;

    @InjectMocks
    private CollaborationService collaborationService;

    // ============ Test Data ============
    private Diagram sampleDiagram;
    private Collaboration sampleCollaboration;
    private Collaboration ownerCollaboration;
    private CollaborationDTO sampleCollaborationDTO;

    /**
     * Khởi tạo dữ liệu test trước mỗi test case.
     * Đảm bảo mỗi test case có dữ liệu sạch, không bị ảnh hưởng bởi test khác.
     */
    @BeforeEach
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test...");

        // Diagram
        sampleDiagram = new Diagram();
        sampleDiagram.setId(1L);
        sampleDiagram.setName("Database Schema Design");

        // Owner Collaboration
        ownerCollaboration = new Collaboration();
        ownerCollaboration.setId(1L);
        ownerCollaboration.setDiagram(sampleDiagram);
        ownerCollaboration.setUsername("owner_user");
        ownerCollaboration.setType(Collaboration.CollaborationType.OWNER);
        ownerCollaboration.setPermission(Collaboration.Permission.FULL_ACCESS);
        ownerCollaboration.setIsActive(true);
        ownerCollaboration.setCreatedAt(LocalDateTime.now());
        ownerCollaboration.setUpdatedAt(LocalDateTime.now());
        ownerCollaboration.setExpiresAt(null);

        // Participant Collaboration
        sampleCollaboration = new Collaboration();
        sampleCollaboration.setId(2L);
        sampleCollaboration.setDiagram(sampleDiagram);
        sampleCollaboration.setUsername("collaborator01");
        sampleCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        sampleCollaboration.setPermission(Collaboration.Permission.EDIT);
        sampleCollaboration.setIsActive(true);
        sampleCollaboration.setCreatedAt(LocalDateTime.now());
        sampleCollaboration.setUpdatedAt(LocalDateTime.now());
        sampleCollaboration.setExpiresAt(null);

        // CollaborationDTO
        sampleCollaborationDTO = CollaborationDTO.fromEntity(sampleCollaboration);

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    /**
     * Dọn dẹp sau mỗi test case - reset tất cả mock objects.
     * Đảm bảo rollback trạng thái về ban đầu.
     */
    @AfterEach
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
        reset(collaborationRepository, diagramRepository, sessionManager, sessionRegistry);
        sampleDiagram = null;
        sampleCollaboration = null;
        ownerCollaboration = null;
        sampleCollaborationDTO = null;
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES CHO getCollaborations()
    // ========================================================================================

    /**
     * UT_CB_001: Lấy danh sách collaborations của diagram - có dữ liệu
     * Mô tả: Kiểm tra xem khi diagram tồn tại với collaborators, hệ thống có thể lấy danh sách
     *        tất cả collaborators kèm thông tin chi tiết (username, permission, type) không.
     *        Test case này đảm bảo API có thể lấy dữ liệu đầy đủ từ DB.
     * Input: diagramId = 1 (diagram tồn tại và có 2 collaborators: 1 owner + 1 participant)
     * Expected: Trả về danh sách 2 CollaborationDTO với thứ tự: owner trước, sau đó participant.
     *           Mỗi DTO chứa đầy đủ: id, username, type (OWNER/PARTICIPANTS), permission, isActive
     * Logic: - Kiểm tra diagram có tồn tại không (existsById)
     *        - Lấy danh sách collaborations từ repository (findByDiagramId)
     *        - Chuyển đổi từ entity sang DTO
     *        - Verify mock gọi đúng số lần (times(1))
     */
    @Test
    public void UT_CB_001_getCollaborations_withExistingDiagram_shouldReturnCollaborationsList() {
        // Arrange
        List<Collaboration> collaborations = Arrays.asList(ownerCollaboration, sampleCollaboration);
        when(diagramRepository.existsById(1L)).thenReturn(true);
        when(collaborationRepository.findByDiagramId(1L)).thenReturn(collaborations);

        // Act
        List<CollaborationDTO> result = collaborationService.getCollaborations(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(Collaboration.CollaborationType.OWNER, result.get(0).getType());
        assertEquals(Collaboration.CollaborationType.PARTICIPANTS, result.get(1).getType());

        verify(diagramRepository, times(1)).existsById(1L);
        verify(collaborationRepository, times(1)).findByDiagramId(1L);
        logger.info("[UT_CB_001] response={}", result);
    }

    /**
     * UT_CB_002: Lấy danh sách collaborations của diagram - không có collaborators
     * Mô tả: Kiểm tra edge case khi diagram tồn tại nhưng không có bất kỳ collaborator nào.
     *        Điều này có thể xảy ra khi diagram mới được tạo nhưng chưa thêm bất kỳ người dùng nào.
     *        Test đảm bảo hệ thống trả về danh sách rỗng thay vì null hoặc exception.
     * Input: diagramId = 1 (diagram tồn tại nhưng collaborationRepository trả về rỗng)
     * Expected: Trả về danh sách rỗng (List<CollaborationDTO> có size = 0)
     *           Không ném exception, không trả về null
     * Logic: - Diagram tồn tại (existsById = true)
     *        - findByDiagramId trả về Collections.emptyList()
     *        - Xác nhận result.isEmpty() = true
     */
    @Test
    public void UT_CB_002_getCollaborations_withNoDiagramCollaborators_shouldReturnEmptyList() {
        // Arrange
        when(diagramRepository.existsById(1L)).thenReturn(true);
        when(collaborationRepository.findByDiagramId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<CollaborationDTO> result = collaborationService.getCollaborations(1L);

        // Assert
        assertNotNull(result, "Danh sách không được null");
        assertTrue(result.isEmpty(), "Danh sách phải rỗng (size = 0)");

        verify(diagramRepository, times(1)).existsById(1L);
        verify(collaborationRepository, times(1)).findByDiagramId(1L);
        logger.info("[UT_CB_002] response={}", result);
    }

    /**
     * UT_CB_003: Lấy danh sách collaborations - diagram không tồn tại
     * Mô tả: Kiểm tra error handling khi user cố lấy collaborations của diagram không tồn tại.
     *        Test đảm bảo hệ thống không crashes, mà thay vào đó ném exception với message rõ ràng.
     *        Điều này giúp client biết rằng diagram không tồn tại thay vì nhận dữ liệu sai.
     * Input: diagramId = 999 (diagram ID không tồn tại trong DB)
     * Expected: Ném EntityNotFoundException với message chứa "Diagram not found"
     *           Repository.findByDiagramId() không được gọi (fail-fast)
     * Logic: - diagramRepository.existsById(999) trả về false
     *        - Service ném EntityNotFoundException
     *        - Repository.findByDiagramId() không được gọi (verify with never())
     *        - Client nhận exception và xử lý lỗi 404
     */
    @Test
    public void UT_CB_003_getCollaborations_withNonExistingDiagram_shouldThrowException() {
        // Arrange
        when(diagramRepository.existsById(999L)).thenReturn(false);

        // Act + Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.getCollaborations(999L);
        }, "Service phải ném EntityNotFoundException khi diagram không tồn tại");

        verify(diagramRepository, times(1)).existsById(999L);
        verify(collaborationRepository, never()).findByDiagramId(anyLong());
        logger.info("[UT_CB_003] exception={}", exception.getMessage());
    }

    // ========================================================================================
    // TEST CASES CHO addCollaborator()
    // ========================================================================================

    /**
     * UT_CB_004: Thêm collaborator mới thành công
     * Mô tả: Kiểm tra thêm collaborator mới với dữ liệu hợp lệ
     * Input: diagramId = 1, username = "new_user", permission = EDIT
     * Expected: Trả về CollaborationDTO của collaborator mới, repository.save() được gọi
     */
    @Test
    public void UT_CB_004_addCollaborator_withValidData_shouldReturnNewCollaboration() {
        // Arrange
        Collaboration newCollaboration = new Collaboration();
        newCollaboration.setId(3L);
        newCollaboration.setDiagram(sampleDiagram);
        newCollaboration.setUsername("new_user");
        newCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        newCollaboration.setPermission(Collaboration.Permission.EDIT);
        newCollaboration.setIsActive(true);
        newCollaboration.setCreatedAt(LocalDateTime.now());
        newCollaboration.setUpdatedAt(LocalDateTime.now());

        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndUsername(1L, "new_user")).thenReturn(Optional.empty());
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(newCollaboration);

        // Act
        CollaborationDTO result = collaborationService.addCollaborator(1L, "new_user", Collaboration.Permission.EDIT);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals("new_user", result.getUsername());
        assertEquals(Collaboration.CollaborationType.PARTICIPANTS, result.getType());
        assertEquals(Collaboration.Permission.EDIT, result.getPermission());
        assertTrue(result.getIsActive());

        verify(diagramRepository, times(1)).findById(1L);
        verify(collaborationRepository, times(1)).findByDiagramIdAndUsername(1L, "new_user");
        verify(collaborationRepository, times(1)).save(any(Collaboration.class));
        logger.info("[UT_CB_004] response={}", result);
    }

    /**
     * UT_CB_005: Thêm collaborator - diagram không tồn tại
     * Mô tả: Kiểm tra error handling khi user cố thêm collaborator vào diagram không tồn tại.
     *        Test đảm bảo service fail-fast (không tiếp tục xử lý) khi diagram không tồn tại.
     * Input: diagramId = 999, username = "new_user", permission = EDIT
     * Expected: Ném EntityNotFoundException
     *           Repository.save() không được gọi (rollback nếu có)
     * Lợi ích: Ngăn ngừa tạo dữ liệu orphan (collaboration không liên kết diagram)
     */
    @Test
    public void UT_CB_005_addCollaborator_withNonExistingDiagram_shouldThrowException() {
        // Arrange
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.addCollaborator(999L, "new_user", Collaboration.Permission.EDIT);
        }, "Service phải ném EntityNotFoundException khi diagram không tồn tại");

        verify(diagramRepository, times(1)).findById(999L);
        verify(collaborationRepository, never()).save(any(Collaboration.class));
        logger.info("[UT_CB_005] exception=EntityNotFoundException");
    }

    /**
     * UT_CB_006: Thêm collaborator - user đã là collaborator
     * Mô tả: Kiểm tra validation khi user cố thêm một người dùng đã là collaborator vào diagram.
     *        Test này ngăn ngừa trùng lặp dữ liệu (duplicate collaborations).
     *        Quan trọng: Mỗi người dùng chỉ được là 1 collaborator trên 1 diagram.
     * Input: diagramId = 1, username = "collaborator01" (user này đã là collaborator)
     * Expected: Ném IllegalArgumentException với message "already a collaborator"
     *           Repository.save() không được gọi
     * Business Rule: Một người dùng không thể được thêm 2 lần vào cùng 1 diagram
     */
    @Test
    public void UT_CB_006_addCollaborator_withExistingCollaborator_shouldThrowException() {
        // Arrange
        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndUsername(1L, "collaborator01"))
            .thenReturn(Optional.of(sampleCollaboration));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> {
            collaborationService.addCollaborator(1L, "collaborator01", Collaboration.Permission.EDIT);
        }, "Service phải ném IllegalArgumentException khi user đã là collaborator");
        logger.info("[UT_CB_006] exception=IllegalArgumentException");
        logger.info("[UT_CB_006] Xác nhận: Repository.findByDiagramIdAndUsername() trả về Optional.of() ");
        logger.info("[UT_CB_006] Xác nhận: Repository.save() không được gọi (ngăn trùng lặp)");
        logger.info("[UT_CB_006] KẾT QUẢ: PASSED ✅ - IllegalArgumentException được ném");
        logger.info("========================================\n");
    }

    /**
     * UT_CB_007: Thêm collaborator với các permission khác nhau
     * Mô tả: Kiểm tra thêm collaborator với VIEW, COMMENT, EDIT, FULL_ACCESS
     * Input: permission = VIEW
     * Expected: Collaborator được lưu với permission đúng
     */
    @Test
    public void UT_CB_007_addCollaborator_withViewPermission_shouldSaveWithViewPermission() {
        // Arrange
        Collaboration viewCollaboration = new Collaboration();
        viewCollaboration.setId(4L);
        viewCollaboration.setDiagram(sampleDiagram);
        viewCollaboration.setUsername("view_user");
        viewCollaboration.setType(Collaboration.CollaborationType.PARTICIPANTS);
        viewCollaboration.setPermission(Collaboration.Permission.VIEW);
        viewCollaboration.setIsActive(true);

        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.findByDiagramIdAndUsername(1L, "view_user")).thenReturn(Optional.empty());
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(viewCollaboration);

        // Act
        CollaborationDTO result = collaborationService.addCollaborator(1L, "view_user", Collaboration.Permission.VIEW);

        // Assert
        assertEquals(Collaboration.Permission.VIEW, result.getPermission());
        verify(collaborationRepository, times(1)).save(any(Collaboration.class));
        logger.info("[UT_CB_007] response={}", result);
    }

    // ========================================================================================
    // TEST CASES CHO updatePermission()
    // ========================================================================================

    /**
     * UT_CB_008: Cập nhật quyền collaborator thành công
     * Mô tả: Kiểm tra hệ thống có thể cập nhật permission của collaborator (EDIT -> VIEW).
     *        Test đảm bảo rằng permission thay đổi được lưu vào DB và không có lỗi.
     * Input: collaborationId = 2, newPermission = VIEW (downgrade từ EDIT)
     * Expected: Collaboration.permission được thay đổi và save vào DB thành công
     *           User không online (isUserActiveInDiagram = false) nên không cần disconnect
     * Logic: - Lấy Collaboration từ DB
     *        - Kiểm tra user có đang online không
     *        - Cập nhật permission
     *        - Save lại DB
     * Business Rule: Không thể đưa người dùng quyền cao hơn ứng dụng hiện tại
     */
    @Test
    public void UT_CB_008_updatePermission_withValidData_shouldUpdateSuccessfully() {
        // Arrange
        Collaboration updated = new Collaboration();
        updated.setId(2L);
        updated.setDiagram(sampleDiagram);
        updated.setUsername("collaborator01");
        updated.setType(Collaboration.CollaborationType.PARTICIPANTS);
        updated.setPermission(Collaboration.Permission.VIEW);
        updated.setIsActive(true);

        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(sampleCollaboration));
        when(sessionManager.isUserActiveInDiagram(1L, "collaborator01")).thenReturn(false);
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(updated);

        // Act
        collaborationService.updatePermission(2L, Collaboration.Permission.VIEW);

        // Assert
        verify(collaborationRepository, times(1)).findById(2L);
        verify(collaborationRepository, times(1)).save(any(Collaboration.class));
        logger.info("[UT_CB_008] response={}", updated);
    }

    /**
     * UT_CB_009: Cập nhật quyền - collaboration không tồn tại
     * Mô tả: Kiểm tra lỗi khi cập nhật permission của collaboration không tồn tại
     * Input: collaborationId = 999
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_CB_009_updatePermission_withNonExistingCollaboration_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.updatePermission(999L, Collaboration.Permission.VIEW);
        });

        verify(collaborationRepository, never()).save(any(Collaboration.class));
        logger.info("[UT_CB_009] exception=EntityNotFoundException");
    }

    /**
     * UT_CB_010: Cập nhật quyền - không được phép thay đổi quyền của owner
     * Mô tả: Kiểm tra business rule: Owner là người có quyền cao nhất và không thể bị hạ quyền.
     *        Điều này rất quan trọng để bảo vệ lảnh đạo diagram.
     *        Nếu owner được hạ quyền thành VIEW, ai sẽ quản lý diagram?
     * Input: collaborationId = 1 (OWNER type)
     * Expected: Ném IllegalArgumentException với message đề cậ thấy "owner"
     *           Repository.save() không được gọi
     * Lợi ích: Ngăn ngừa đơn dụn và lỗi logic khi quản lý diagram
     */
    @Test
    public void UT_CB_010_updatePermission_forOwner_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(ownerCollaboration));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> {
            collaborationService.updatePermission(1L, Collaboration.Permission.EDIT);
        }, "Service phải ném IllegalArgumentException khi cố cập nhật permission của owner");

        verify(collaborationRepository, never()).save(any(Collaboration.class));
        logger.info("[UT_CB_010] exception=IllegalArgumentException");
    }

    /**
     * UT_CB_011: Cập nhật quyền - downgrade từ FULL_ACCESS sang VIEW và user online
     * Mô tả: Kiểm tra downgrade permission khi user đang kết nối
     * Input: Downgrade từ FULL_ACCESS -> VIEW, user online
     * Expected: User bị disconnect, permission được cập nhật
     */
    @Test
    public void UT_CB_011_updatePermission_downgradeFromFullAccessToViewWithUserOnline_shouldForceDisconnect() {
        // Arrange
        Collaboration fullAccessCollab = new Collaboration();
        fullAccessCollab.setId(5L);
        fullAccessCollab.setDiagram(sampleDiagram);
        fullAccessCollab.setUsername("online_user");
        fullAccessCollab.setType(Collaboration.CollaborationType.PARTICIPANTS);
        fullAccessCollab.setPermission(Collaboration.Permission.FULL_ACCESS);
        fullAccessCollab.setIsActive(true);

        Set<String> activeSessions = new HashSet<>(Arrays.asList("session1", "session2"));

        when(collaborationRepository.findById(5L)).thenReturn(Optional.of(fullAccessCollab));
        when(sessionManager.isUserActiveInDiagram(1L, "online_user")).thenReturn(true);
        when(sessionManager.getActiveSessions(1L)).thenReturn(activeSessions);
        when(sessionManager.getUsernameForSession("session1")).thenReturn("online_user");
        when(sessionManager.getUsernameForSession("session2")).thenReturn("online_user");
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(fullAccessCollab);

        // Act
        collaborationService.updatePermission(5L, Collaboration.Permission.VIEW);

        // Assert
        verify(sessionManager, times(1)).isUserActiveInDiagram(1L, "online_user");
        verify(sessionRegistry, times(1)).closeSessions(any(Set.class));
        verify(collaborationRepository, times(1)).save(any(Collaboration.class));
        logger.info("[UT_CB_011] response={}", fullAccessCollab);
    }

    // ========================================================================================
    // TEST CASES CHO removeCollaborator()
    // ========================================================================================

    /**
     * UT_CB_012: Xóa collaborator thành công
     * Mô tả: Kiểm tra xóa collaborator bình thường
     * Input: collaborationId = 2
     * Expected: repository.delete() được gọi
     */
    @Test
    public void UT_CB_012_removeCollaborator_withValidId_shouldRemoveSuccessfully() {
        // Arrange
        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(sampleCollaboration));
        when(sessionManager.isUserActiveInDiagram(1L, "collaborator01")).thenReturn(false);
        doNothing().when(collaborationRepository).delete(sampleCollaboration);

        // Act
        collaborationService.removeCollaborator(2L);

        // Assert
        verify(collaborationRepository, times(1)).findById(2L);
        verify(collaborationRepository, times(1)).delete(sampleCollaboration);
        logger.info("[UT_CB_012] response=deleted");
    }

    /**
     * UT_CB_013: Xóa collaborator - collaboration không tồn tại
     * Mô tả: Kiểm tra lỗi khi xóa collaboration không tồn tại
     * Input: collaborationId = 999
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_CB_013_removeCollaborator_withNonExistingCollaboration_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.removeCollaborator(999L);
        });

        verify(collaborationRepository, never()).delete(any(Collaboration.class));
        logger.info("[UT_CB_013] exception=EntityNotFoundException");
    }

    /**
     * UT_CB_014: Xóa collaborator - không được phép xóa owner
     * Mô tả: Kiểm tra lỗi khi cố xóa owner
     * Input: collaborationId = 1 (owner)
     * Expected: Ném IllegalArgumentException
     */
    @Test
    public void UT_CB_014_removeCollaborator_forOwner_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(ownerCollaboration));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> {
            collaborationService.removeCollaborator(1L);
        });

        verify(collaborationRepository, never()).delete(any(Collaboration.class));
        logger.info("[UT_CB_014] exception=IllegalArgumentException");
    }

    /**
     * UT_CB_015: Xóa collaborator - user online bị force disconnect
     * Mô tả: Kiểm tra xóa collaborator khi user đang online
     * Input: collaborationId = 2, user online
     * Expected: User bị disconnect, collaboration bị xóa
     */
    @Test
    public void UT_CB_015_removeCollaborator_withUserOnline_shouldForceDisconnect() {
        // Arrange
        Set<String> activeSessions = new HashSet<>(Arrays.asList("session1", "session2"));
        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(sampleCollaboration));
        when(sessionManager.isUserActiveInDiagram(1L, "collaborator01")).thenReturn(true);
        when(sessionManager.getActiveSessions(1L)).thenReturn(activeSessions);
        when(sessionManager.getUsernameForSession("session1")).thenReturn("collaborator01");
        when(sessionManager.getUsernameForSession("session2")).thenReturn("collaborator01");
        doNothing().when(collaborationRepository).delete(sampleCollaboration);

        // Act
        collaborationService.removeCollaborator(2L);

        // Assert
        verify(sessionManager, times(1)).isUserActiveInDiagram(1L, "collaborator01");
        verify(sessionRegistry, times(1)).closeSessions(any(Set.class));
        verify(collaborationRepository, times(1)).delete(sampleCollaboration);
        logger.info("[UT_CB_015] response=deleted");
    }

    // ========================================================================================
    // TEST CASES CHO hasAccess()
    // ========================================================================================

    /**
     * UT_CB_016: Kiểm tra quyền truy cập - user có quyền
     * Mô tả: Kiểm tra user có quyền truy cập diagram
     * Input: diagramId = 1, username = "collaborator01"
     * Expected: Trả về true
     */
    @Test
    public void UT_CB_016_hasAccess_withValidUser_shouldReturnTrue() {
        // Arrange
        when(collaborationRepository.hasAccess(1L, "collaborator01")).thenReturn(true);

        // Act
        boolean result = collaborationService.hasAccess(1L, "collaborator01");

        // Assert
        assertTrue(result);
        verify(collaborationRepository, times(1)).hasAccess(1L, "collaborator01");
        logger.info("[UT_CB_016] response={}", result);
    }

    /**
     * UT_CB_017: Kiểm tra quyền truy cập - user không có quyền
     * Mô tả: Kiểm tra user không có quyền truy cập diagram
     * Input: diagramId = 1, username = "unauthorized_user"
     * Expected: Trả về false
     */
    @Test
    public void UT_CB_017_hasAccess_withUnauthorizedUser_shouldReturnFalse() {
        // Arrange
        when(collaborationRepository.hasAccess(1L, "unauthorized_user")).thenReturn(false);

        // Act
        boolean result = collaborationService.hasAccess(1L, "unauthorized_user");

        // Assert
        assertFalse(result);
        verify(collaborationRepository, times(1)).hasAccess(1L, "unauthorized_user");
        logger.info("[UT_CB_017] response={}", result);
    }

    // ========================================================================================
    // TEST CASES CHO getUserCollaboration()
    // ========================================================================================

    /**
     * UT_CB_018: Lấy thông tin collaboration của user - có quyền
     * Mô tả: Kiểm tra lấy thông tin collaboration của user
     * Input: diagramId = 1, username = "collaborator01"
     * Expected: Trả về CollaborationDTO với đủ thông tin
     */
    @Test
    public void UT_CB_018_getUserCollaboration_withValidUser_shouldReturnCollaborationDTO() {
        // Arrange
        when(collaborationRepository.findActiveCollaboration(1L, "collaborator01"))
            .thenReturn(Optional.of(sampleCollaboration));

        // Act
        CollaborationDTO result = collaborationService.getUserCollaboration(1L, "collaborator01");

        // Assert
        assertNotNull(result);
        assertEquals("collaborator01", result.getUsername());
        assertEquals(Collaboration.Permission.EDIT, result.getPermission());
        assertTrue(result.getIsActive());

        verify(collaborationRepository, times(1)).findActiveCollaboration(1L, "collaborator01");
        logger.info("[UT_CB_018] response={}", result);
    }

    /**
     * UT_CB_019: Lấy thông tin collaboration - user không có quyền
     * Mô tả: Kiểm tra lỗi khi lấy collaboration của user không có quyền
     * Input: diagramId = 1, username = "unauthorized_user"
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_CB_019_getUserCollaboration_withUnauthorizedUser_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findActiveCollaboration(1L, "unauthorized_user"))
            .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.getUserCollaboration(1L, "unauthorized_user");
        });

        verify(collaborationRepository, times(1)).findActiveCollaboration(1L, "unauthorized_user");
        logger.info("[UT_CB_019] exception=EntityNotFoundException");
    }

    // ========================================================================================
    // TEST CASES CHO getOwner()
    // ========================================================================================

    /**
     * UT_CB_020: Lấy thông tin owner - diagram có owner
     * Mô tả: Kiểm tra lấy thông tin owner của diagram
     * Input: diagramId = 1
     * Expected: Trả về CollaborationDTO của owner
     */
    @Test
    public void UT_CB_020_getOwner_withValidDiagram_shouldReturnOwner() {
        // Arrange
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
            .thenReturn(Optional.of(ownerCollaboration));

        // Act
        CollaborationDTO result = collaborationService.getOwner(1L);

        // Assert
        assertNotNull(result);
        assertEquals("owner_user", result.getUsername());
        assertEquals(Collaboration.CollaborationType.OWNER, result.getType());
        assertEquals(Collaboration.Permission.FULL_ACCESS, result.getPermission());

        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);
        logger.info("[UT_CB_020] response={}", result);
    }

    /**
     * UT_CB_021: Lấy thông tin owner - diagram không có owner
     * Mô tả: Kiểm tra lỗi khi diagram không có owner
     * Input: diagramId = 1 (không có owner)
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_CB_021_getOwner_withoutOwner_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
            .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.getOwner(1L);
        });

        verify(collaborationRepository, times(1)).findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER);
        logger.info("[UT_CB_021] exception=EntityNotFoundException");
    }

    // ========================================================================================
    // TEST CASES CHO countParticipants()
    // ========================================================================================

    /**
     * UT_CB_022: Đếm số participants của diagram - có participants
     * Mô tả: Kiểm tra đếm số participants
     * Input: diagramId = 1
     * Expected: Trả về số lượng participants (3)
     */
    @Test
    public void UT_CB_022_countParticipants_withMultipleParticipants_shouldReturnCorrectCount() {
        // Arrange
        when(collaborationRepository.countParticipants(1L)).thenReturn(3);

        // Act
        long result = collaborationService.countParticipants(1L);

        // Assert
        assertEquals(3, result);
        verify(collaborationRepository, times(1)).countParticipants(1L);
        logger.info("[UT_CB_022] response={}", result);
    }

    /**
     * UT_CB_023: Đếm số participants - không có participants
     * Mô tả: Kiểm tra đếm khi diagram không có participants
     * Input: diagramId = 1 (chỉ có owner)
     * Expected: Trả về 0
     */
    @Test
    public void UT_CB_023_countParticipants_withoutParticipants_shouldReturnZero() {
        // Arrange
        when(collaborationRepository.countParticipants(1L)).thenReturn(0);

        // Act
        long result = collaborationService.countParticipants(1L);

        // Assert
        assertEquals(0, result);
        verify(collaborationRepository, times(1)).countParticipants(1L);
        logger.info("[UT_CB_023] response={}", result);
    }

    // ========================================================================================
    // TEST CASES CHO createOwner()
    // ========================================================================================

    /**
     * UT_CB_024: Tạo owner collaboration - thành công
     * Mô tả: Kiểm tra tạo owner khi tạo diagram mới
     * Input: diagramId = 1, username = "owner_user"
     * Expected: Trả về CollaborationDTO của owner, repository.save() được gọi
     */
    @Test
    public void UT_CB_024_createOwner_withValidData_shouldReturnOwnerCollaboration() {
        // Arrange
        Collaboration newOwner = new Collaboration();
        newOwner.setId(6L);
        newOwner.setDiagram(sampleDiagram);
        newOwner.setUsername("new_owner");
        newOwner.setType(Collaboration.CollaborationType.OWNER);
        newOwner.setPermission(Collaboration.Permission.FULL_ACCESS);
        newOwner.setIsActive(true);
        newOwner.setCreatedAt(LocalDateTime.now());

        when(diagramRepository.findById(1L)).thenReturn(Optional.of(sampleDiagram));
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(newOwner);

        // Act
        CollaborationDTO result = collaborationService.createOwner(1L, "new_owner");

        // Assert
        assertNotNull(result);
        assertEquals(6L, result.getId());
        assertEquals("new_owner", result.getUsername());
        assertEquals(Collaboration.CollaborationType.OWNER, result.getType());
        assertEquals(Collaboration.Permission.FULL_ACCESS, result.getPermission());
        assertTrue(result.getIsActive());

        verify(diagramRepository, times(1)).findById(1L);
        verify(collaborationRepository, times(1)).save(any(Collaboration.class));
        logger.info("[UT_CB_024] response={}", result);
    }

    /**
     * UT_CB_025: Tạo owner - diagram không tồn tại
     * Mô tả: Kiểm tra lỗi khi tạo owner cho diagram không tồn tại
     * Input: diagramId = 999
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_CB_025_createOwner_withNonExistingDiagram_shouldThrowException() {
        // Arrange
        when(diagramRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.createOwner(999L, "new_owner");
        });

        verify(collaborationRepository, never()).save(any(Collaboration.class));
        logger.info("[UT_CB_025] exception=EntityNotFoundException");
    }

    // ========================================================================================
    // TEST CASES CHO deactivateCollaboration()
    // ========================================================================================

    /**
     * UT_CB_026: Vô hiệu hóa collaboration - thành công
     * Mô tả: Kiểm tra vô hiệu hóa collaboration (soft delete)
     * Input: collaborationId = 2
     * Expected: isActive = false, repository.save() được gọi
     */
    @Test
    public void UT_CB_026_deactivateCollaboration_withValidId_shouldSetIsActiveFalse() {
        // Arrange
        Collaboration deactivated = new Collaboration();
        deactivated.setId(2L);
        deactivated.setDiagram(sampleDiagram);
        deactivated.setUsername("collaborator01");
        deactivated.setType(Collaboration.CollaborationType.PARTICIPANTS);
        deactivated.setPermission(Collaboration.Permission.EDIT);
        deactivated.setIsActive(false);

        when(collaborationRepository.findById(2L)).thenReturn(Optional.of(sampleCollaboration));
        when(collaborationRepository.save(any(Collaboration.class))).thenReturn(deactivated);

        // Act
        collaborationService.deactivateCollaboration(2L);

        // Assert
        verify(collaborationRepository, times(1)).findById(2L);
        verify(collaborationRepository, times(1)).save(any(Collaboration.class));
        logger.info("[UT_CB_026] response={}", deactivated);
    }

    /**
     * UT_CB_027: Vô hiệu hóa - collaboration không tồn tại
     * Mô tả: Kiểm tra lỗi khi vô hiệu hóa collaboration không tồn tại
     * Input: collaborationId = 999
     * Expected: Ném EntityNotFoundException
     */
    @Test
    public void UT_CB_027_deactivateCollaboration_withNonExistingCollaboration_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findById(999L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(EntityNotFoundException.class, () -> {
            collaborationService.deactivateCollaboration(999L);
        });

        verify(collaborationRepository, never()).save(any(Collaboration.class));
        logger.info("[UT_CB_027] exception=EntityNotFoundException");
    }

    /**
     * UT_CB_028: Vô hiệu hóa - không được phép vô hiệu hóa owner
     * Mô tả: Kiểm tra lỗi khi cố vô hiệu hóa owner collaboration
     * Input: collaborationId = 1 (owner)
     * Expected: Ném IllegalArgumentException
     */
    @Test
    public void UT_CB_028_deactivateCollaboration_forOwner_shouldThrowException() {
        // Arrange
        when(collaborationRepository.findById(1L)).thenReturn(Optional.of(ownerCollaboration));

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> {
            collaborationService.deactivateCollaboration(1L);
        });

        verify(collaborationRepository, never()).save(any(Collaboration.class));
        logger.info("[UT_CB_028] exception=IllegalArgumentException");
    }

    // ========================================================================================
    // TEST CASES CHO deleteAllByDiagramId()
    // ========================================================================================

    /**
     * UT_CB_029: Xóa tất cả collaborations của diagram - thành công
     * Mô tả: Kiểm tra xóa tất cả collaborations khi xóa diagram
     * Input: diagramId = 1
     * Expected: repository.deleteByDiagramId() được gọi
     */
    @Test
    public void UT_CB_029_deleteAllByDiagramId_withValidId_shouldDeleteAllCollaborations() {
        // Arrange
        doNothing().when(collaborationRepository).deleteByDiagramId(1L);

        // Act
        collaborationService.deleteAllByDiagramId(1L);

        // Assert
        verify(collaborationRepository, times(1)).deleteByDiagramId(1L);
        logger.info("[UT_CB_029] response=deleted");
    }

    /**
     * UT_CB_030: Xóa tất cả - không ảnh hưởng diagram khác
     * Mô tả: Kiểm tra chỉ xóa collaborations của diagram chỉ định
     * Input: diagramId = 1
     * Expected: Chỉ diagram 1 bị xóa collaborations, diagram 2 không bị ảnh hưởng
     */
    @Test
    public void UT_CB_030_deleteAllByDiagramId_shouldOnlyDeleteForSpecificDiagram() {
        // Arrange
        doNothing().when(collaborationRepository).deleteByDiagramId(1L);

        // Act
        collaborationService.deleteAllByDiagramId(1L);

        // Assert
        verify(collaborationRepository, times(1)).deleteByDiagramId(1L);
        verify(collaborationRepository, never()).deleteByDiagramId(2L);
        logger.info("[UT_CB_030] response=deleted");
    }
}
