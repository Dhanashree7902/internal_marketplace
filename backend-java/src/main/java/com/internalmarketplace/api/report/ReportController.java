package com.internalmarketplace.api.report;

import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.common.web.IdResponse;
import com.internalmarketplace.api.report.dto.CreateReportRequest;
import com.internalmarketplace.api.report.dto.ReportResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // POST /api/v1/reports
    @PostMapping("/reports")
    public ResponseEntity<IdResponse> createReport(@Valid @RequestBody CreateReportRequest body,
                                                    @CurrentUser FirebaseUserPrincipal user) {
        String id = reportService.createReport(user.uid(), user.email(), user.name(),
                body.targetType(), body.targetId(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    // GET /api/v1/admin/reports
    @GetMapping("/admin/reports")
    public DataResponse<List<ReportResponse>> listReports() {
        return DataResponse.of(reportService.listPendingReports());
    }
}
