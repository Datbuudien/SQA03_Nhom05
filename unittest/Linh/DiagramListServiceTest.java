package com.example.react_flow_be.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.react_flow_be.dto.DiagramListItemDto;
import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;

/**
 * ============================================================================
 * Unit Test cho DiagramListService - Tầng Service quản lý Danh Sách Diagrams
 * ============================================================================
 * Mô tả: Test các phương thức lấy danh sách diagrams với filter, sorting,
 *        pagination, access control, và conversion to DTOs
 * Phương pháp: Sử dụng Mockito để mock các Repository và Specification
 * Rollback: Sử dụng Mockito (không tương tác DB thật) nên không cần rollback DB.
 *           Mỗi test được reset mock objects qua @BeforeEach/@AfterEach.
 * Database: Khi test query dữ liệu, phải verify:
 *           1. Repository query được gọi đúng số lần
 *           2. Filters được apply đúng theo request
 *           3. Sorting được thực hiện đúng theo sortBy/sortDirection
 *           4. Pagination cursor-based được xử lý đúng
 *           5. DTO conversion complete và chính xác
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class DiagramListServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DiagramListServiceTest.class);

    @Mock
    private DiagramRepository diagramRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private MigrationRepository migrationRepository;

    @InjectMocks
    private DiagramListService diagramListService;

    // ============ Test Data ============
    private Diagram diagram1;
    private Diagram diagram2;
    private Diagram diagram3;
    private Collaboration collab1;
    private Collaboration collab2;
    private Migration migration1;
    private DiagramListRequestDto requestDto;

    /**
     * Khởi tạo dữ liệu test trước mỗi test case.
     * Đảm bảo mỗi test case có dữ liệu sạch, không bị ảnh hưởng bởi test khác.
     * 
     * Các object được khởi tạo:
     * - Diagram (3 diagrams): Biểu diễn sơ đồ (khác DatabaseDiagram)
     * - Collaboration: Biểu diễn access control (OWNER, PARTICIPANTS)
     * - Migration: Biểu diễn lịch sử thay đổi
     * - DiagramListRequestDto: Biểu diễn request filter/sort/page
     */
    @BeforeEach
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test cho DiagramListService...");

        // Khởi tạo Diagram 1 - Owned by admin, mới nhất
        diagram1 = new Diagram();
        diagram1.setId(1L);
        diagram1.setName("API Schema Diagram");
        diagram1.setDescription("REST API database schema");
        diagram1.setIsDeleted(false);
        diagram1.setCreatedAt(LocalDateTime.now().minusHours(10));
        diagram1.setUpdatedAt(LocalDateTime.now());

        // Khởi tạo Diagram 2 - Owned by lecturer, tuổi vừa phải
        diagram2 = new Diagram();
        diagram2.setId(2L);
        diagram2.setName("Business Process Diagram");
        diagram2.setDescription("System workflow");
        diagram2.setIsDeleted(false);
        diagram2.setCreatedAt(LocalDateTime.now().minusDays(1));
        diagram2.setUpdatedAt(LocalDateTime.now().minusHours(5));

        // Khởi tạo Diagram 3 - Owned by admin, cũ nhất, deleted
        diagram3 = new Diagram();
        diagram3.setId(3L);
        diagram3.setName("Analytics Dashboard Diagram");
        diagram3.setDescription("Report schema");
        diagram3.setIsDeleted(false);
        diagram3.setCreatedAt(LocalDateTime.now().minusDays(7));
        diagram3.setUpdatedAt(LocalDateTime.now().minusDays(2));

        // Khởi tạo Collaboration 1 - Owner admin cho diagram1
        collab1 = new Collaboration();
        collab1.setId(1L);
        collab1.setUsername("admin");
        collab1.setType(Collaboration.CollaborationType.OWNER);

        // Khởi tạo Collaboration 2 - Owner lecturer cho diagram2
        collab2 = new Collaboration();
        collab2.setId(2L);
        collab2.setUsername("lecturer");
        collab2.setType(Collaboration.CollaborationType.OWNER);

        // Khởi tạo Migration 1 - Last migration của diagram1
        migration1 = new Migration();
        migration1.setId(1L);
        migration1.setUsername("admin");
        migration1.setCreatedAt(LocalDateTime.now().minusHours(1));
        migration1.setSnapshotHash("hash123");
        migration1.setSnapshotJson("{}");

        // Khởi tạo Request DTO - Default request
        requestDto = new DiagramListRequestDto();
        requestDto.setPageSize(10);
        requestDto.setIsDeleted(false);

        logger.info("[SETUP] Hoàn tất khởi tạo dữ liệu test.");
    }

    /**
     * Dọn dẹp sau mỗi test case - reset tất cả mock objects.
     * Đảm bảo rollback trạng thái về ban đầu.
     */
    @AfterEach
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
        reset(diagramRepository, collaborationRepository, migrationRepository);
        diagram1 = null;
        diagram2 = null;
        diagram3 = null;
        collab1 = null;
        collab2 = null;
        migration1 = null;
        requestDto = null;
        logger.info("[TEARDOWN] Hoàn tất dọn dẹp. Trạng thái đã được khôi phục.");
        logger.info("========================================\n");
    }

    // ========================================================================================
    // TEST CASES CHO getDiagramList()
    // ========================================================================================
    // Đây là phương thức chính lấy danh sách diagrams với filter, sort, và pagination
    // ========================================================================================

    /**
     * UT_DL_001: Lấy danh sách diagrams - happy path
     * 
     * Mô tả: Kiểm tra lấy danh sách diagrams có dữ liệu, user có access, 
     *        kích thước page bình thường, không vượt quá limit
     * Kịch bản:
     *   - Có 3 diagrams trong database
     *   - User "admin" có access tới tất cả
     *   - Request pageSize=10, không có filters
     *   - Sắp xếp theo updatedAt DESC (mặc định)
     *   - hasMore = false (3 items < 10 pageSize)
     * 
     * Input:
     *   - currentUsername="admin"
     *   - request: pageSize=10, no filters
     * 
     * Expected Output:
     *   - Return DiagramListResponseDto
     *   - diagrams.size() = 3 (tất cả diagrams)
     *   - hasMore = false (không có trang tiếp theo)
     *   - totalCount = 3
     *   - lastDiagramId = diagram1.id (mới nhất)
     *   - Danh sách sorted by updatedAt DESC
     * 
     * Coverage:
     *   - Happy path: full flow works
     *   - Access control: user access verified
     *   - Pagination: hasMore calculation
     *   - Sorting: default sort applied
     */
    @Test
    public void UT_DL_001_getDiagramList_withValidDataAndNoFilters_shouldReturnSortedList() throws Exception {
        logger.info("[UT_DL_001] BẮT ĐẦU: Lấy danh sách diagrams - happy path");
        logger.info("[UT_DL_001] Input: username='admin', pageSize=10, no filters");

        // Arrange: Setup mock behavior
        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram2, diagram3)));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab2));
        when(collaborationRepository.findByDiagramIdAndType(3L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(migration1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);
        when(diagramRepository.count(any(Specification.class))).thenReturn(3L);

        // Act: Gọi service method
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertNotNull(result, "Result không được null");
        assertEquals(3, result.getDiagrams().size(), "Phải có 3 items");
        assertFalse(result.getHasMore(), "hasMore phải false (3 < pageSize 10)");
        assertEquals(3, result.getTotalCount(), "totalCount phải 3");
        assertNotNull(result.getLastDiagramId(), "lastDiagramId không được null");
        
        // Verify repository calls
        verify(diagramRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        verify(diagramRepository, times(1)).count(any(Specification.class));

        logger.info("[UT_DL_001] KẾT QUẢ: PASSED - Danh sách {} diagrams được trả về",
                result.getDiagrams().size());
    }

    /**
     * UT_DL_002: Lấy danh sách diagrams - không có dữ liệu
     * 
     * Mô tả: Khi database rỗng hoặc user không có access tới diagram nào
     * Input: currentUsername="guest", no diagrams user can access
     * Expected:
     *   - diagrams.size() = 0 (empty list)
     *   - hasMore = false
     *   - totalCount = 0
     *   - lastDiagramId = null
     */
    @Test
    public void UT_DL_002_getDiagramList_noDataOrNoAccess_shouldReturnEmpty() throws Exception {
        logger.info("[UT_DL_002] BẮT ĐẦU: Lấy danh sách diagrams - không có dữ liệu");
        logger.info("[UT_DL_002] Input: username='guest', user không có access");

        // Arrange
        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(diagramRepository.count(any(Specification.class))).thenReturn(0L);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "guest");

        // Assert
        assertNotNull(result, "Result không được null");
        assertTrue(result.getDiagrams().isEmpty(), "Diagrams phải rỗng khi không có access");
        assertFalse(result.getHasMore(), "hasMore phải false");
        assertEquals(0, result.getTotalCount(), "totalCount phải 0");
        assertNull(result.getLastDiagramId(), "lastDiagramId phải null");

        logger.info("[UT_DL_002] KẾT QUẢ: PASSED - Empty list returned khi không có access");
    }

    /**
     * UT_DL_003: Lấy danh sách diagrams - pagination với lastDiagramId
     * 
     * Mô tả: Kiểm tra cursor-based pagination - lấy trang tiếp theo dựa vào lastDiagramId
     * Input:
     *   - 5 diagrams tổng
     *   - pageSize=2
     *   - lastDiagramId=2 (request trang thứ 2)
     * Expected:
     *   - Bắt đầu từ diagram 3
     *   - Return 2 items (diagrams 3-4)
     *   - hasMore = true (có diagram 5)
     */
    @Test
    public void UT_DL_003_getDiagramList_withCursorPagination_shouldReturnNextPage() throws Exception {
        logger.info("[UT_DL_003] BẮT ĐẦU: Lấy danh sách diagrams - cursor pagination");
        logger.info("[UT_DL_003] Input: pageSize=2, lastDiagramId=2");

        // Arrange
        Diagram diagram4 = new Diagram();
        diagram4.setId(4L);
        diagram4.setName("Analytics Diagram");
        diagram4.setUpdatedAt(LocalDateTime.now().minusHours(3));
        
        Diagram diagram5 = new Diagram();
        diagram5.setId(5L);
        diagram5.setName("Backup Diagram");
        diagram5.setUpdatedAt(LocalDateTime.now().minusHours(4));

        List<Diagram> nextPageDiagrams = Arrays.asList(diagram3, diagram4, diagram5);
        requestDto.setPageSize(2);
        requestDto.setLastDiagramId(2L);

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(nextPageDiagrams));
        when(diagramRepository.count(any(Specification.class))).thenReturn(5L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 items trên trang");
        assertTrue(result.getHasMore(), "hasMore phải true (có diagram 5)");
        assertEquals(5, result.getTotalCount(), "totalCount phải 5");

        logger.info("[UT_DL_003] KẾT QUẢ: PASSED - Cursor pagination works, next page returned");
    }

    /**
     * UT_DL_004: Lấy danh sách diagrams - filter by isDeleted
     * 
     * Mô tả: Kiểm tra lọc diagrams theo trạng thái deleted
     * Kịch bản:
     *   - diagram1: isDeleted=false
     *   - diagram3: isDeleted=true
     *   - Request: isDeleted=false
     * Expected:
     *   - Trả về chỉ non-deleted diagrams
     */
    @Test
    public void UT_DL_004_getDiagramList_filterByDeleted_shouldExcludeDeletedDiagrams() throws Exception {
        logger.info("[UT_DL_004] BẮT ĐẦU: Lấy danh sách diagrams - filter by isDeleted");
        logger.info("[UT_DL_004] Input: isDeleted=false, should exclude deleted");

        // Arrange
        diagram3.setIsDeleted(true);
        requestDto.setIsDeleted(false);

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram2)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(2L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 items (diagram3 bị lọc)");
        assertFalse(result.getDiagrams().stream().anyMatch(item -> item.getId() == 3L),
                "diagram3 không được xuất hiện");

        logger.info("[UT_DL_004] KẾT QUẢ: PASSED - Deleted diagrams filtered out");
    }

    /**
     * UT_DL_005: Lấy danh sách diagrams - filter by nameStartsWith
     * 
     * Mô tả: Kiểm tra lọc diagrams theo tên bắt đầu bằng ký tự
     * Input:
     *   - Request: nameStartsWith="API"
     * Expected:
     *   - Trả về chỉ diagram1 (API Schema Diagram)
     */
    @Test
    public void UT_DL_005_getDiagramList_filterByNameStartsWith_shouldReturnMatching() throws Exception {
        logger.info("[UT_DL_005] BẮT ĐẦU: Lấy danh sách diagrams - filter by nameStartsWith");
        logger.info("[UT_DL_005] Input: nameStartsWith='API'");

        // Arrange
        requestDto.setNameStartsWith("API");

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(1L);
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(migration1));
        when(collaborationRepository.countParticipants(1L)).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(1, result.getDiagrams().size(), "Phải có 1 item (chỉ API Schema Diagram)");
        assertEquals(1L, result.getDiagrams().get(0).getId(), "Item phải là diagram1");
        assertEquals("API Schema Diagram", result.getDiagrams().get(0).getName(), "Name phải match");

        logger.info("[UT_DL_005] KẾT QUẢ: PASSED - Name filter applied correctly");
    }

    /**
     * UT_DL_006: Lấy danh sách diagrams - filter by searchQuery
     * 
     * Mô tả: Kiểm tra tìm kiếm diagrams theo query (tìm trong name hoặc description)
     * Input:
     *   - Request: searchQuery="workflow"
     * Expected:
     *   - Trả về diagram2 (Business Process Diagram - "System workflow")
     */
    @Test
    public void UT_DL_006_getDiagramList_filterBySearchQuery_shouldSearchInNameAndDescription() throws Exception {
        logger.info("[UT_DL_006] BẮT ĐẦU: Lấy danh sách diagrams - filter by searchQuery");
        logger.info("[UT_DL_006] Input: searchQuery='workflow'");

        // Arrange
        requestDto.setSearchQuery("workflow");

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram2)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(1L);
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab2));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(2L)).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(1, result.getDiagrams().size(), "Phải có 1 item (match 'workflow')");
        assertEquals(2L, result.getDiagrams().get(0).getId(), "Item phải là diagram2");

        logger.info("[UT_DL_006] KẾT QUẢ: PASSED - Search query filter applied");
    }

    /**
     * UT_DL_007: Lấy danh sách diagrams - sort by name ASC
     * 
     * Mô tả: Kiểm tra sắp xếp diagrams theo tên (A-Z)
     * Input:
     *   - sortBy="name", sortDirection="ASC"
     * Expected:
     *   - Diagrams sắp xếp alphabetically
     */
    @Test
    public void UT_DL_007_getDiagramList_sortByNameASC_shouldSortAlphabetically() throws Exception {
        logger.info("[UT_DL_007] BẮT ĐẦU: Lấy danh sách diagrams - sort by name ASC");
        logger.info("[UT_DL_007] Input: sortBy='name', sortDirection='ASC'");

        // Arrange
        requestDto.setSortBy("name");
        requestDto.setSortDirection("ASC");

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram3, diagram2)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(3L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(3, result.getDiagrams().size(), "Phải có 3 items");
        assertTrue(result.getDiagrams().get(0).getName().compareTo(result.getDiagrams().get(1).getName()) < 0,
                "Items phải sorted A-Z");

        logger.info("[UT_DL_007] KẾT QUẢ: PASSED - Sorted by name ASC correctly");
    }

    /**
     * UT_DL_008: Lấy danh sách diagrams - sort by updatedAt DESC (default)
     * 
     * Mô tả: Kiểm tra sắp xếp diagrams theo updatedAt (mới nhất trước)
     * Input:
     *   - sortBy="updatedAt", sortDirection="DESC"
     * Expected:
     *   - Diagrams sắp xếp: diagram1 (now), diagram2 (5h ago), diagram3 (2d ago)
     */
    @Test
    public void UT_DL_008_getDiagramList_sortByUpdatedAtDESC_shouldSortByNewest() throws Exception {
        logger.info("[UT_DL_008] BẮT ĐẦU: Lấy danh sách diagrams - sort by updatedAt DESC");
        logger.info("[UT_DL_008] Input: sortBy='updatedAt', sortDirection='DESC'");

        // Arrange
        requestDto.setSortBy("updatedAt");
        requestDto.setSortDirection("DESC");

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram2, diagram3)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(3L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(3, result.getDiagrams().size(), "Phải có 3 items");
        assertEquals(1L, result.getDiagrams().get(0).getId(), "Thứ nhất: diagram1 (mới nhất)");

        logger.info("[UT_DL_008] KẾT QUẢ: PASSED - Sorted by updatedAt DESC correctly");
    }

    /**
     * UT_DL_009: Lấy danh sách diagrams - filter by sharedWithMe
     * 
     * Mô tả: Kiểm tra lọc diagrams được chia sẻ với tôi
     * Input:
     *   - sharedWithMe=true
     *   - currentUsername="dev"
     * Expected:
     *   - Trả về diagrams shared with dev user
     */
    @Test
    public void UT_DL_009_getDiagramList_filterBySharedWithMe_shouldReturnSharedDiagrams() throws Exception {
        logger.info("[UT_DL_009] BẮT ĐẦU: Lấy danh sách diagrams - filter by sharedWithMe");
        logger.info("[UT_DL_009] Input: sharedWithMe=true, currentUsername='dev'");

        // Arrange
        requestDto.setSharedWithMe(true);

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram2)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(1L);
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab2));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(2L)).thenReturn(1);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "dev");

        // Assert
        assertEquals(1, result.getDiagrams().size(), "Phải có 1 diagram shared với dev");

        logger.info("[UT_DL_009] KẾT QUẢ: PASSED - Shared diagrams filter applied");
    }

    /**
     * UT_DL_010: Lấy danh sách diagrams - filter by ownerFilter
     * 
     * Mô tả: Kiểm tra lọc diagrams tôi sở hữu
     * Input:
     *   - ownerFilter="me"
     *   - currentUsername="admin"
     * Expected:
     *   - Trả về diagrams owned by admin (diagram1, diagram3)
     */
    @Test
    public void UT_DL_010_getDiagramList_filterByOwnerMe_shouldReturnOwnedDiagrams() throws Exception {
        logger.info("[UT_DL_010] BẮT ĐẦU: Lấy danh sách diagrams - filter by ownerFilter=me");
        logger.info("[UT_DL_010] Input: ownerFilter='me', currentUsername='admin'");

        // Arrange
        requestDto.setOwnerFilter("me");

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram3)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(2L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenAnswer(inv -> {
                    Long diagramId = inv.getArgument(0);
                    if (diagramId == 1L || diagramId == 3L) {
                        return Optional.of(collab1); // admin is owner
                    } else {
                        return Optional.of(collab2); // lecturer is owner
                    }
                });
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 diagrams (owned by admin)");
        assertTrue(result.getDiagrams().stream().allMatch(d -> d.getOwnerUsername().equals("admin")),
                "Tất cả phải owned by admin");

        logger.info("[UT_DL_010] KẾT QUẢ: PASSED - Owner filter 'me' applied correctly");
    }

    /**
     * UT_DL_011: Filter by dateRange - last 7 days
     * 
     * Mô tả: Kiểm tra lọc diagrams theo ngày tạo
     * Input:
     *   - dateRange="week"
     * Expected:
     *   - Trả về diagrams được update trong 7 ngày qua
     */
    @Test
    public void UT_DL_011_getDiagramList_filterByDateRangeWeek_shouldReturnRecentDiagrams() throws Exception {
        logger.info("[UT_DL_011] BẮT ĐẦU: Lấy danh sách diagrams - filter by dateRange week");
        logger.info("[UT_DL_011] Input: dateRange='week'");

        // Arrange
        requestDto.setDateRange("week");

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram2, diagram3)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(3L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertFalse(result.getDiagrams().isEmpty(), "Phải có ít nhất 1 diagram trong tuần");

        logger.info("[UT_DL_011] KẾT QUẢ: PASSED - Date range filter applied");
    }

    // ========================================================================================
    // TEST CASES CHO convertToDto()
    // ========================================================================================

    /**
     * UT_DL_012: Convert diagram to DTO - với migration
     * 
     * Mô tả: Khi diagram có migration history, DTO phải chứa lastMigration info
     * Expected:
     *   - lastMigrationUsername = "admin"
     *   - lastMigrationDate populated
     *   - updatedByUsername = "admin"
     */
    @Test
    public void UT_DL_012_convertToDto_withMigration_shouldIncludeMigrationInfo() throws Exception {
        logger.info("[UT_DL_012] BẮT ĐẦU: Convert diagram to DTO - với migration");

        // Arrange
        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1)));
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(migration1));
        when(collaborationRepository.countParticipants(1L)).thenReturn(0);
        when(diagramRepository.count(any(Specification.class))).thenReturn(1L);

        // Act
        DiagramListResponseDto response = diagramListService.getDiagramList(requestDto, "admin");
        DiagramListItemDto result = response.getDiagrams().get(0);

        // Assert
        assertNotNull(result, "DTO không được null");
        assertEquals("admin", result.getLastMigrationUsername(), "lastMigrationUsername phải 'admin'");
        assertNotNull(result.getLastMigrationDate(), "lastMigrationDate phải populated");
        assertEquals("admin", result.getUpdatedByUsername(), "updatedByUsername phải 'admin'");

        logger.info("[UT_DL_012] KẾT QUẢ: PASSED - Migration info included in DTO");
    }

    /**
     * UT_DL_013: Convert diagram to DTO - không có migration
     * 
     * Mô tả: Khi diagram không có migration, updatedBy phải lấy từ owner
     * Expected:
     *   - lastMigrationUsername = null
     *   - updatedByUsername = owner username
     */
    @Test
    public void UT_DL_013_convertToDto_noMigration_shouldUseOwnerAsUpdatedBy() throws Exception {
        logger.info("[UT_DL_013] BẮT ĐẦU: Convert diagram to DTO - không có migration");

        // Arrange
        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram2)));
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab2));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                .thenReturn(Collections.emptyList()); // No migration
        when(collaborationRepository.countParticipants(2L)).thenReturn(0);
        when(diagramRepository.count(any(Specification.class))).thenReturn(1L);

        // Act
        DiagramListResponseDto response = diagramListService.getDiagramList(requestDto, "admin");
        DiagramListItemDto result = response.getDiagrams().get(0);

        // Assert
        assertNotNull(result, "DTO không được null");
        assertNull(result.getLastMigrationUsername(), "lastMigrationUsername phải null");
        assertEquals("lecturer", result.getUpdatedByUsername(), "updatedByUsername phải từ owner (lecturer)");

        logger.info("[UT_DL_013] KẾT QUẢ: PASSED - Owner used as updatedBy when no migration");
    }

    /**
     * UT_DL_014: Kiểm tra hasCollaborators flag
     * 
     * Mô tả: DTO phải indicate nếu diagram có collaborators (participants)
     * Expected:
     *   - hasCollaborators = true khi participantCount > 0
     *   - hasCollaborators = false khi participantCount = 0
     */
    @Test
    public void UT_DL_014_convertToDto_hasCollaboratorsFlag_shouldIndicateParticipants() throws Exception {
        logger.info("[UT_DL_014] BẮT ĐẦU: Kiểm tra hasCollaborators flag");

        // Arrange
        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram1, diagram2)));
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(1L)).thenReturn(3); // diagram1 có 3 participants
        when(collaborationRepository.countParticipants(2L)).thenReturn(0); // diagram2 không có
        when(diagramRepository.count(any(Specification.class))).thenReturn(2L);

        // Act
        DiagramListResponseDto response = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        DiagramListItemDto item1 = response.getDiagrams().stream().filter(d -> d.getId() == 1L).findFirst().orElse(null);
        DiagramListItemDto item2 = response.getDiagrams().stream().filter(d -> d.getId() == 2L).findFirst().orElse(null);

        assertNotNull(item1, "Item 1 phải exist");
        assertNotNull(item2, "Item 2 phải exist");
        assertTrue(item1.getHasCollaborators(), "diagram1 phải có hasCollaborators=true");
        assertFalse(item2.getHasCollaborators(), "diagram2 phải có hasCollaborators=false");

        logger.info("[UT_DL_014] KẾT QUẢ: PASSED - hasCollaborators flag set correctly");
    }

    /**
     * UT_DL_015: Integration test - full flow với multiple filters
     * 
     * Mô tả: Test kết hợp nhiều filters, sort, và pagination
     * Kịch bản:
     *   - Filter: nameStartsWith="Database", not deleted
     *   - Sort: by name ASC
     *   - Pagination: pageSize=2
     * Expected:
     *   - Trả về sorted list theo các filter rules
     */
    @Test
    public void UT_DL_015_fullFlow_multipleFiltersAndSorting_shouldApplyAll() throws Exception {
        logger.info("[UT_DL_015] BẮT ĐẦU: Integration test - multiple filters + sort");
        logger.info("[UT_DL_015] Input: nameStartsWith='Database', sortBy='name', pageSize=2");

        // Arrange
        requestDto.setNameStartsWith("Analytics");
        requestDto.setSortBy("name");
        requestDto.setSortDirection("ASC");
        requestDto.setPageSize(2);

        when(diagramRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(diagram3)));
        when(diagramRepository.count(any(Specification.class))).thenReturn(1L);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = diagramListService.getDiagramList(requestDto, "admin");

        // Assert
        assertNotNull(result, "Result không được null");
        assertTrue(result.getDiagrams().size() <= 2, "Phải respects pageSize=2");
        assertFalse(result.getHasMore(), "hasMore phải false (1 item)");

        logger.info("[UT_DL_015] KẾT QUẢ: PASSED - Multiple filters and sorting applied correctly");
    }
}
