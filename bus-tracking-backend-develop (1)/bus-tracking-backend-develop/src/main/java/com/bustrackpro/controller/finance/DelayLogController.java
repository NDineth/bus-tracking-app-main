package com.bustrackpro.controller.finance;

import com.bustrackpro.dto.request.finance.DelayLogStatusUpdateRequest;
import com.bustrackpro.dto.response.finance.DelayLogResponse;
import com.bustrackpro.modal.finance.DelayLogRecord;
import com.bustrackpro.repository.finance.DelayLogRecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/finance/delay-logs")
@RequiredArgsConstructor
public class DelayLogController {

    private final DelayLogRecordRepository delayLogRecordRepository;

    private synchronized void seedInitialLogsIfEmpty() {
        if (delayLogRecordRepository.count() == 0) {
            String todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

            List<DelayLogRecord> initialRecords = List.of(
                DelayLogRecord.builder()
                    .busNumber("NC-1234")
                    .route("Homagama – Hanwella")
                    .scheduledDurationMinutes(60)
                    .startTime(LocalDateTime.parse(todayStr + "T08:00:00"))
                    .endTime(LocalDateTime.parse(todayStr + "T09:18:00"))
                    .status("Applied")
                    .date(todayStr)
                    .build(),
                DelayLogRecord.builder()
                    .busNumber("WP-AB-1234")
                    .route("Homagama – Hanwella")
                    .scheduledDurationMinutes(60)
                    .startTime(LocalDateTime.parse(todayStr + "T09:30:00"))
                    .endTime(LocalDateTime.parse(todayStr + "T10:38:00"))
                    .status("Applied")
                    .date(todayStr)
                    .build(),
                DelayLogRecord.builder()
                    .busNumber("WP-CD-5678")
                    .route("Homagama – Hanwella")
                    .scheduledDurationMinutes(60)
                    .startTime(LocalDateTime.parse(todayStr + "T11:00:00"))
                    .endTime(LocalDateTime.parse(todayStr + "T12:22:00"))
                    .status("Under Review")
                    .date(todayStr)
                    .build(),
                DelayLogRecord.builder()
                    .busNumber("ND-5566")
                    .route("Homagama – Hanwella")
                    .scheduledDurationMinutes(60)
                    .startTime(LocalDateTime.parse(todayStr + "T13:15:00"))
                    .endTime(LocalDateTime.parse(todayStr + "T14:27:00"))
                    .status("Waived")
                    .date(todayStr)
                    .build(),
                DelayLogRecord.builder()
                    .busNumber("SP-EF-9012")
                    .route("Homagama – Hanwella")
                    .scheduledDurationMinutes(60)
                    .startTime(LocalDateTime.parse(todayStr + "T14:45:00"))
                    .endTime(LocalDateTime.parse(todayStr + "T16:00:00"))
                    .status("Applied")
                    .date(todayStr)
                    .build(),
                DelayLogRecord.builder()
                    .busNumber("NC-9988")
                    .route("Homagama – Hanwella")
                    .scheduledDurationMinutes(60)
                    .startTime(LocalDateTime.parse(todayStr + "T16:10:00"))
                    .endTime(LocalDateTime.parse(todayStr + "T17:35:00"))
                    .status("Applied")
                    .date(todayStr)
                    .build()
            );

            delayLogRecordRepository.saveAll(initialRecords);
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER', 'BUS_OWNER')")
    public ResponseEntity<List<DelayLogResponse>> getDelayLogs(
            @RequestParam(required = false) String busNumber,
            @RequestParam(required = false) String route,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        seedInitialLogsIfEmpty();

        List<DelayLogRecord> records = delayLogRecordRepository.findAll();

        List<DelayLogResponse> filtered = records.stream()
            .filter(r -> busNumber == null || busNumber.isBlank() || r.getBusNumber().toLowerCase().contains(busNumber.toLowerCase()))
            .filter(r -> route == null || route.isBlank() || r.getRoute().toLowerCase().contains(route.toLowerCase()))
            .filter(r -> from == null || from.isBlank() || r.getDate().compareTo(from) >= 0)
            .filter(r -> to == null || to.isBlank() || r.getDate().compareTo(to) <= 0)
            .map(this::mapToResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'BUS_AUTHORITY_USER')")
    public ResponseEntity<DelayLogResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody DelayLogStatusUpdateRequest request) {

        Integer rawId;
        try {
            String cleanId = id.replace("DL-", "");
            rawId = Integer.parseInt(cleanId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid delay log ID format: " + id);
        }

        DelayLogRecord record = delayLogRecordRepository.findById(rawId)
            .orElseThrow(() -> new IllegalArgumentException("Delay log not found with ID: " + id));

        record.setStatus(request.getStatus());
        DelayLogRecord updated = delayLogRecordRepository.save(record);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    private DelayLogResponse mapToResponse(DelayLogRecord record) {
        return DelayLogResponse.builder()
            .id("DL-" + record.getId())
            .busNumber(record.getBusNumber())
            .route(record.getRoute())
            .scheduledDurationMinutes(record.getScheduledDurationMinutes())
            .startTime(record.getStartTime())
            .endTime(record.getEndTime())
            .status(record.getStatus())
            .date(record.getDate())
            .build();
    }
}
