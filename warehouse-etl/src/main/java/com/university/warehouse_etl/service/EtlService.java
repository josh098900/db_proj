package com.university.warehouse_etl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List; 
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class EtlService {

    // ... (Logger, JdbcTemplate fields, DTOs, and constructor 
    private static final Logger log = LoggerFactory.getLogger(EtlService.class);
    private final JdbcTemplate operationalJdbcTemplate;
    private final JdbcTemplate warehouseJdbcTemplate;
    private record LecturerDTO(Integer id, String firstName, String lastName, String email) {}
    private record CourseDTO(Integer id, String code, String title, String level, Integer credits) {}
    private record StudentDTO(Integer id, String firstName, String lastName, String gender, String nationality, LocalDate dateOfBirth) {}
    private record EnrollmentDTO(Integer studentId, Integer courseId, Integer lecturerId, Integer academicYear, Integer semester, Double finalGrade, String status, Integer departmentId, Double tuitionFee) {}
    public EtlService(@Qualifier("operationalJdbcTemplate") JdbcTemplate operationalJdbcTemplate, @Qualifier("warehouseJdbcTemplate") JdbcTemplate warehouseJdbcTemplate) { this.operationalJdbcTemplate = operationalJdbcTemplate; this.warehouseJdbcTemplate = warehouseJdbcTemplate; }


    // ... (All dimension loading methods are the same as before) ...
    public void loadDepartments() { log.info("--- Starting ETL for Dim_Department... ---"); String extractSql = "SELECT department_id, department_name, dean_name FROM departments"; List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("department_id"), rs.getString("department_name"), rs.getString("dean_name")}); log.info("Extracted {} departments from source.", sourceData.size()); if (sourceData.isEmpty()) { return; } warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_department CASCADE"); log.info("Truncated dim_department table."); String loadSql = "INSERT INTO dim_department (department_id, department_name, dean_name) VALUES (?, ?, ?)"; warehouseJdbcTemplate.batchUpdate(loadSql, sourceData); log.info("Successfully loaded {} records into dim_department.", sourceData.size()); }
    public void loadLecturers() { log.info("--- Starting ETL for Dim_Lecturer... ---"); String extractSql = "SELECT lecturer_id, first_name, last_name, email FROM lecturers"; List<LecturerDTO> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new LecturerDTO(rs.getInt("lecturer_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"))); log.info("Extracted {} lecturers from source.", sourceData.size()); if (sourceData.isEmpty()) { return; } List<Object[]> transformedData = sourceData.stream().map(dto -> new Object[]{dto.id(), dto.firstName() + " " + dto.lastName(), dto.email()}).collect(Collectors.toList()); warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_lecturer CASCADE"); log.info("Truncated dim_lecturer table."); String loadSql = "INSERT INTO dim_lecturer (lecturer_id, full_name, email) VALUES (?, ?, ?)"; warehouseJdbcTemplate.batchUpdate(loadSql, transformedData); log.info("Successfully loaded {} records into dim_lecturer.", transformedData.size()); }
    public void loadCourses() { log.info("--- Starting ETL for Dim_Course... ---"); String extractSql = "SELECT course_id, course_code, course_title, level, credits FROM courses"; List<CourseDTO> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new CourseDTO(rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_title"), rs.getString("level"), rs.getInt("credits"))); log.info("Extracted {} courses from source.", sourceData.size()); if (sourceData.isEmpty()) { return; } List<Object[]> transformedData = sourceData.stream().map(dto -> new Object[]{dto.id(), dto.code(), dto.title(), dto.level(), dto.credits()}).collect(Collectors.toList()); warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_course CASCADE"); log.info("Truncated dim_course table."); String loadSql = "INSERT INTO dim_course (course_id, course_code, course_title, level, credits) VALUES (?, ?, ?, ?, ?)"; warehouseJdbcTemplate.batchUpdate(loadSql, transformedData); log.info("Successfully loaded {} records into dim_course.", transformedData.size()); }
    public void loadStudents() { log.info("--- Starting ETL for Dim_Student... ---"); String extractSql = "SELECT student_id, first_name, last_name, gender, nationality, date_of_birth FROM students"; List<StudentDTO> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new StudentDTO(rs.getInt("student_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("gender"), rs.getString("nationality"), rs.getDate("date_of_birth").toLocalDate())); log.info("Extracted {} students from source.", sourceData.size()); if (sourceData.isEmpty()) { return; } List<Object[]> transformedData = sourceData.stream().map(dto -> { String fullName = dto.firstName() + " " + dto.lastName(); int age = Period.between(dto.dateOfBirth(), LocalDate.now()).getYears(); String ageGroup; if (age <= 21) { ageGroup = "18-21"; } else if (age <= 25) { ageGroup = "22-25"; } else { ageGroup = "26+"; } return new Object[]{ dto.id(), fullName, dto.gender(), dto.nationality(), ageGroup }; }).collect(Collectors.toList()); warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_student CASCADE"); log.info("Truncated dim_student table."); String loadSql = "INSERT INTO dim_student (student_id, full_name, gender, nationality, age_group) VALUES (?, ?, ?, ?, ?)"; warehouseJdbcTemplate.batchUpdate(loadSql, transformedData); log.info("Successfully loaded {} records into dim_student.", sourceData.size()); }
    public void loadDateDimension() { log.info("--- Starting ETL for Dim_Date... ---"); LocalDate startDate = LocalDate.of(2020, 1, 1); LocalDate endDate = LocalDate.of(2025, 12, 31); List<Object[]> dateRecords = new ArrayList<>(); DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd"); for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) { int dateKey = Integer.parseInt(date.format(keyFormatter)); int month = date.getMonthValue(); int quarter = (month - 1) / 3 + 1; String academicYear; String semester; if (month >= 9) { academicYear = date.getYear() + "/" + (date.getYear() + 1); semester = "Semester 1"; } else if (month <= 5) { academicYear = (date.getYear() - 1) + "/" + date.getYear(); semester = "Semester 2"; } else { academicYear = (date.getYear() - 1) + "/" + date.getYear(); semester = "Summer"; } dateRecords.add(new Object[]{ dateKey, java.sql.Date.valueOf(date), date.getDayOfWeek().name(), date.getMonthValue(), date.getMonth().name(), quarter, date.getYear(), academicYear, semester }); } log.info("Generated {} date records.", dateRecords.size()); warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_date CASCADE"); log.info("Truncated dim_date table."); String loadSql = "INSERT INTO dim_date (date_key, full_date, day_of_week, month, month_name, quarter, year, academic_year, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; warehouseJdbcTemplate.batchUpdate(loadSql, dateRecords); log.info("Successfully loaded {} records into dim_date.", dateRecords.size()); }


    public void loadFactEnrollment() {
        log.info("--- Starting ETL for Fact_Enrollment... ---");
        Map<Integer, Integer> studentKeyMap = loadDimensionMap("SELECT student_id, student_key FROM dim_student");
        Map<Integer, Integer> courseKeyMap = loadDimensionMap("SELECT course_id, course_key FROM dim_course");
        Map<Integer, Integer> lecturerKeyMap = loadDimensionMap("SELECT lecturer_id, lecturer_key FROM dim_lecturer");
        Map<Integer, Integer> departmentKeyMap = loadDimensionMap("SELECT department_id, department_key FROM dim_department");
        log.info("Dimension key maps loaded into memory.");

        String extractSql = "SELECT e.student_id, e.course_id, e.lecturer_id, e.academic_year, e.semester, " +
                            "e.final_grade, e.status, c.department_id, c.tuition_fee " +
                            "FROM enrollments e JOIN courses c ON e.course_id = c.course_id";

        List<EnrollmentDTO> sourceEnrollments = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> {
            // Get as BigDecimal first, then convert to Double
            BigDecimal finalGradeBd = rs.getBigDecimal("final_grade");
            Double finalGrade = (finalGradeBd == null) ? null : finalGradeBd.doubleValue();

            BigDecimal tuitionFeeBd = rs.getBigDecimal("tuition_fee");
            Double tuitionFee = (tuitionFeeBd == null) ? 0.0 : tuitionFeeBd.doubleValue();

            return new EnrollmentDTO(
                rs.getInt("student_id"),
                rs.getInt("course_id"),
                rs.getInt("lecturer_id"),
                rs.getInt("academic_year"),
                rs.getInt("semester"),
                finalGrade, // Use the converted Double
                rs.getString("status"),
                rs.getInt("department_id"),
                tuitionFee // Use the converted Double
            );
        });
        log.info("Extracted {} enrollment records from source.", sourceEnrollments.size());
        if (sourceEnrollments.isEmpty()) { return; }

        DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        List<Object[]> factRecords = new ArrayList<>();
        for (EnrollmentDTO dto : sourceEnrollments) {
            LocalDate factDate = (dto.semester == 1) ? LocalDate.of(dto.academicYear, 9, 1) : LocalDate.of(dto.academicYear + 1, 2, 1);
            Integer dateKey = Integer.parseInt(factDate.format(keyFormatter));
            Integer studentKey = studentKeyMap.get(dto.studentId);
            Integer courseKey = courseKeyMap.get(dto.courseId);
            Integer lecturerKey = lecturerKeyMap.get(dto.lecturerId);
            Integer departmentKey = departmentKeyMap.get(dto.departmentId);
            int isPassed = "Passed".equalsIgnoreCase(dto.status) ? 1 : 0;
            if (studentKey != null && courseKey != null && lecturerKey != null && departmentKey != null) {
                factRecords.add(new Object[]{ dateKey, studentKey, courseKey, departmentKey, lecturerKey, dto.finalGrade, dto.tuitionFee, 1, isPassed });
            } else {
                log.warn("Skipping enrollment record due to missing key. StudentId: {}, CourseId: {}", dto.studentId, dto.courseId);
            }
        }
        log.info("Transformed {} records.", factRecords.size());

        warehouseJdbcTemplate.execute("TRUNCATE TABLE fact_enrollment");
        log.info("Truncated fact_enrollment table.");
        String loadSql = "INSERT INTO fact_enrollment (date_key, student_key, course_key, department_key, lecturer_key, final_grade, tuition_fee, enrollment_count, is_passed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        warehouseJdbcTemplate.batchUpdate(loadSql, factRecords);
        log.info("Successfully loaded {} records into fact_enrollment.", factRecords.size());
    }

    private Map<Integer, Integer> loadDimensionMap(String sql) {
        return warehouseJdbcTemplate.query(sql, rs -> {
            Map<Integer, Integer> map = new java.util.HashMap<>();
            while (rs.next()) {
                map.put(rs.getInt(1), rs.getInt(2));
            }
            return map;
        });
    }
}