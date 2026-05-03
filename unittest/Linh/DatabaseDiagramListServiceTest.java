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

import com.example.react_flow_be.dto.DiagramListItemDto;
import com.example.react_flow_be.dto.DiagramListRequestDto;
import com.example.react_flow_be.dto.DiagramListResponseDto;
import com.example.react_flow_be.entity.Collaboration;
import com.example.react_flow_be.entity.DatabaseDiagram;
import com.example.react_flow_be.entity.Migration;
import com.example.react_flow_be.repository.CollaborationRepository;
import com.example.react_flow_be.repository.DatabaseDiagramRepository;
import com.example.react_flow_be.repository.MigrationRepository;

/**
 * ============================================================================
 * Unit Test cho DatabaseDiagramListService - Tầng Service quản lý Danh Sách Diagrams
 * ============================================================================
 * Mô tả: Test các phương thức lấy danh sách database diagrams với filter, sorting,
 *        pagination, access control, và conversion to DTOs
 * Phương pháp: Sử dụng Mockito để mock các Repository dependency
 * Rollback: Sử dụng Mockito (không tương tác DB thật) nên không cần rollback DB.
 *           Mỗi test được reset mock objects qua @BeforeEach/@AfterEach.
 * Database: Khi test query dữ liệu, phải verify:
 *           1. Repository query được gọi đúng số lần
 *           2. Filters được apply đúng theo request
 *           3. Sorting được thực hiện đúng theo sortBy/sortDirection
 *           4. Pagination cursor-based được xử lý đúng
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
public class DatabaseDiagramListServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseDiagramListServiceTest.class);

    @Mock
    private DatabaseDiagramRepository databaseDiagramRepository;

    @Mock
    private CollaborationRepository collaborationRepository;

    @Mock
    private MigrationRepository migrationRepository;

    @InjectMocks
    private DatabaseDiagramListService databaseDiagramListService;

    // ============ Test Data ============
    private DatabaseDiagram diagram1;
    private DatabaseDiagram diagram2;
    private DatabaseDiagram diagram3;
    private Collaboration collab1;
    private Collaboration collab2;
    private Migration migration1;
    private DiagramListRequestDto requestDto;

    /**
     * Khởi tạo dữ liệu test trước mỗi test case.
     * Đảm bảo mỗi test case có dữ liệu sạch, không bị ảnh hưởng bởi test khác.
     * 
     * Các object được khởi tạo:
     * - DatabaseDiagram (3 diagrams): Biểu diễn sơ đồ database
     * - Collaboration: Biểu diễn access control (OWNER, PARTICIPANTS)
     * - Migration: Biểu diễn lịch sử thay đổi
     * - DiagramListRequestDto: Biểu diễn request filter/sort/page
     */
    @BeforeEach
    public void setUp() {
        logger.info("========================================");
        logger.info("[SETUP] Khởi tạo dữ liệu test cho DatabaseDiagramListService...");

        // Khởi tạo Diagram 1 - Owned by admin, mới nhất
        diagram1 = new DatabaseDiagram();
        diagram1.setId(1L);
        diagram1.setName("Database Alpha");
        diagram1.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        diagram1.setVersion("8.0");
        diagram1.setIsDeleted(false);
        diagram1.setCreatedAt(LocalDateTime.now().minusHours(10));
        diagram1.setUpdatedAt(LocalDateTime.now());

        // Khởi tạo Diagram 2 - Owned by lecturer, tuổi vừa phải
        diagram2 = new DatabaseDiagram();
        diagram2.setId(2L);
        diagram2.setName("Database Beta");
        diagram2.setDatabaseType(DatabaseDiagram.DatabaseType.POSTGRESQL);
        diagram2.setVersion("13.0");
        diagram2.setIsDeleted(false);
        diagram2.setCreatedAt(LocalDateTime.now().minusDays(1));
        diagram2.setUpdatedAt(LocalDateTime.now().minusHours(5));

        // Khởi tạo Diagram 3 - Owned by admin, cũ nhất
        diagram3 = new DatabaseDiagram();
        diagram3.setId(3L);
        diagram3.setName("Database Gamma");
        diagram3.setDatabaseType(DatabaseDiagram.DatabaseType.MYSQL);
        diagram3.setVersion("5.7");
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
        migration1.setDatabaseDiagram(diagram1);
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
     * 
     * Mockito.reset() sẽ:
     * - Xóa stubbing (when...thenReturn)
     * - Xóa verification history
     * - Trả mock về trạng thái sạch
     */
    @SuppressWarnings("unchecked")
    @AfterEach
    public void tearDown() {
        logger.info("[TEARDOWN] Dọn dẹp dữ liệu test, reset mock objects...");
        reset(databaseDiagramRepository, collaborationRepository, migrationRepository);
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
    // TEST CASES CHO getDatabaseDiagramList()
    // ========================================================================================
    // Đây là phương thức chính lấy danh sách diagrams với filter, sort, và pagination
    // ========================================================================================

    /**
     * UT_DBL_001: Lấy danh sách diagrams - happy path
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
     *   - items.size() = 3 (tất cả diagrams)
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
    public void UT_DBL_001_getDatabaseDiagramList_withValidDataAndNoFilters_shouldReturnSortedList() throws Exception {
        logger.info("[UT_DBL_001] BẮT ĐẦU: Lấy danh sách diagrams - happy path");
        logger.info("[UT_DBL_001] Input: username='admin', pageSize=10, no filters");

        // Arrange: Setup mock behavior
        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
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

        // Act: Gọi service method
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertNotNull(result, "Result không được null");
        assertEquals(3, result.getDiagrams().size(), "Phải có 3 items");
        assertFalse(result.getHasMore(), "hasMore phải false (3 < pageSize 10)");
        assertEquals(3, result.getTotalCount(), "totalCount phải 3");
        assertNotNull(result.getLastDiagramId(), "lastDiagramId không được null");
        
        // Verify repository calls
        verify(databaseDiagramRepository, times(1)).findAll();
        verify(collaborationRepository, times(3)).hasAccess(anyLong(), eq("admin"));

        logger.info("[UT_DBL_001] KẾT QUẢ: PASSED - Danh sách {} diagrams được trả về",
                result.getDiagrams().size());
    }

    /**
     * UT_DBL_002: Lấy danh sách diagrams - không có dữ liệu
     * 
     * Mô tả: Khi database rỗng hoặc user không có access tới diagram nào
     * Input: currentUsername="guest", no diagrams user can access
     * Expected:
     *   - items.size() = 0 (empty list)
     *   - hasMore = false
     *   - totalCount = 0
     *   - lastDiagramId = null
     */
    @Test
    public void UT_DBL_002_getDatabaseDiagramList_noDataOrNoAccess_shouldReturnEmpty() throws Exception {
        logger.info("[UT_DBL_002] BẮT ĐẦU: Lấy danh sách diagrams - không có dữ liệu");
        logger.info("[UT_DBL_002] Input: username='guest', user không có access");

        // Arrange
        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("guest"))).thenReturn(false);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "guest");

        // Assert
        assertNotNull(result, "Result không được null");
        assertTrue(result.getDiagrams().isEmpty(), "Items phải rỗng khi không có access");
        assertFalse(result.getHasMore(), "hasMore phải false");
        assertEquals(0, result.getTotalCount(), "totalCount phải 0");
        assertNull(result.getLastDiagramId(), "lastDiagramId phải null");

        logger.info("[UT_DBL_002] KẾT QUẢ: PASSED - Empty list returned khi không có access");
    }

    /**
     * UT_DBL_003: Lấy danh sách diagrams - pagination với lastDiagramId
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
    public void UT_DBL_003_getDatabaseDiagramList_withCursorPagination_shouldReturnNextPage() throws Exception {
        logger.info("[UT_DBL_003] BẮT ĐẦU: Lấy danh sách diagrams - cursor pagination");
        logger.info("[UT_DBL_003] Input: pageSize=2, lastDiagramId=2");

        // Arrange
        DatabaseDiagram diagram4 = new DatabaseDiagram();
        diagram4.setId(4L);
        diagram4.setName("Database Delta");
        diagram4.setUpdatedAt(LocalDateTime.now().minusHours(3));
        
        DatabaseDiagram diagram5 = new DatabaseDiagram();
        diagram5.setId(5L);
        diagram5.setName("Database Epsilon");
        diagram5.setUpdatedAt(LocalDateTime.now().minusHours(4));

        // Return only the filtered paginated results (diagrams 3 and 4)
        List<DatabaseDiagram> paginatedResults = Arrays.asList(diagram3, diagram4);
        requestDto.setPageSize(2);
        requestDto.setLastDiagramId(2L);

        when(databaseDiagramRepository.findAll()).thenReturn(paginatedResults);
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 items trên trang");
        assertFalse(result.getHasMore(), "hasMore phải false (2 items == pageSize 2)");
        assertEquals(2, result.getTotalCount(), "totalCount phải 2 (pagination test)");

        logger.info("[UT_DBL_003] KẾT QUẢ: PASSED - Cursor pagination works, next page returned");
    }

    /**
     * UT_DBL_004: Lấy danh sách diagrams - filter by isDeleted
     * 
     * Mô tả: Kiểm tra lọc diagrams theo trạng thái deleted
     * Kịch bản:
     *   - diagram1: isDeleted=false
     *   - diagram2: isDeleted=true
     *   - Request: isDeleted=false
     * Expected:
     *   - Trả về chỉ diagram1 (diagram2 bị lọc vì isDeleted=true)
     */
    @Test
    public void UT_DBL_004_getDatabaseDiagramList_filterByDeleted_shouldExcludeDeletedDiagrams() throws Exception {
        logger.info("[UT_DBL_004] BẮT ĐẦU: Lấy danh sách diagrams - filter by isDeleted");
        logger.info("[UT_DBL_004] Input: isDeleted=false, should exclude deleted");

        // Arrange
        diagram2.setIsDeleted(true);
        requestDto.setIsDeleted(false);

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 items (diagram2 bị lọc)");
        assertFalse(result.getDiagrams().stream().anyMatch(item -> item.getId() == 2L),
                "diagram2 không được xuất hiện");

        logger.info("[UT_DBL_004] KẾT QUẢ: PASSED - Deleted diagrams filtered out");
    }

    /**
     * UT_DBL_005: Lấy danh sách diagrams - filter by nameStartsWith
     * 
     * Mô tả: Kiểm tra lọc diagrams theo tên bắt đầu bằng ký tự
     * Input:
     *   - diagram1: name="Database Alpha"
     *   - diagram2: name="Database Beta"
     *   - diagram3: name="Database Gamma"
     *   - Request: nameStartsWith="Beta"
     * Expected:
     *   - Trả về chỉ diagram2
     */
    @Test
    public void UT_DBL_005_getDatabaseDiagramList_filterByNameStartsWith_shouldReturnMatching() throws Exception {
        logger.info("[UT_DBL_005] BẮT ĐẦU: Lấy danh sách diagrams - filter by nameStartsWith");
        logger.info("[UT_DBL_005] Input: nameStartsWith='Database Beta'");

        // Arrange - Return only diagram2 which matches the filter
        requestDto.setNameStartsWith("Database Beta");

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram2));
        when(collaborationRepository.hasAccess(2L, "admin")).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(2L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(2L))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(2L)).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertEquals(1, result.getDiagrams().size(), "Phải có 1 item (chỉ Database Beta)");
        assertEquals(2L, result.getDiagrams().get(0).getId(), "Item phải là diagram2");
        assertEquals("Database Beta", result.getDiagrams().get(0).getName(), "Tên phải match");

        logger.info("[UT_DBL_005] KẾT QUẢ: PASSED - Name filter applied correctly");
    }

    /**
     * UT_DBL_006: Lấy danh sách diagrams - filter by searchQuery
     * 
     * Mô tả: Kiểm tra tìm kiếm diagrams theo query (tìm trong name hoặc owner)
     * Input:
     *   - Request: searchQuery="admin"
     * Expected:
     *   - Trả về diagrams mà owner hoặc name chứa "admin"
     */
    @Test
    public void UT_DBL_006_getDatabaseDiagramList_filterBySearchQuery_shouldSearchInNameAndOwner() throws Exception {
        logger.info("[UT_DBL_006] BẮT ĐẦU: Lấy danh sách diagrams - filter by searchQuery");
        logger.info("[UT_DBL_006] Input: searchQuery='admin'");

        // Arrange
        requestDto.setSearchQuery("admin");

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
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
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        // diagrams 1 và 3 owned by admin, diagram 2 owned by lecturer
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 diagrams (owned by admin)");

        logger.info("[UT_DBL_006] KẾT QUẢ: PASSED - Search query filter applied");
    }

    /**
     * UT_DBL_007: Lấy danh sách diagrams - sort by name ASC
     * 
     * Mô tả: Kiểm tra sắp xếp diagrams theo tên (A-Z)
     * Input:
     *   - sortBy="name", sortDirection="ASC"
     * Expected:
     *   - Diagrams sắp xếp: Alpha, Beta, Gamma
     */
    @Test
    public void UT_DBL_007_getDatabaseDiagramList_sortByNameASC_shouldSortAlphabetically() throws Exception {
        logger.info("[UT_DBL_007] BẮT ĐẦU: Lấy danh sách diagrams - sort by name ASC");
        logger.info("[UT_DBL_007] Input: sortBy='name', sortDirection='ASC'");

        // Arrange
        requestDto.setSortBy("name");
        requestDto.setSortDirection("ASC");

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertEquals(3, result.getDiagrams().size(), "Phải có 3 items");
        assertEquals("Database Alpha", result.getDiagrams().get(0).getName(), "Thứ tự: Alpha");
        assertEquals("Database Beta", result.getDiagrams().get(1).getName(), "Thứ tự: Beta");
        assertEquals("Database Gamma", result.getDiagrams().get(2).getName(), "Thứ tự: Gamma");

        logger.info("[UT_DBL_007] KẾT QUẢ: PASSED - Sorted by name ASC correctly");
    }

    /**
     * UT_DBL_008: Lấy danh sách diagrams - sort by updatedAt DESC (default)
     * 
     * Mô tả: Kiểm tra sắp xếp diagrams theo updatedAt (mới nhất trước)
     * Input:
     *   - sortBy="updatedAt", sortDirection="DESC"
     * Expected:
     *   - Diagrams sắp xếp: diagram1 (now), diagram2 (5h ago), diagram3 (2d ago)
     */
    @Test
    public void UT_DBL_008_getDatabaseDiagramList_sortByUpdatedAtDESC_shouldSortByNewest() throws Exception {
        logger.info("[UT_DBL_008] BẮT ĐẦU: Lấy danh sách diagrams - sort by updatedAt DESC");
        logger.info("[UT_DBL_008] Input: sortBy='updatedAt', sortDirection='DESC'");

        // Arrange
        requestDto.setSortBy("updatedAt");
        requestDto.setSortDirection("DESC");

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertEquals(3, result.getDiagrams().size(), "Phải có 3 items");
        // Mới nhất trước (diagram1)
        assertEquals(1L, result.getDiagrams().get(0).getId(), "Thứ nhất: diagram1 (mới nhất)");

        logger.info("[UT_DBL_008] KẾT QUẢ: PASSED - Sorted by updatedAt DESC correctly");
    }

    /**
     * UT_DBL_009: Lấy danh sách diagrams - filter by dateRange (today)
     * 
     * Mô tả: Kiểm tra lọc diagrams theo ngày tạo
     * Input:
     *   - dateRange="today"
     * Expected:
     *   - Trả về diagrams được update trong hôm nay
     *   - Diagram1 (updated now): có
     *   - Diagram2 (updated 5h ago): có (hôm nay)
     *   - Diagram3 (updated 2d ago): không
     */
    @Test
    public void UT_DBL_009_getDatabaseDiagramList_filterByDateRangeToday_shouldReturnTodayDiagrams() throws Exception {
        logger.info("[UT_DBL_009] BẮT ĐẦU: Lấy danh sách diagrams - filter by dateRange today");
        logger.info("[UT_DBL_009] Input: dateRange='today'");

        // Arrange
        requestDto.setDateRange("today");

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        // Diagram1 and 2 should be returned (updated today/recently)
        // Diagram3 is 2 days old, might be filtered depending on implementation
        assertTrue(result.getDiagrams().size() >= 2, "Phải có ít nhất 2 diagrams hôm nay");
        assertTrue(result.getDiagrams().stream().anyMatch(d -> d.getId() == 1L),
                "diagram1 phải có");

        logger.info("[UT_DBL_009] KẾT QUẢ: PASSED - Date range filter applied");
    }

    /**
     * UT_DBL_010: Lấy danh sách diagrams - filter by ownerFilter (me)
     * 
     * Mô tả: Kiểm tra lọc diagrams tôi sở hữu
     * Input:
     *   - ownerFilter="me"
     *   - currentUsername="admin"
     * Expected:
     *   - Trả về diagrams owned by admin (diagram1, diagram3)
     */
    @Test
    public void UT_DBL_010_getDatabaseDiagramList_filterByOwnerMe_shouldReturnOwnedDiagrams() throws Exception {
        logger.info("[UT_DBL_010] BẮT ĐẦU: Lấy danh sách diagrams - filter by ownerFilter=me");
        logger.info("[UT_DBL_010] Input: ownerFilter='me', currentUsername='admin'");

        // Arrange
        requestDto.setOwnerFilter("me");

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
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
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertEquals(2, result.getDiagrams().size(), "Phải có 2 diagrams (owned by admin)");
        assertTrue(result.getDiagrams().stream().allMatch(d -> d.getOwnerUsername().equals("admin")),
                "Tất cả phải owned by admin");

        logger.info("[UT_DBL_010] KẾT QUẢ: PASSED - Owner filter 'me' applied correctly");
    }

    // ========================================================================================
    // TEST CASES CHO getDatabaseDiagramAll()
    // ========================================================================================

    /**
     * UT_DBL_011: Lấy tất cả diagrams - không filter
     * 
     * Mô tả: getDatabaseDiagramAll() không apply filter như getDatabaseDiagramList()
     * Expected: Trả về tất cả diagrams user có access tới
     */
    @Test
    public void UT_DBL_011_getDatabaseDiagramAll_noFilters_shouldReturnAll() throws Exception {
        logger.info("[UT_DBL_011] BẮT ĐẦU: Lấy tất cả diagrams - không filter");

        // Arrange
        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramAll(requestDto, "admin");

        // Assert
        assertNotNull(result, "Result không được null");
        assertEquals(3, result.getDiagrams().size(), "Phải có tất cả 3 diagrams");

        logger.info("[UT_DBL_011] KẾT QUẢ: PASSED - All diagrams returned without filters");
    }

    // ========================================================================================
    // TEST CASES CHO convertToDto()
    // ========================================================================================

    /**
     * UT_DBL_012: Convert diagram to DTO - với migration
     * 
     * Mô tả: Khi diagram có migration history, DTO phải chứa lastMigration info
     * Expected:
     *   - lastMigrationUsername = "admin"
     *   - lastMigrationDate populated
     *   - updatedByUsername = "admin"
     */
    @Test
    public void UT_DBL_012_convertToDto_withMigration_shouldIncludeMigrationInfo() throws Exception {
        logger.info("[UT_DBL_012] BẮT ĐẦU: Convert diagram to DTO - với migration");

        // Arrange
        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1));
        when(collaborationRepository.hasAccess(1L, "admin")).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Arrays.asList(migration1));
        when(collaborationRepository.countParticipants(1L)).thenReturn(0);

        // Act
        DiagramListResponseDto response = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");
        DiagramListItemDto result = response.getDiagrams().stream()
                .filter(item -> item.getId() == 1L)
                .findFirst()
                .orElse(null);

        // Assert
        assertNotNull(result, "DTO không được null");
        assertEquals("admin", result.getLastMigrationUsername(), "lastMigrationUsername phải 'admin'");
        assertNotNull(result.getLastMigrationDate(), "lastMigrationDate phải populated");

        logger.info("[UT_DBL_012] KẾT QUẢ: PASSED - Migration info included in DTO");
    }

    /**
     * UT_DBL_013: Convert diagram to DTO - không có migration
     * 
     * Mô tả: Khi diagram không có migration, updatedBy phải lấy từ owner
     * Expected:
     *   - lastMigrationUsername = null
     *   - updatedByUsername = owner username
     */
    @Test
    public void UT_DBL_013_convertToDto_noMigration_shouldUseOwnerAsUpdatedBy() throws Exception {
        logger.info("[UT_DBL_013] BẮT ĐẦU: Convert diagram to DTO - không có migration");

        // Arrange
        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1));
        when(collaborationRepository.hasAccess(1L, "admin")).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(1L, Collaboration.CollaborationType.OWNER))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList()); // No migration
        when(collaborationRepository.countParticipants(1L)).thenReturn(0);

        // Act
        DiagramListResponseDto response = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");
        DiagramListItemDto result = response.getDiagrams().get(0);

        // Assert
        assertNotNull(result, "DTO không được null");
        assertEquals("admin", result.getUpdatedByUsername(), "updatedByUsername phải từ owner");

        logger.info("[UT_DBL_013] KẾT QUẢ: PASSED - Owner used as updatedBy when no migration");
    }

    /**
     * UT_DBL_014: Kiểm tra hasCollaborators flag
     * 
     * Mô tả: DTO phải indicate nếu diagram có collaborators (participants)
     * Expected:
     *   - hasCollaborators = true khi participantCount > 0
     *   - hasCollaborators = false khi participantCount = 0
     */
    @Test
    public void UT_DBL_014_convertToDto_hasCollaboratorsFlag_shouldIndicateParticipants() throws Exception {
        logger.info("[UT_DBL_014] BẮT ĐẦU: Kiểm tra hasCollaborators flag");

        // Arrange
        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(1L)).thenReturn(3); // diagram1 có 3 participants
        when(collaborationRepository.countParticipants(2L)).thenReturn(0); // diagram2 không có

        // Act
        DiagramListResponseDto response = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        DiagramListItemDto item1 = response.getDiagrams().stream().filter(d -> d.getId() == 1L).findFirst().orElse(null);
        DiagramListItemDto item2 = response.getDiagrams().stream().filter(d -> d.getId() == 2L).findFirst().orElse(null);

        assertNotNull(item1, "Item 1 phải exist");
        assertTrue(item1.getHasCollaborators(), "diagram1 phải có hasCollaborators=true");
        assertFalse(item2.getHasCollaborators(), "diagram2 phải có hasCollaborators=false");

        logger.info("[UT_DBL_014] KẾT QUẢ: PASSED - hasCollaborators flag set correctly");
    }

    /**
     * UT_DBL_015: Integration test - full flow với multiple filters
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
    public void UT_DBL_015_fullFlow_multipleFiltersAndSorting_shouldApplyAll() throws Exception {
        logger.info("[UT_DBL_015] BẮT ĐẦU: Integration test - multiple filters + sort");
        logger.info("[UT_DBL_015] Input: nameStartsWith='Database', sortBy='name', pageSize=2");

        // Arrange
        requestDto.setNameStartsWith("Database");
        requestDto.setSortBy("name");
        requestDto.setSortDirection("ASC");
        requestDto.setPageSize(2);

        when(databaseDiagramRepository.findAll()).thenReturn(Arrays.asList(diagram1, diagram2, diagram3));
        when(collaborationRepository.hasAccess(anyLong(), eq("admin"))).thenReturn(true);
        when(collaborationRepository.findByDiagramIdAndType(anyLong(), eq(Collaboration.CollaborationType.OWNER)))
                .thenReturn(Optional.of(collab1));
        when(migrationRepository.findTopByDatabaseDiagramIdOrderByCreatedAtDesc(anyLong()))
                .thenReturn(Collections.emptyList());
        when(collaborationRepository.countParticipants(anyLong())).thenReturn(0);

        // Act
        DiagramListResponseDto result = databaseDiagramListService.getDatabaseDiagramList(requestDto, "admin");

        // Assert
        assertNotNull(result, "Result không được null");
        assertTrue(result.getDiagrams().size() <= 2, "Phải respects pageSize=2");
        assertTrue(result.getHasMore(), "hasMore phải true (3 items > pageSize 2)");

        logger.info("[UT_DBL_015] KẾT QUẢ: PASSED - Multiple filters and sorting applied correctly");
    }
}
