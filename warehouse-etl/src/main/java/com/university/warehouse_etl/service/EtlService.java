package com.university.warehouse_etl.service;

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

    // DTOs for processing data in Java
    private record LecturerDTO(Integer id, String firstName, String lastName, String email) {}
    private record StudentDTO(Integer id, String firstName, String lastName, String gender, String nationality, LocalDate dateOfBirth) {}

    public EtlService(@Qualifier("operationalJdbcTemplate") JdbcTemplate operationalJdbcTemplate,
                      @Qualifier("warehouseJdbcTemplate") JdbcTemplate warehouseJdbcTemplate) {
        this.operationalJdbcTemplate = operationalJdbcTemplate;
        this.warehouseJdbcTemplate = warehouseJdbcTemplate;
    }

    // --- ETL PROCESS METHODS (ORACLE COMPATIBLE) ---

    public void loadDateDimension() {
        log.info("--- Starting ETL for Dim_Date... ---");
        List<Object[]> dateRecords = new ArrayList<>();
        DateTimeFormatter keyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        for (LocalDate date = LocalDate.of(2020, 1, 1); !date.isAfter(LocalDate.of(2025, 12, 31)); date = date.plusDays(1)) {
            int month = date.getMonthValue();
            String academicYear, semester;
            if (month >= 9) { academicYear = date.getYear() + "/" + (date.getYear() + 1); semester = "Semester 1"; }
            else if (month <= 5) { academicYear = (date.getYear() - 1) + "/" + date.getYear(); semester = "Semester 2"; }
            else { academicYear = (date.getYear() - 1) + "/" + date.getYear(); semester = "Summer"; }
            dateRecords.add(new Object[]{
                Integer.parseInt(date.format(keyFormatter)), java.sql.Date.valueOf(date),
                date.getDayOfWeek().name(), date.getMonthValue(), date.getMonth().name(),
                (month - 1) / 3 + 1, date.getYear(), academicYear, semester
            });
        }
        log.info("Generated {} date records.", dateRecords.size());
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_date");
        String loadSql = "INSERT INTO dim_date (date_key, full_date, day_of_week, month_num, month_name, quarter_num, year_num, academic_year, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        warehouseJdbcTemplate.batchUpdate(loadSql, dateRecords);
        log.info("Successfully loaded {} records into dim_date.", dateRecords.size());
    }

    public void loadDepartments() {
        log.info("--- Starting ETL for Dim_Department... ---");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE stg_departments");
        String extractSql = "SELECT department_id, department_name, dean_name FROM departments";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("department_id"), rs.getString("department_name"), rs.getString("dean_name")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO stg_departments (department_id, department_name, dean_name) VALUES (?, ?, ?)", sourceData);
        log.info("Extracted {} departments into staging.", sourceData.size());
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_department");
        String loadSql = "INSERT INTO dim_department (department_id, department_name, dean_name) SELECT department_id, department_name, dean_name FROM stg_departments";
        int rowsAffected = warehouseJdbcTemplate.update(loadSql);
        log.info("Successfully loaded {} records into dim_department.", rowsAffected);
    }

    public void loadLecturers() {
        log.info("--- Starting ETL for Dim_Lecturer... ---");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE stg_lecturers");
        String extractSql = "SELECT lecturer_id, first_name, last_name, email, department_id FROM lecturers";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("lecturer_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email"), rs.getInt("department_id")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO stg_lecturers (lecturer_id, first_name, last_name, email, department_id) VALUES (?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} lecturers into staging.", sourceData.size());
        
        List<LecturerDTO> stagedData = warehouseJdbcTemplate.query("SELECT lecturer_id, first_name, last_name, email FROM stg_lecturers", (rs, rowNum) -> new LecturerDTO(rs.getInt("lecturer_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("email")));
        
        // FIX IS HERE: Using Java's '+' for string concatenation
        List<Object[]> transformedData = stagedData.stream().map(dto -> new Object[]{
            dto.id(),
            dto.firstName() + " " + dto.lastName(),
            dto.email()
        }).collect(Collectors.toList());

        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_lecturer");
        warehouseJdbcTemplate.batchUpdate("INSERT INTO dim_lecturer (lecturer_id, full_name, email) VALUES (?, ?, ?)", transformedData);
        log.info("Successfully loaded {} records into dim_lecturer.", transformedData.size());
    }

    public void loadCourses() {
        log.info("--- Starting ETL for Dim_Course... ---");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE stg_courses");
        String extractSql = "SELECT course_id, course_code, course_title, credits, course_level, tuition_fee, department_id FROM courses";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_title"), rs.getInt("credits"), rs.getString("course_level"), rs.getBigDecimal("tuition_fee"), rs.getInt("department_id")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO stg_courses (course_id, course_code, course_title, credits, course_level, tuition_fee, department_id) VALUES (?, ?, ?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} courses into staging.", sourceData.size());
        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_course");
        String loadSql = "INSERT INTO dim_course (course_id, course_code, course_title, course_level, credits) SELECT course_id, course_code, course_title, course_level, credits FROM stg_courses";
        int rowsAffected = warehouseJdbcTemplate.update(loadSql);
        log.info("Successfully loaded {} records into dim_course.", rowsAffected);
    }
    
    public void loadStudents() {
        log.info("--- Starting ETL for Dim_Student... ---");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE stg_students");
        String extractSql = "SELECT student_id, first_name, last_name, date_of_birth, gender, nationality, email, enrollment_date FROM students";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("student_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getDate("date_of_birth"), rs.getString("gender"), rs.getString("nationality"), rs.getString("email"), rs.getDate("enrollment_date")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO stg_students (student_id, first_name, last_name, date_of_birth, gender, nationality, email, enrollment_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} students into staging.", sourceData.size());
        
        List<StudentDTO> stagedData = warehouseJdbcTemplate.query("SELECT student_id, first_name, last_name, gender, nationality, date_of_birth FROM stg_students", (rs, rowNum) -> new StudentDTO(rs.getInt("student_id"), rs.getString("first_name"), rs.getString("last_name"), rs.getString("gender"), rs.getString("nationality"), rs.getDate("date_of_birth").toLocalDate()));
        
        List<Object[]> transformedData = stagedData.stream().map(dto -> {
            // FIX IS HERE: Using Java's '+' for string concatenation
            String fullName = dto.firstName() + " " + dto.lastName();
            int age = Period.between(dto.dateOfBirth(), LocalDate.now()).getYears();
            String ageGroup; if (age <= 21) { ageGroup = "18-21"; } else if (age <= 25) { ageGroup = "22-25"; } else { ageGroup = "26+"; }
            return new Object[]{ dto.id(), fullName, dto.gender(), dto.nationality(), ageGroup };
        }).collect(Collectors.toList());

        warehouseJdbcTemplate.execute("TRUNCATE TABLE dim_student");
        warehouseJdbcTemplate.batchUpdate("INSERT INTO dim_student (student_id, full_name, gender, nationality, age_group) VALUES (?, ?, ?, ?, ?)", transformedData);
        log.info("Successfully loaded {} records into dim_student.", transformedData.size());
    }

    public void loadFactEnrollment() {
        log.info("--- Starting ETL for Fact_Enrollment... ---");
        warehouseJdbcTemplate.execute("TRUNCATE TABLE stg_enrollments");
        String extractSql = "SELECT enrollment_id, student_id, course_id, lecturer_id, academic_year, semester, final_grade, status FROM enrollments";
        List<Object[]> sourceData = operationalJdbcTemplate.query(extractSql, (rs, rowNum) -> new Object[]{rs.getInt("enrollment_id"), rs.getInt("student_id"), rs.getInt("course_id"), rs.getInt("lecturer_id"), rs.getInt("academic_year"), rs.getInt("semester"), rs.getBigDecimal("final_grade"), rs.getString("status")});
        warehouseJdbcTemplate.batchUpdate("INSERT INTO stg_enrollments (enrollment_id, student_id, course_id, lecturer_id, academic_year, semester, final_grade, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", sourceData);
        log.info("Extracted {} enrollments into staging.", sourceData.size());

        log.info("Transforming and loading data from staging into fact_enrollment...");
        Map<Integer, Integer> studentKeyMap = loadDimensionMap("SELECT student_id, student_key FROM dim_student");
        Map<Integer, Integer> courseKeyMap = loadDimensionMap("SELECT course_id, course_key FROM dim_course");
        Map<Integer, Integer> lecturerKeyMap = loadDimensionMap("SELECT lecturer_id, lecturer_key FROM dim_lecturer");
        Map<Integer, Integer> departmentKeyMap = loadDimensionMap("SELECT department_id, department_key FROM dim_department");
        log.info("Dimension key maps loaded into memory.");

        String transformSql = "SELECT se.student_id, se.course_id, se.lecturer_id, se.academic_year, se.semester, se.final_grade, se.status, sc.department_id, sc.tuition_fee FROM stg_enrollments se JOIN stg_courses sc ON se.course_id = sc.course_id";
        List<Object[]> factRecords = warehouseJdbcTemplate.query(transformSql, (rs, rowNum) -> {
            LocalDate factDate = (rs.getInt("semester") == 1) ? LocalDate.of(rs.getInt("academic_year"), 9, 1) : LocalDate.of(rs.getInt("academic_year") + 1, 2, 1);
            Integer dateKey = Integer.parseInt(factDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            Integer studentKey = studentKeyMap.get(rs.getInt("student_id"));
            Integer courseKey = courseKeyMap.get(rs.getInt("course_id"));
            Integer lecturerKey = lecturerKeyMap.get(rs.getInt("lecturer_id"));
            Integer departmentKey = departmentKeyMap.get(rs.getInt("department_id"));
            int isPassed = "Passed".equalsIgnoreCase(rs.getString("status")) ? 1 : 0;
            if (studentKey != null && courseKey != null && lecturerKey != null && departmentKey != null) {
                return new Object[]{ dateKey, studentKey, courseKey, departmentKey, lecturerKey, rs.getBigDecimal("final_grade"), rs.getBigDecimal("tuition_fee"), 1, isPassed };
            } else {
                log.warn("Skipping fact record due to missing key. StudentId: {}, CourseId: {}", rs.getInt("student_id"), rs.getInt("course_id"));
                return null;
            }
        });
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