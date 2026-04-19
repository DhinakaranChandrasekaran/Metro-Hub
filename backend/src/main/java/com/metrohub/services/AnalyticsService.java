package com.metrohub.services;

import com.metrohub.dto.AnalyticsDTOs.*;
import com.metrohub.models.Department;
import com.metrohub.models.RiskScoreSnapshot;
import com.metrohub.models.RiskScoreSnapshot.RiskLevel;
import com.metrohub.models.User;
import com.metrohub.repositories.ComplianceViolationRepository;
import com.metrohub.repositories.DepartmentRepository;
import com.metrohub.repositories.DocumentAcknowledgementRepository;
import com.metrohub.repositories.DocumentRepository;
import com.metrohub.repositories.RiskScoreSnapshotRepository;
import com.metrohub.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final RiskCalculationService riskCalculationService;
    private final RiskScoreSnapshotRepository snapshotRepository;
    private final ComplianceViolationRepository violationRepository;
    private final DocumentAcknowledgementRepository acknowledgementRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    // ============================================
    // ORGANIZATION RISK SUMMARY
    // ============================================

    

    public RiskSummaryDTO getOrganizationRiskSummary() {
        log.info("📊 Generating organization risk summary...");
        LocalDateTime now = LocalDateTime.now();

        // Get all department risk scores
        List<DepartmentRiskDTO> allDeptRisks = getAllDepartmentRisks();

        // Calculate organization risk score (weighted average)
        int orgRiskScore = calculateOrganizationRiskScore(allDeptRisks);
        RiskLevel orgRiskLevel = RiskScoreSnapshot.calculateRiskLevel(orgRiskScore);

        // Department breakdown
        Map<String, Integer> deptRiskDistribution = new HashMap<>();
        deptRiskDistribution.put("LOW", 0);
        deptRiskDistribution.put("MEDIUM", 0);
        deptRiskDistribution.put("HIGH", 0);
        deptRiskDistribution.put("CRITICAL", 0);

        for (DepartmentRiskDTO risk : allDeptRisks) {
            String level = risk.getRiskLevel().name();
            deptRiskDistribution.put(level, deptRiskDistribution.get(level) + 1);
        }

        // Top risk departments (top 5)
        List<DepartmentRiskDTO> topRiskDepts = allDeptRisks.stream()
                .sorted(Comparator.comparing(DepartmentRiskDTO::getRiskScore).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // User statistics
        List<User> allUsers = userRepository.findAll();
        int totalUsers = allUsers.size();
        int activeUsers = (int) allUsers.stream().filter(this::isActiveUser).count();

        // Get user risk distribution
        Map<String, Integer> userRiskDistribution = new HashMap<>();
        userRiskDistribution.put("LOW", 0);
        userRiskDistribution.put("MEDIUM", 0);
        userRiskDistribution.put("HIGH", 0);
        userRiskDistribution.put("CRITICAL", 0);

        int chronicDefaulters = 0;
        int repeatOffenders = 0;
        List<UserRiskDTO> topDefaulters = new ArrayList<>();

        for (User user : allUsers) {
            try {
                UserRiskDTO userRisk = riskCalculationService.calculateUserRisk(user.getId());
                String level = userRisk.getRiskLevel().name();
                userRiskDistribution.put(level, userRiskDistribution.get(level) + 1);

                if (Boolean.TRUE.equals(userRisk.getIsChronicDefaulter())) {
                    chronicDefaulters++;
                }
                if (Boolean.TRUE.equals(userRisk.getIsRepeatOffender())) {
                    repeatOffenders++;
                }

                if (userRisk.getTotalViolationCount() > 0) {
                    topDefaulters.add(userRisk);
                }
            } catch (Exception e) {
                log.debug("Could not calculate risk for user {}", user.getId());
            }
        }

        // Sort and limit top defaulters
        topDefaulters = topDefaulters.stream()
                .sorted(Comparator.comparing(UserRiskDTO::getTotalViolationCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // Violation statistics
        long totalViolations = violationRepository.count();
        long pendingViolations = violationRepository.countAllPending();
        long resolvedViolations = violationRepository.countAllResolved();
        long lateAcknowledged = violationRepository.countLateAcknowledged();
        long criticalViolations = violationRepository.countCriticalViolations();
        long highViolations = violationRepository.countHighSeverityViolations();
        long mediumViolations = violationRepository.countMediumSeverityViolations();

        // Escalation statistics
        long remindersSent = violationRepository.countRemindersSent();
        long deptEscalations = violationRepository.countDeptAdminEscalations();
        long superEscalations = violationRepository.countSuperAdminEscalations();

        // Compliance metrics
        long totalDocs = documentRepository.count();
        long totalAcks = acknowledgementRepository.countTotalAcknowledgements();
        double complianceRate = totalDocs > 0 ?
                Math.max(0, (1.0 - (pendingViolations * 1.0 / totalDocs)) * 100) : 100.0;

        // Legal hold stats
        long legalHoldDocs = documentRepository.countByLegalHoldTrue();
        long legalHoldViolations = violationRepository.countUnresolvedSafetyViolations();  // Approximate

        // Safety stats
        long safetyViolations = violationRepository.countSafetyViolations();
        long pendingSafetyViolations = violationRepository.countUnresolvedSafetyViolations();

        // Trend data
        long violations7Days = violationRepository.countViolationsSince(now.minusDays(7));
        long violations30Days = violationRepository.countViolationsSince(now.minusDays(30));
        long violations90Days = violationRepository.countViolationsSince(now.minusDays(90));

        // Calculate trend
        long previousPeriod = violations90Days - violations30Days;
        int trendPercentage = previousPeriod > 0 ?
                (int) ((violations30Days - previousPeriod) * 100 / previousPeriod) : 0;
        String trendDirection = trendPercentage > 10 ? "UP" : (trendPercentage < -10 ? "DOWN" : "STABLE");

        // Early warning indicators
        List<String> warnings = new ArrayList<>();
        boolean hasRapidIncrease = violations7Days > violations30Days * 0.4;
        boolean hasCriticalDepts = deptRiskDistribution.get("CRITICAL") > 0;
        boolean hasChronicDefaulters = chronicDefaulters >= 3;
        boolean hasSafetyIssues = pendingSafetyViolations > 0;

        if (hasRapidIncrease) warnings.add("⚠️ Rapid violation increase in last 7 days");
        if (hasCriticalDepts) warnings.add("🚨 " + deptRiskDistribution.get("CRITICAL") + " department(s) at critical risk");
        if (hasChronicDefaulters) warnings.add("⚠️ " + chronicDefaulters + " chronic defaulters identified");
        if (hasSafetyIssues) warnings.add("🚨 " + pendingSafetyViolations + " pending safety violations");

        return RiskSummaryDTO.builder()
                .organizationRiskScore(orgRiskScore)
                .organizationRiskLevel(orgRiskLevel)
                .riskLevelColor(getRiskLevelColor(orgRiskLevel))
                .riskLevelDescription(getRiskLevelDescription(orgRiskLevel))
                .totalDepartments(allDeptRisks.size())
                .lowRiskDepartments(deptRiskDistribution.get("LOW"))
                .mediumRiskDepartments(deptRiskDistribution.get("MEDIUM"))
                .highRiskDepartments(deptRiskDistribution.get("HIGH"))
                .criticalRiskDepartments(deptRiskDistribution.get("CRITICAL"))
                .topRiskDepartments(topRiskDepts)
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lowRiskUsers(userRiskDistribution.get("LOW"))
                .mediumRiskUsers(userRiskDistribution.get("MEDIUM"))
                .highRiskUsers(userRiskDistribution.get("HIGH"))
                .criticalRiskUsers(userRiskDistribution.get("CRITICAL"))
                .chronicDefaultersCount(chronicDefaulters)
                .repeatOffendersCount(repeatOffenders)
                .topDefaulters(topDefaulters)
                .totalViolations(totalViolations)
                .pendingViolations(pendingViolations)
                .resolvedViolations(resolvedViolations)
                .lateAcknowledgedViolations(lateAcknowledged)
                .criticalViolations(criticalViolations)
                .highSeverityViolations(highViolations)
                .mediumSeverityViolations(mediumViolations)
                .totalRemindersSent(remindersSent)
                .deptAdminEscalations(deptEscalations)
                .superAdminEscalations(superEscalations)
                .activeEscalations(deptEscalations + superEscalations)
                .overallComplianceRate(complianceRate)
                .totalDocuments(totalDocs)
                .totalAcknowledgements(totalAcks)
                .pendingAcknowledgements(pendingViolations)
                .legalHoldDocuments(legalHoldDocs)
                .legalHoldViolations(legalHoldViolations)
                .safetyDocuments(documentRepository.countSafetyDocuments())
                .safetyViolations(safetyViolations)
                .pendingSafetyViolations(pendingSafetyViolations)
                .violationsLast7Days(violations7Days)
                .violationsLast30Days(violations30Days)
                .violationsLast90Days(violations90Days)
                .violationTrendPercentage(trendPercentage)
                .trendDirection(trendDirection)
                .departmentRiskDistribution(deptRiskDistribution)
                .userRiskDistribution(userRiskDistribution)
                .hasRapidViolationIncrease(hasRapidIncrease)
                .hasCriticalDepartments(hasCriticalDepts)
                .hasMultipleChronicDefaulters(hasChronicDefaulters)
                .hasSafetyComplianceIssues(hasSafetyIssues)
                .activeWarnings(warnings)
                .calculatedAt(now)
                .periodStart(now.minusDays(90))
                .periodEnd(now)
                .calculationVersion("1.0")
                .build();
    }

    // ============================================
    // DEPARTMENT ANALYTICS
    // ============================================

    

    public List<DepartmentRiskDTO> getAllDepartmentRisks() {
        List<Department> departments = departmentRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        List<DepartmentRiskDTO> results = new ArrayList<>();
        for (Department dept : departments) {
            DepartmentRiskDTO dto = calculateDepartmentRiskSafe(dept);
            if (dto != null) {
                results.add(dto);
            }
        }
        return results;
    }

    public List<DepartmentRiskDTO> getDepartmentRisksForDepartment(Long departmentId) {
        Optional<Department> dept = departmentRepository.findById(departmentId);
        if (dept.isPresent()) {
            DepartmentRiskDTO dto = calculateDepartmentRiskSafe(dept.get());
            return dto != null ? List.of(dto) : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    

    public RiskHeatmapDTO getDepartmentRiskHeatmap() {
        log.info("📊 Generating department risk heatmap...");
        LocalDateTime now = LocalDateTime.now();

        List<DepartmentRiskDTO> deptRisks = getAllDepartmentRisks();

        List<RiskHeatmapDTO.HeatmapCell> cells = new ArrayList<>();
        for (DepartmentRiskDTO risk : deptRisks) {
            cells.add(toHeatmapCell(risk));
        }

        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("LOW", (int) cells.stream().filter(this::isLowRiskCell).count());
        distribution.put("MEDIUM", (int) cells.stream().filter(this::isMediumRiskCell).count());
        distribution.put("HIGH", (int) cells.stream().filter(this::isHighRiskCell).count());
        distribution.put("CRITICAL", (int) cells.stream().filter(this::isCriticalRiskCell).count());

        int avgScore = cells.isEmpty() ? 0 :
                (int) cells.stream().mapToInt(RiskHeatmapDTO.HeatmapCell::getRiskScore).average().orElse(0);
        int maxScore = cells.isEmpty() ? 0 :
                cells.stream().mapToInt(RiskHeatmapDTO.HeatmapCell::getRiskScore).max().orElse(0);
        int minScore = cells.isEmpty() ? 0 :
                cells.stream().mapToInt(RiskHeatmapDTO.HeatmapCell::getRiskScore).min().orElse(0);

        String highestRiskDept = cells.stream()
                .max(Comparator.comparingInt(RiskHeatmapDTO.HeatmapCell::getRiskScore))
                .map(RiskHeatmapDTO.HeatmapCell::getDepartmentName)
                .orElse("N/A");
        String lowestRiskDept = cells.stream()
                .min(Comparator.comparingInt(RiskHeatmapDTO.HeatmapCell::getRiskScore))
                .map(RiskHeatmapDTO.HeatmapCell::getDepartmentName)
                .orElse("N/A");

        return RiskHeatmapDTO.builder()
                .title("Department Risk Heatmap")
                .description("Risk scores by department based on violation and compliance data")
                .generatedAt(now)
                .periodCovered("Last 90 days")
                .cells(cells)
                .riskDistribution(distribution)
                .totalDepartments(cells.size())
                .avgRiskScore(avgScore)
                .maxRiskScore(maxScore)
                .minRiskScore(minScore)
                .highestRiskDepartment(highestRiskDept)
                .lowestRiskDepartment(lowestRiskDept)
                .build();
    }

    // ============================================
    // USER ANALYTICS
    // ============================================

    

    public List<UserRiskDTO> getAllUserRisks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<User> users = userRepository.findAll(pageable).getContent();

        List<UserRiskDTO> results = new ArrayList<>();
        for (User user : users) {
            UserRiskDTO dto = calculateUserRiskSafe(user);
            if (dto != null) {
                results.add(dto);
            }
        }
        return results;
    }

    

    public List<TopDefaulterDTO> getTopDefaulters(int limit) {
        log.info("📊 Getting top {} defaulters...", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> defaulters = violationRepository.findDefaulters(1L, pageable);

        List<TopDefaulterDTO> result = new ArrayList<>();
        for (Object[] row : defaulters) {
            Long userId = (Long) row[0];
            String userName = (String) row[1];
            String employeeId = (String) row[2];
            String deptName = (String) row[3];
            Long violationCount = (Long) row[4];

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            UserRiskDTO userRisk = riskCalculationService.calculateUserRisk(userId);

            result.add(TopDefaulterDTO.builder()
                    .userId(userId)
                    .userName(userName)
                    .email(user.getEmail())
                    .employeeId(employeeId)
                    .role(user.getRole().name())
                    .departmentId(user.getDepartmentEntity() != null ? user.getDepartmentEntity().getId() : null)
                    .departmentName(deptName)
                    .departmentCode(user.getDepartmentEntity() != null ? user.getDepartmentEntity().getCode() : null)
                    .totalViolations(violationCount.intValue())
                    .pendingViolations(userRisk.getPendingViolationCount())
                    .resolvedViolations(userRisk.getResolvedViolationCount())
                    .lateAcknowledgements(userRisk.getLateAcknowledgementCount())
                    .riskScore(userRisk.getRiskScore())
                    .riskLevel(userRisk.getRiskLevel())
                    .riskLevelColor(userRisk.getRiskLevelColor())
                    .isChronicDefaulter(userRisk.getIsChronicDefaulter())
                    .isRepeatOffender(userRisk.getIsRepeatOffender())
                    .defaulterCategory(violationCount >= 5 ? "CHRONIC" : (violationCount >= 3 ? "REPEAT" : "MODERATE"))
                    .deptAdminEscalations(userRisk.getDeptAdminEscalationCount())
                    .superAdminEscalations(userRisk.getSuperAdminEscalationCount())
                    .hasActiveEscalation(userRisk.getHasOpenEscalation())
                    .violationsLast30Days(userRisk.getViolationsLast30Days())
                    .violationsLast90Days(userRisk.getViolationsLast90Days())
                    .build());
        }

        return result;
    }

    // ============================================
    // TREND ANALYTICS
    // ============================================

    

    public RiskTrendDTO getViolationTrends(String period) {
        log.info("📊 Generating violation trends for period: {}", period);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period) {
            case "7_DAYS":
                startDate = now.minusDays(7);
                break;
            case "30_DAYS":
                startDate = now.minusDays(30);
                break;
            case "90_DAYS":
                startDate = now.minusDays(90);
                break;
            case "12_MONTHS":
            default:
                startDate = now.minusMonths(12);
                break;
        }

        // Get monthly violation counts
        List<Object[]> monthlyData = violationRepository.getMonthlyViolationCounts(startDate);
        List<RiskTrendDTO.TrendDataPoint> violationTrend = new ArrayList<>();

        for (Object[] row : monthlyData) {
            String month = (String) row[0];
            Long count = (Long) row[1];
            violationTrend.add(RiskTrendDTO.TrendDataPoint.builder()
                    .date(month)
                    .value(count.intValue())
                    .label(month)
                    .build());
        }

        // Get monthly resolved counts
        List<Object[]> resolvedData = violationRepository.getMonthlyResolvedCounts(startDate);
        Map<String, RiskTrendDTO.MonthlyStats> monthlyStats = new HashMap<>();

        for (Object[] row : monthlyData) {
            String month = (String) row[0];
            Long violationCount = (Long) row[1];

            Long resolvedCount = findResolvedCountForMonth(resolvedData, month);

            monthlyStats.put(month, RiskTrendDTO.MonthlyStats.builder()
                    .month(month)
                    .violationCount(violationCount)
                    .resolvedCount(resolvedCount)
                    .newViolations(violationCount.intValue())
                    .closedViolations(resolvedCount.intValue())
                    .build());
        }

        // Calculate trend direction
        int recentViolations = (int) violationRepository.countViolationsSince(now.minusDays(30));
        int olderViolations = (int) violationRepository.countViolationsSince(now.minusDays(60)) - recentViolations;
        int trendPercentage = olderViolations > 0 ?
                ((recentViolations - olderViolations) * 100 / olderViolations) : 0;
        String trendDirection = trendPercentage > 10 ? "UP" : (trendPercentage < -10 ? "DOWN" : "STABLE");

        // Period comparison
        RiskTrendDTO.PeriodComparison comparison = RiskTrendDTO.PeriodComparison.builder()
                .currentPeriod("Last 30 days")
                .comparisonPeriod("Previous 30 days")
                .currentViolations((long) recentViolations)
                .comparisonViolations((long) olderViolations)
                .percentageChange(trendPercentage)
                .changeDirection(trendDirection.equals("UP") ? "DEGRADED" :
                        (trendDirection.equals("DOWN") ? "IMPROVED" : "STABLE"))
                .build();

        // Get historical snapshot count for context
        long snapshotCount = snapshotRepository.count();
        log.debug("Historical risk snapshots available: {}", snapshotCount);

        return RiskTrendDTO.builder()
                .trendPeriod(period)
                .violationTrend(violationTrend)
                .monthlyStatistics(monthlyStats)
                .overallTrendDirection(trendDirection)
                .trendPercentageChange(trendPercentage)
                .thisVsLastPeriod(comparison)
                .build();
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    private int calculateOrganizationRiskScore(List<DepartmentRiskDTO> deptRisks) {
        if (deptRisks.isEmpty()) return 0;

        // Weighted average with emphasis on high-risk departments
        double totalWeight = 0;
        double weightedSum = 0;

        for (DepartmentRiskDTO risk : deptRisks) {
            // Higher weight for critical and high-risk departments
            double weight = switch (risk.getRiskLevel()) {
                case CRITICAL -> 3.0;
                case HIGH -> 2.0;
                case MEDIUM -> 1.5;
                case LOW -> 1.0;
            };
            totalWeight += weight;
            weightedSum += risk.getRiskScore() * weight;
        }

        return (int) Math.round(weightedSum / totalWeight);
    }

    private String getRiskLevelColor(RiskLevel level) {
        return switch (level) {
            case LOW -> "#22c55e";
            case MEDIUM -> "#f59e0b";
            case HIGH -> "#f97316";
            case CRITICAL -> "#ef4444";
        };
    }

    private String getRiskLevelDescription(RiskLevel level) {
        return switch (level) {
            case LOW -> "Risk within acceptable limits";
            case MEDIUM -> "Moderate risk - monitoring recommended";
            case HIGH -> "High risk - immediate attention required";
            case CRITICAL -> "Critical risk - urgent intervention needed";
        };
    }

    private DepartmentRiskDTO calculateDepartmentRiskSafe(Department dept) {
        try {
            return riskCalculationService.calculateDepartmentRisk(dept.getId());
        } catch (Exception e) {
            log.error("Failed to calculate risk for dept {}", dept.getId(), e);
            return null;
        }
    }

    private UserRiskDTO calculateUserRiskSafe(User user) {
        try {
            return riskCalculationService.calculateUserRisk(user.getId());
        } catch (Exception e) {
            log.debug("Failed to calculate risk for user {}", user.getId());
            return null;
        }
    }

    private RiskHeatmapDTO.HeatmapCell toHeatmapCell(DepartmentRiskDTO risk) {
        return RiskHeatmapDTO.HeatmapCell.builder()
                .departmentId(risk.getDepartmentId())
                .departmentName(risk.getDepartmentName())
                .departmentCode(risk.getDepartmentCode())
                .riskScore(risk.getRiskScore())
                .riskLevel(risk.getRiskLevel().name())
                .cellColor(RiskHeatmapDTO.getColorForScore(risk.getRiskScore()))
                .violationCount(risk.getTotalViolationCount())
                .pendingCount(risk.getPendingViolationCount())
                .complianceRate(risk.getComplianceRate())
                .tooltip(String.format("%s: Score %d (%s) - %d violations",
                        risk.getDepartmentName(), risk.getRiskScore(),
                        risk.getRiskLevel(), risk.getTotalViolationCount()))
                .build();
    }

    private Long findResolvedCountForMonth(List<Object[]> resolvedData, String month) {
        for (Object[] r : resolvedData) {
            if (month.equals(r[0])) {
                return (Long) r[1];
            }
        }
        return 0L;
    }

    private boolean isActiveUser(User u) {
        return Boolean.TRUE.equals(u.getIsActive());
    }

    private boolean isLowRiskCell(RiskHeatmapDTO.HeatmapCell c) {
        return "LOW".equals(c.getRiskLevel());
    }

    private boolean isMediumRiskCell(RiskHeatmapDTO.HeatmapCell c) {
        return "MEDIUM".equals(c.getRiskLevel());
    }

    private boolean isHighRiskCell(RiskHeatmapDTO.HeatmapCell c) {
        return "HIGH".equals(c.getRiskLevel());
    }

    private boolean isCriticalRiskCell(RiskHeatmapDTO.HeatmapCell c) {
        return "CRITICAL".equals(c.getRiskLevel());
    }
}
