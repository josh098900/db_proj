-- Create a new schema to hold our staging tables
CREATE SCHEMA IF NOT EXISTS staging;

-- Staging tables that will hold raw data from the operational DB
DROP TABLE IF EXISTS staging.stg_departments;
CREATE TABLE staging.stg_departments (
    department_id INT,
    department_name VARCHAR(100),
    dean_name VARCHAR(100)
);

DROP TABLE IF EXISTS staging.stg_lecturers;
CREATE TABLE staging.stg_lecturers (
    lecturer_id INT,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    department_id INT
);

DROP TABLE IF EXISTS staging.stg_courses;
CREATE TABLE staging.stg_courses (
    course_id INT,
    course_code VARCHAR(20),
    course_title VARCHAR(150),
    credits INT,
    level VARCHAR(50),
    tuition_fee NUMERIC(10, 2),
    department_id INT
);

DROP TABLE IF EXISTS staging.stg_students;
CREATE TABLE staging.stg_students (
    student_id INT,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    date_of_birth DATE,
    gender VARCHAR(20),
    nationality VARCHAR(50),
    email VARCHAR(100),
    enrollment_date DATE
);

DROP TABLE IF EXISTS staging.stg_enrollments;
CREATE TABLE staging.stg_enrollments (
    enrollment_id INT,
    student_id INT,
    course_id INT,
    lecturer_id INT,
    academic_year INT,
    semester INT,
    final_grade NUMERIC(5, 2),
    status VARCHAR(20)
);