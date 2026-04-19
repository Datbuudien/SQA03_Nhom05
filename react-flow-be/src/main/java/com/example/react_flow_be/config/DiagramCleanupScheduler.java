package com.example.react_flow_be.config;

import com.example.react_flow_be.entity.Diagram;
import com.example.react_flow_be.repository.DiagramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for diagram cleanup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiagramCleanupScheduler {

    private final DiagramRepository diagramRepository;

    /**
     * Auto-delete diagrams that have been in trash for 7 days
     * Runs daily at 2:00 AM
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void autoDeleteExpiredDiagrams() {
        log.info("🗑️ Starting auto-delete job for expired diagrams in trash...");

        try {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

            // Tìm các diagram đã bị xóa mềm và đã quá 7 ngày
            List<Diagram> expiredDiagrams = diagramRepository
                    .findByIsDeletedTrueAndDeletedAtBefore(sevenDaysAgo);

            if (expiredDiagrams.isEmpty()) {
                log.info("✅ No expired diagrams found");
                return;
            }

            log.info("Found {} diagrams to permanently delete", expiredDiagrams.size());

            int deletedCount = 0;
            for (Diagram diagram : expiredDiagrams) {
                try {
                    log.info("Deleting diagram: id={}, name='{}', deletedAt={}",
                            diagram.getId(), diagram.getName(), diagram.getDeletedAt());

                    diagramRepository.delete(diagram);
                    deletedCount++;
                } catch (Exception e) {
                    log.error("Failed to delete diagram id={}: {}",
                            diagram.getId(), e.getMessage(), e);
                }
            }

            log.info("✅ Auto-delete job completed. Deleted {}/{} diagrams",
                    deletedCount, expiredDiagrams.size());

        } catch (Exception e) {
            log.error("❌ Error during auto-delete job: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Log statistics about trash
     * Runs daily at 1:00 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void logTrashStatistics() {
        try {
            long totalInTrash = diagramRepository.countByIsDeleted(true);

            if (totalInTrash > 0) {
                LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
                long expiringSoon = diagramRepository
                        .countByIsDeletedTrueAndDeletedAtBefore(sevenDaysAgo);

                log.info("📊 Trash statistics: {} diagrams in trash, {} will be auto-deleted soon",
                        totalInTrash, expiringSoon);
            }
        } catch (Exception e) {
            log.error("Error logging trash statistics: {}", e.getMessage());
        }
    }
}