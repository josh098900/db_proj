package com.university.warehouse_etl.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    // --- DTO Records for our reports ---
    public record DepartmentPerformanceDTO(String departmentName, Long studentsPassed, BigDecimal averageGrade) {}
    public record EnrollmentTrendDTO(String yearMonth, Long enrollmentCount) {}
    public record DiversityDTO(String category, Long count) {}
    public record TuitionRevenueDTO(String departmentName, BigDecimal totalTuition) {}
    public record TopStudentDTO(String studentName, BigDecimal grade) {}
    public record LecturerWorkloadDTO(String lecturerName, Long studentCount) {}
    public record YoYEnrollmentDTO(String monthName, long currentYearCount, long previousYearCount) {}


    private final JdbcTemplate warehouseJdbcTemplate;
    private final JdbcTemplate operationalJdbcTemplate;

    public ReportController(@Qualifier("warehouseJdbcTemplate") JdbcTemplate warehouseJdbcTemplate,
                            @Qualifier("operationalJdbcTemplate") JdbcTemplate operationalJdbcTemplate) {
        this.warehouseJdbcTemplate = warehouseJdbcTemplate;
        this.operationalJdbcTemplate = operationalJdbcTemplate;
    }

    // --- VC & ADMIN Reports ---

    @GetMapping("/department-performance")
    @PreAuthorize("hasAnyRole('VC', 'ADMIN')")
    public List<DepartmentPerformanceDTO> getDepartmentPerformance() {
        String sql = "SELECT d.department_name, SUM(f.is_passed) AS number_of_students_passed, CAST(AVG(f.final_grade) AS NUMERIC(10,2)) AS average_grade FROM fact_enrollment f JOIN dim_department d ON f.department_key = d.department_key WHERE f.is_passed = 1 GROUP BY d.department_name ORDER BY d.department_name";
        return warehouseJdbcTemplate.query(sql, (rs, rowNum) -> new DepartmentPerformanceDTO(rs.getString("department_name"), rs.getLong("number_of_students_passed"), rs.getBigDecimal("average_grade")));
    }

    @GetMapping("/enrollment-trend")
    @PreAuthorize("hasAnyRole('VC', 'ADMIN')")
    public List<EnrollmentTrendDTO> getEnrollmentTrend() {
        String sql = "SELECT d.year || '-' || d.month_name AS year_month, SUM(f.enrollment_count) AS enrollment_count FROM fact_enrollment f JOIN dim_date d ON f.date_key = d.date_key GROUP BY d.year, d.month, d.month_name, year_month ORDER BY d.year, d.month";
        return warehouseJdbcTemplate.query(sql, (rs, rowNum) -> new EnrollmentTrendDTO(rs.getString("year_month"), rs.getLong("enrollment_count")));
    }

    @GetMapping("/student-diversity")
    @PreAuthorize("hasAnyRole('VC', 'ADMIN')")
    public List<DiversityDTO> getStudentDiversity() {
        String sql = "SELECT nationality AS category, COUNT(*) as count FROM dim_student GROUP BY nationality ORDER BY count DESC";
        return warehouseJdbcTemplate.query(sql, (rs, rowNum) -> new DiversityDTO(rs.getString("category"), rs.getLong("count")));
    }

    @GetMapping("/tuition-revenue")
    @PreAuthorize("hasAnyRole('VC', 'ADMIN')")
    public List<TuitionRevenueDTO> getTuitionRevenue() {
        String sql = "SELECT d.department_name, CAST(SUM(f.tuition_fee) AS NUMERIC(15,2)) as total_tuition " +
                     "FROM fact_enrollment f JOIN dim_department d ON f.department_key = d.department_key " +
                     "GROUP BY d.department_name ORDER BY total_tuition DESC";
        return warehouseJdbcTemplate.query(sql, (rs, rowNum) -> new TuitionRevenueDTO(rs.getString("department_name"), rs.getBigDecimal("total_tuition")));
    }
    
    
    @GetMapping("/yoy-enrollment")
    @PreAuthorize("hasAnyRole('VC', 'ADMIN')")
    public List<YoYEnrollmentDTO> getYoyEnrollment() {
        // advanced query uses  Common Table Expression (CTE) and the LAG() window function
        String sql = "WITH monthly_enrollments AS ( " +
                     "  SELECT d.year, d.month, d.month_name, SUM(f.enrollment_count) as total_enrollments " +
                     "  FROM fact_enrollment f " +
                     "  JOIN dim_date d ON f.date_key = d.date_key " +
                     "  GROUP BY d.year, d.month, d.month_name " +
                     "), " +
                     "yoy_comparison AS ( " +
                     "  SELECT year, month, month_name, total_enrollments, " +
                     "  LAG(total_enrollments, 1) OVER (PARTITION BY month_name ORDER BY year) as previous_year_enrollments " +
                     "  FROM monthly_enrollments " +
                     ") " +
                     "SELECT month_name, total_enrollments as current_year_count, " +
                     "COALESCE(previous_year_enrollments, 0) as previous_year_count " +
                     "FROM yoy_comparison " +
                     
                     "WHERE year = (SELECT MAX(d.year) FROM fact_enrollment f JOIN dim_date d ON f.date_key = d.date_key) " +
                     "ORDER BY month";

        return warehouseJdbcTemplate.query(sql, (rs, rowNum) ->
            new YoYEnrollmentDTO(
                rs.getString("month_name"),
                rs.getLong("current_year_count"),
                rs.getLong("previous_year_count")
            ));
    }


    // --- HOD-Specific Reports ---

    private String getDepartmentForHod(Authentication authentication) {
        String username = authentication.getName();
        return operationalJdbcTemplate.queryForObject(
            "SELECT department_name FROM app_user WHERE username = ?", String.class, username);
    }

    @GetMapping("/hod/course-performance")
    @PreAuthorize("hasRole('HOD')")
    public List<Map<String, Object>> getHodCoursePerformance(Authentication authentication) {
        String userDepartment = getDepartmentForHod(authentication);
        String sql = "SELECT c.course_title, SUM(f.is_passed) as passed_count, (COUNT(f.enrollment_fact_id) - SUM(f.is_passed)) as failed_count, CAST(AVG(f.final_grade) AS NUMERIC(10,2)) as average_grade FROM fact_enrollment f JOIN dim_course c ON f.course_key = c.course_key JOIN dim_department d ON f.department_key = d.department_key WHERE d.department_name = ? AND f.final_grade IS NOT NULL GROUP BY c.course_title ORDER BY c.course_title";
        return warehouseJdbcTemplate.queryForList(sql, userDepartment);
    }

    @GetMapping("/hod/top-students")
    @PreAuthorize("hasRole('HOD')")
    public List<TopStudentDTO> getTopStudents(Authentication authentication) {
        String userDepartment = getDepartmentForHod(authentication);
        String sql = "SELECT s.full_name, CAST(AVG(f.final_grade) AS NUMERIC(10,2)) as grade FROM fact_enrollment f JOIN dim_student s ON f.student_key = s.student_key JOIN dim_department d ON f.department_key = d.department_key WHERE d.department_name = ? AND f.final_grade IS NOT NULL GROUP BY s.full_name ORDER BY grade DESC LIMIT 5";
        return warehouseJdbcTemplate.query(sql, (rs, rowNum) -> new TopStudentDTO(rs.getString("full_name"), rs.getBigDecimal("grade")), userDepartment);
    }

    @GetMapping("/hod/lecturer-workload")
    @PreAuthorize("hasRole('HOD')")
    public List<LecturerWorkloadDTO> getLecturerWorkload(Authentication authentication) {
        String userDepartment = getDepartmentForHod(authentication);
        String sql = "SELECT l.full_name, COUNT(f.enrollment_fact_id) as student_count FROM fact_enrollment f JOIN dim_lecturer l ON f.lecturer_key = l.lecturer_key JOIN dim_department d ON f.department_key = d.department_key WHERE d.department_name = ? GROUP BY l.full_name ORDER BY student_count DESC";
        return warehouseJdbcTemplate.query(sql, (rs, rowNum) -> new LecturerWorkloadDTO(rs.getString("full_name"), rs.getLong("student_count")), userDepartment);
    }
}