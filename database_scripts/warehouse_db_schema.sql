
-- DATA WAREHOUSE SCHEMA (STAR SCHEMA)

-- This script creates the dimension and fact tables for the
-- analytical system (OLAP).

-- Drop tables if they exist to ensure a clean slate on re-run
DROP TABLE IF EXISTS Fact_Enrollment;
DROP TABLE IF EXISTS Dim_Date;
DROP TABLE IF EXISTS Dim_Student;
DROP TABLE IF EXISTS Dim_Course;
DROP TABLE IF EXISTS Dim_Department;
DROP TABLE IF EXISTS Dim_Lecturer;

-- Dimension Table: Dim_Date
CREATE TABLE Dim_Date (
    date_key INT PRIMARY KEY, -- e.g. 20250618
    full_date DATE NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    month INT NOT NULL,
    month_name VARCHAR(10) NOT NULL,
    quarter INT NOT NULL,
    year INT NOT NULL,
    academic_year VARCHAR(10), -- e.g., '2024/2025'
    semester VARCHAR(20)      -- e.g., 'Semester 1'
);

-- Dimension Table: Dim_Student
CREATE TABLE Dim_Student (
    student_key SERIAL PRIMARY KEY, -- Surrogate Key
    student_id INT NOT NULL,        -- Natural Key from operational DB
    full_name VARCHAR(101) NOT NULL,
    gender VARCHAR(20),
    nationality VARCHAR(50),
    age_group VARCHAR(10) -- e.g., '18-21', '22-25'
);

-- Dimension Table: Dim_Course
CREATE TABLE Dim_Course (
    course_key SERIAL PRIMARY KEY, -- Surrogate Key
    course_id INT NOT NULL,        -- Natural Key from operational DB
    course_code VARCHAR(20) NOT NULL,
    course_title VARCHAR(150) NOT NULL,
    level VARCHAR(50),
    credits INT
);

-- Dimension Table: Dim_Department
CREATE TABLE Dim_Department (
    department_key SERIAL PRIMARY KEY, -- Surrogate Key
    department_id INT NOT NULL,        -- Natural Key from operational DB
    department_name VARCHAR(100) NOT NULL,
    dean_name VARCHAR(100)
);

-- Dimension Table: Dim_Lecturer
CREATE TABLE Dim_Lecturer (
    lecturer_key SERIAL PRIMARY KEY, -- Surrogate Key
    lecturer_id INT NOT NULL,        -- Natural Key from operational DB
    full_name VARCHAR(101) NOT NULL,
    email VARCHAR(100)
);

-- Fact Table: Fact_Enrollment
CREATE TABLE Fact_Enrollment (
    enrollment_fact_id BIGSERIAL PRIMARY KEY,
    -- Foreign keys to dimensions
    date_key INT NOT NULL,
    student_key INT NOT NULL,
    course_key INT NOT NULL,
    department_key INT NOT NULL,
    lecturer_key INT NOT NULL,
    -- Measures
    final_grade NUMERIC(5, 2),
    tuition_fee NUMERIC(10, 2),
    enrollment_count INT NOT NULL DEFAULT 1,
    is_passed INT NOT NULL, -- 1 for true, 0 for false
    -- Constraints to link to dimensions
    CONSTRAINT fk_dim_date FOREIGN KEY (date_key) REFERENCES Dim_Date(date_key),
    CONSTRAINT fk_dim_student FOREIGN KEY (student_key) REFERENCES Dim_Student(student_key),
    CONSTRAINT fk_dim_course FOREIGN KEY (course_key) REFERENCES Dim_Course(course_key),
    CONSTRAINT fk_dim_department FOREIGN KEY (department_key) REFERENCES Dim_Department(department_key),
    CONSTRAINT fk_dim_lecturer FOREIGN KEY (lecturer_key) REFERENCES Dim_Lecturer(lecturer_key)
);

-- Index on the fact table's foreign keys is crucial for query performance
CREATE INDEX idx_fact_date_key ON Fact_Enrollment(date_key);
CREATE INDEX idx_fact_student_key ON Fact_Enrollment(student_key);
CREATE INDEX idx_fact_course_key ON Fact_Enrollment(course_key);