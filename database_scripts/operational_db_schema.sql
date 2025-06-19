
-- OPERATIONAL DATABASE SCHEMA

-- This script creates the tables for the day-to-day
-- transactional system (OLTP).

-- Drop tables if they exist to ensure a clean slate on re-run
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS lecturers;
DROP TABLE IF EXISTS departments;

-- Table: departments
CREATE TABLE departments (
    department_id SERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    dean_name VARCHAR(100)
);

-- Table: lecturers
CREATE TABLE lecturers (
    lecturer_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    department_id INT NOT NULL,
    CONSTRAINT fk_department
        FOREIGN KEY(department_id) 
        REFERENCES departments(department_id)
);

-- Table: courses
CREATE TABLE courses (
    course_id SERIAL PRIMARY KEY,
    course_code VARCHAR(20) NOT NULL UNIQUE,
    course_title VARCHAR(150) NOT NULL,
    credits INT NOT NULL,
    level VARCHAR(50) NOT NULL, -- e.g., 'Undergraduate', 'Postgraduate'
    tuition_fee NUMERIC(10, 2) NOT NULL,
    department_id INT NOT NULL,
    CONSTRAINT fk_department
        FOREIGN KEY(department_id) 
        REFERENCES departments(department_id)
);

-- Table: students
CREATE TABLE students (
    student_id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20),
    nationality VARCHAR(50),
    email VARCHAR(100) NOT NULL UNIQUE,
    enrollment_date DATE NOT NULL
);

-- Table: enrollments
-- This is a junction table connecting students, courses, and lecturers
CREATE TABLE enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    lecturer_id INT NOT NULL,
    academic_year INT NOT NULL,
    semester INT NOT NULL,
    final_grade NUMERIC(5, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'Enrolled', -- e.g., Enrolled, Passed, Failed
    CONSTRAINT fk_student
        FOREIGN KEY(student_id) REFERENCES students(student_id),
    CONSTRAINT fk_course
        FOREIGN KEY(course_id) REFERENCES courses(course_id),
    CONSTRAINT fk_lecturer
        FOREIGN KEY(lecturer_id) REFERENCES lecturers(lecturer_id)
);

-- Table: payments
CREATE TABLE payments (
    payment_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    amount_paid NUMERIC(10, 2) NOT NULL,
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(255),
    CONSTRAINT fk_student
        FOREIGN KEY(student_id) REFERENCES students(student_id)
);

--  indexes for performance on foreign keys
CREATE INDEX idx_lecturer_dept ON lecturers(department_id);
CREATE INDEX idx_course_dept ON courses(department_id);
CREATE INDEX idx_enrollment_student ON enrollments(student_id);
CREATE INDEX idx_enrollment_course ON enrollments(course_id);