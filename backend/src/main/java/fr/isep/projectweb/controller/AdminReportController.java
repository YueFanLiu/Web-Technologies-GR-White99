package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.response.PlatformUsageReportResponse;
import fr.isep.projectweb.model.service.PlatformReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
@Tag(name = "Admin reports", description = "Platform reporting endpoints")
public class AdminReportController {

    private final PlatformReportService platformReportService;

    public AdminReportController(PlatformReportService platformReportService) {
        this.platformReportService = platformReportService;
    }

    @GetMapping("/platform-usage")
    @Operation(summary = "Get platform usage report")
    public PlatformUsageReportResponse getPlatformUsage(@RequestParam(required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                        @RequestParam(required = false)
                                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return platformReportService.getPlatformUsage(from, to, jwt);
    }
}
