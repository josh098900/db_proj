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

    private static final Logger log = LoggerFactory.getLogger(EtlService.class);

    private final JdbcTemplate operationalJdbcTemplate;
    private final JdbcTemplate warehouseJdbcTemplate;

    // DTOs are unchanged
    private record LecturerDTO(Integer id, String firstName, String lastName, String email) {}
    
    private record StudentDTO(Integer id, String firstName, String lastName, String gender, String nationality, LocalDate dateOfBirth) {}
    


    public EtlService(@Qualifier("operationalJdbcTemplate") JdbcTemplate operationalJdbcTemplate,
                      @Qualifier("warehouseJdbcTemplate") JdbcTemplate warehouseJdbcTemplate) {
        this.operationalJdbcTemplate = operationalJdbcTemplate;
        this.warehouseJdbcTemplate = warehouseJdbcTemplate;
    }

    // This method is unchanged as it does not depend on the operational DB
    public void loadDateDimension() {
        log.info("--- Starting ETL for Dim_Date... ---");
        LocalDate startDate = LocalDate.of(2020, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);
        List<Object[]> dateRecords = new ArrayList<>();
        DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int dateKey = Integer.parseInt(date.format(keyFormatter));
            int month = date.getMonthValue();
            int quarter = (month - 1) / 3 + 1;
            String academicYear; String semester;
            if (month >= 9) { academicYear = date.getYear() + "/" + (date.getYear() + 1); semester = "Semester 1"; }
            else if (month <= 5) { academicYear = (date.getYear() - 1) + "/" + date.getYear(); semester = "Semester 2"; }
            else { academicYear = (date.getYear() - 1) + "/" + date.getYear(); semester = "Summer"; }
            dateRecords.add(new Object[]{ dateKey, java.sql.Date.valueOf(date), date.getDayOfWeek().name(), date.getMonthValue(), date.getMonth().name(), quarter, date.getYear(), academicYear, semester });
        }
        log.info("Generated {} date records.", dateRecords.size());
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_date CASCADE");
        log.info("Truncated dim_date table.");
        String loadSql = "INSERT INTO dim_date (date_key, full_date, day_of_week, month, month_name, quarter, year, academic_year, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        warehouseJdbcTemplate.batchUpdate(loadSql, dateRecords);
        log.info("Successfully loaded {} records into dim_date.", dateRecords.size());
    }

    // --- REFACTORED METHODS USING STAGING TABLES ---

    public void loadDepartments() {
        log.info("--- Starting ETL for Dim_Department... ---");
        // STAGE 1: Extract from Operational DB into Staging DB
        log.info("[1/2] Extracting data into staging.stg_departments...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE staging.stg_departments");
        String extractSql = "SELECT department_id, department_name, dean_name FROM departments";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("department_id"), rs.getString("department_name"), rs.getString("dean_name")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO staging.stg_departments VALUES (?, ?, ?)", sourceData);
        log.info("Extracted {} departments into staging.", sourceData.size());

        // STAGE 2: Load from Staging into final Dimension Table
        log.info("[2/2] Loading data from staging into dim_department...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_department CASCADE");
        String loadSql = "INSERT INTO dim_department (department_id, department_name, dean_name) SELECT department_id, department_name, dean_name FROM staging.stg_departments";
        int rowsAffected = warehouseJdbcTemplate.update(loadSql);
        log.info("Successfully loaded {} records into dim_department.", rowsAffected);
    }

    public void loadLecturers() {
        log.info("--- Starting ETL for Dim_Lecturer... ---");
        // STAGE 1: Extract to Staging
        log.info("[1/2] Extracting data into staging.stg_lecturers...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE staging.stg_lecturers");
        String extractSql = "SELECT lecturer_id, first_name, last_name, email, department_id FROM lecturers";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("lecturer_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"), rs.getInt("department_id")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO staging.stg_lecturers VALUES (?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} lecturers into staging.", sourceData.size());

        // STAGE 2: Transform & Load from Staging
        log.info("[2/2] Loading data from staging into dim_lecturer...");
        List<LecturerDTO> stagedData = warehouseJdbcTemplate.query("SELECT lecturer_id, first_name, last_name, email FROM staging.stg_lecturers", (rs, rowNum) -> new LecturerDTO(rs.getInt("lecturer_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email")));
        List<Object[]> transformedData = stagedData.stream().map(dto -> new Object[]{dto.id(), dto.firstName() + " " + dto.lastName(), dto.email()}).collect(Collectors.toList());
        
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_lecturer CASCADE");
        warehouseJdbcTemplate.batchUpdate("INSERT INTO dim_lecturer (lecturer_id, full_name, email) VALUES (?, ?, ?)", transformedData);
        log.info("Successfully loaded {} records into dim_lecturer.", transformedData.size());
    }

    
    public void loadCourses() {
        log.info("--- Starting ETL for Dim_Course... ---");
        // STAGE 1: Extract to Staging
        log.info("[1/2] Extracting data into staging.stg_courses...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE staging.stg_courses");
        String extractSql = "SELECT course_id, course_code, course_title, level, credits, department_id, tuition_fee FROM courses";

        
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{
            rs.getInt("course_id"),
            rs.getString("course_code"),
            rs.getString("course_title"),
            rs.getInt("credits"),      
            rs.getString("level"),      
            rs.getBigDecimal("tuition_fee"),
            rs.getInt("department_id")
        });
        
        // The INSERT statement now matches the corrected order
        warehouseJdbcTemplate.batchUpdate("INSERT INTO staging.stg_courses (course_id, course_code, course_title, credits, level, tuition_fee, department_id) VALUES (?, ?, ?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} courses into staging.", sourceData.size());

        // STAGE 2: Load from Staging
        log.info("[2/2] Loading data from staging into dim_course...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_course CASCADE");
        String loadSql = "INSERT INTO dim_course (course_id, course_code, course_title, level, credits) SELECT course_id, course_code, course_title, level, credits FROM staging.stg_courses";
        int rowsAffected = warehouseJdbcTemplate.update(loadSql);
        log.info("Successfully loaded {} records into dim_course.", rowsAffected);
    }
    
    // Replace your existing loadStudents method with this one
    public void loadStudents() {
        log.info("--- Starting ETL for Dim_Student... ---");
        // STAGE 1: Extract to Staging
        log.info("[1/2] Extracting data into staging.stg_students...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE staging.stg_students");
        
        // Ensure all necessary columns, including email, are selected
        String extractSql = "SELECT student_id, first_name, last_name, date_of_birth, gender, nationality, email, enrollment_date FROM students";
        
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{
            rs.getInt("student_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getDate("date_of_birth"),      // Correct order: 4th column is DATE
            rs.getString("gender"),          // Correct order: 5th column is VARCHAR
            rs.getString("nationality"),
            rs.getString("email"),           // This column was missing before
            rs.getDate("enrollment_date")
        });
        
        // The INSERT now has the correct number of placeholders
        warehouseJdbcTemplate.batchUpdate("INSERT INTO staging.stg_students VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} students into staging.", sourceData.size());

        // STAGE 2: Transform & Load from Staging
        log.info("[2/2] Loading data from staging into dim_student...");
        List<StudentDTO> stagedData = warehouseJdbcTemplate.query("SELECT student_id, first_name, last_name, gender, nationality, date_of_birth FROM staging.stg_students", (rs, rowNum) -> new StudentDTO(rs.getInt("student_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("gender"), rs.getString("nationality"), rs.getDate("date_of_birth").toLocalDate()));
        List<Object[]> transformedData = stagedData.stream().map(dto -> {
            String fullName = dto.firstName() + " " + dto.lastName();
            int age = Period.between(dto.dateOfBirth(), LocalDate.now()).getYears();
            String ageGroup; if (age <= 21) { ageGroup = "18-21"; } else if (age <= 25) { ageGroup = "22-25"; } else { ageGroup = "26+"; }
            return new Object[]{ dto.id(), fullName, dto.gender(), dto.nationality(), ageGroup };
        }).collect(Collectors.toList());

        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_student CASCADE");
        warehouseJdbcTemplate.batchUpdate("INSERT INTO dim_student (student_id, full_name, gender, nationality, age_group) VALUES (?, ?, ?, ?, ?)", transformedData);
        log.info("Successfully loaded {} records into dim_student.", transformedData.size());
    }

    public void loadFactEnrollment() {
        log.info("--- Starting ETL for Fact_Enrollment... ---");
        // STAGE 1: Extract to Staging (occurs in a separate method call)
        log.info("[1/2] Extracting data into staging.stg_enrollments...");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE staging.stg_enrollments");
        String extractSql = "SELECT enrollment_id, student_id, course_id, lecturer_id, academic_year, semester, final_grade, status FROM enrollments";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("enrollment_id"), rs.getInt("student_id"), rs.getInt("course_id"), rs.getInt("lecturer_id"), rs.getInt("academic_year"), rs.getInt("semester"), rs.getBigDecimal("final_grade"), rs.getString("status")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO staging.stg_enrollments VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} enrollments into staging.", sourceData.size());

        // STAGE 2: Transform & Load from Staging
        log.info("[2/2] Transforming and loading data from staging into fact_enrollment...");
        Map<Integer, Integer> studentKeyMap = loadDimensionMap("SELECT student_id, student_key FROM dim_student");
        Map<Integer, Integer> courseKeyMap = loadDimensionMap("SELECT course_id, course_key FROM dim_course");
        Map<Integer, Integer> lecturerKeyMap = loadDimensionMap("SELECT lecturer_id, lecturer_key FROM dim_lecturer");
        Map<Integer, Integer> departmentKeyMap = loadDimensionMap("SELECT department_id, department_key FROM dim_department");
        log.info("Dimension key maps loaded into memory.");

        // The new source for facts is a JOIN between staging tables
        String transformSql = "SELECT se.student_id, se.course_id, se.lecturer_id, se.academic_year, se.semester, " +
                              "se.final_grade, se.status, sc.department_id, sc.tuition_fee " +
                              "FROM staging.stg_enrollments se JOIN staging.stg_courses sc ON se.course_id = sc.course_id";

        // The EnrollmentDTO now reads from the staging join result
        List<Object[]> factRecords = warehouseJdbcTemplate.query(transformSql, (rs, rowNum) -> {
            BigDecimal finalGradeBd = rs.getBigDecimal("final_grade");
            Double finalGrade = (finalGradeBd == null) ? null : finalGradeBd.doubleValue();

            // Transform data for the fact table record
            LocalDate factDate = (rs.getInt("semester") == 1) ? LocalDate.of(rs.getInt("academic_year"), 9, 1) : LocalDate.of(rs.getInt("academic_year") + 1, 2, 1);
            Integer dateKey = Integer.parseInt(factDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            Integer studentKey = studentKeyMap.get(rs.getInt("student_id"));
            Integer courseKey = courseKeyMap.get(rs.getInt("course_id"));
            Integer lecturerKey = lecturerKeyMap.get(rs.getInt("lecturer_id"));
            Integer departmentKey = departmentKeyMap.get(rs.getInt("department_id"));
            int isPassed = "Passed".equalsIgnoreCase(rs.getString("status")) ? 1 : 0;
            
            if (studentKey != null && courseKey != null && lecturerKey != null && departmentKey != null) {
                return new Object[]{ dateKey, studentKey, courseKey, departmentKey, lecturerKey, finalGrade, rs.getBigDecimal("tuition_fee"), 1, isPassed };
            } else {
                log.warn("Skipping fact record due to missing key. StudentId: {}, CourseId: {}", rs.getInt("student_id"), rs.getInt("course_id"));
                return null;
            }
        });
        // Filter out any null records that were skipped
        factRecords.removeIf(record -> record == null);
        log.info("Transformed {} records.", factRecords.size());

        warehouseJdbcTemplate.execute("TRUNCATE TABLE fact_enrollment");
        String loadSql = "INSERT INTO fact_enrollment (date_key, student_key, course_key, department_key, lecturer_key, final_grade, tuition_fee, enrollment_count, is_passed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        warehouseJdbcTemplate.batchUpdate(loadSql, factRecords);
        log.info("Successfully loaded {} records into fact_enrollment.", factRecords.size());
    }

    private Map<Integer, Integer> loadDimensionMap(String sql) {
        return warehouseJdbcTemplate.query(sql, rs -> {
            Map<Integer, Integer> map = new java.util.HashMap<>();
            while (rs.next()) { map.put(rs.getInt(1), rs.getInt(2)); }
            return map;
        });
    }
}