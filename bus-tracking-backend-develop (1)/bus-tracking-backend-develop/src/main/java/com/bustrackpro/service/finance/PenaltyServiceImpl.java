package com.bustrackpro.service.finance;

import com.bustrackpro.dto.request.finance.PenaltyRequest;
import com.bustrackpro.dto.response.finance.PenaltyRecordResponse;
import com.bustrackpro.modal.bus.Bus;
import com.bustrackpro.modal.finance.PenaltyRecord;
import com.bustrackpro.modal.schedule.AssignmentStatus;
import com.bustrackpro.modal.schedule.ScheduleAssignment;
import com.bustrackpro.modal.user.Users;
import com.bustrackpro.repository.bus.BusRepository;
import com.bustrackpro.repository.finance.PenaltyRecordRepository;
import com.bustrackpro.repository.schedule.ScheduleAssignmentRepository;
import com.bustrackpro.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyRecordRepository penaltyRecordRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final BusRepository busRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PenaltyRecordResponse applyPenalty(PenaltyRequest request, Integer issuedById) {
        Bus bus = busRepository.findById(request.getBusId())
                .orElseThrow(() -> new IllegalArgumentException("Bus not found"));
        Users issuedBy = userRepository.findById(issuedById)
                .orElseThrow(() -> new IllegalArgumentException("Admin/Authority not found"));

        // 1. Create penalty record
        PenaltyRecord record = PenaltyRecord.builder()
                .bus(bus)
                .issuedBy(issuedBy)
                .penaltyAmount(request.getPenaltyAmount())
                .schedulesSuspended(request.getSchedulesSuspended())
                .reason(request.getReason())
                .build();
        
        penaltyRecordRepository.save(record);

        // 2. Suspend schedules if required
        if (request.getSchedulesSuspended() != null && request.getSchedulesSuspended() > 0) {
            List<ScheduleAssignment> nextAssignments = scheduleAssignmentRepository
                    .findNextScheduledAssignments(bus.getId(), LocalDate.now(), LocalTime.now());

            List<ScheduleAssignment> toCancel = nextAssignments.stream()
                    .limit(request.getSchedulesSuspended())
                    .collect(Collectors.toList());

            for (ScheduleAssignment assignment : toCancel) {
                assignment.setStatus(AssignmentStatus.CANCELLED);
                assignment.setNotes("Cancelled due to penalty suspension: " + request.getReason());
            }

            scheduleAssignmentRepository.saveAll(toCancel);
        }

        return mapToResponse(record);
    }

    @Override
    public List<PenaltyRecordResponse> getAllPenalties() {
        return penaltyRecordRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PenaltyRecordResponse mapToResponse(PenaltyRecord record) {
        return PenaltyRecordResponse.builder()
                .id(record.getId())
                .busNumber(record.getBus().getBusNumber())
                .issuedByName(record.getIssuedBy().getFirstName() + " " + record.getIssuedBy().getLastName())
                .penaltyAmount(record.getPenaltyAmount())
                .schedulesSuspended(record.getSchedulesSuspended())
                .reason(record.getReason())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
