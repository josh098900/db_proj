
-- SAMPLE DATA FOR OPERATIONAL DATABASE

--  script inserts sample data into the OLTP tables.


-- 1. Insert into departments
INSERT INTO departments (department_name, dean_name) VALUES
('Computer Science', 'Dr. Alan Turing'),
('Business Administration', 'Dr. Peter Drucker'),
('Fine Arts', 'Dr. Georgia O''Keeffe');

-- 2. Insert into lecturers
-- Note: department_id corresponds to the SERIAL PK from the departments table (1: CS, 2: Business, 3: Arts)
INSERT INTO lecturers (first_name, last_name, email, department_id) VALUES
('Ada', 'Lovelace', 'ada.lovelace@uni.edu', 1),
('Grace', 'Hopper', 'grace.hopper@uni.edu', 1),
('Warren', 'Buffett', 'warren.buffett@uni.edu', 2),
('Indra', 'Nooyi', 'indra.nooyi@uni.edu', 2),
('Leonardo', 'da Vinci', 'leo.davinci@uni.edu', 3);

-- 3. Insert into courses
-- department_id again corresponds to the departments table
INSERT INTO courses (course_code, course_title, credits, level, tuition_fee, department_id) VALUES
('CS101', 'Introduction to Programming', 3, 'Undergraduate', 1500.00, 1),
('CS304', 'Databases and Data Warehousing', 4, 'Undergraduate', 2500.00, 1),
('CS501', 'Advanced Algorithms', 4, 'Postgraduate', 3500.00, 1),
('BA101', 'Principles of Management', 3, 'Undergraduate', 1400.00, 2),
('BA450', 'Investment Strategies', 3, 'Postgraduate', 4000.00, 2),
('FA100', 'History of Art', 3, 'Undergraduate', 1200.00, 3),
('FA220', 'Studio Painting', 5, 'Undergraduate', 2800.00, 3);

-- 4. Insert into students
INSERT INTO students (first_name, last_name, date_of_birth, gender, nationality, email, enrollment_date) VALUES
('John', 'Smith', '2004-08-15', 'Male', 'USA', 'john.smith@email.com', '2022-09-01'),
('Maria', 'Garcia', '2003-05-20', 'Female', 'Spain', 'maria.garcia@email.com', '2022-09-01'),
('Chen', 'Wei', '2005-01-30', 'Male', 'China', 'chen.wei@email.com', '2023-09-01'),
('Fatima', 'Al-Fassi', '2002-11-10', 'Female', 'Morocco', 'fatima.alfassi@email.com', '2021-09-01'),
('David', 'Jones', '2003-03-25', 'Male', 'UK', 'david.jones@email.com', '2022-09-01'),
('Emily', 'White', '2005-07-12', 'Female', 'Canada', 'emily.white@email.com', '2023-09-01');

-- 5. Insert into enrollments
-- This is the most important table for our analytics
-- student_id, course_id, lecturer_id, academic_year, semester, final_grade, status
-- Past enrollments with grades
INSERT INTO enrollments (student_id, course_id, lecturer_id, academic_year, semester, final_grade, status) VALUES
(1, 1, 1, 2022, 1, 85.5, 'Passed'), -- John, CS101
(2, 1, 1, 2022, 1, 92.0, 'Passed'), -- Maria, CS101
(4, 4, 3, 2021, 2, 78.0, 'Passed'), -- Fatima, BA101
(5, 6, 5, 2022, 2, 65.0, 'Passed'), -- David, FA100
(1, 4, 3, 2022, 2, 55.0, 'Passed'), -- John, BA101
(2, 2, 2, 2023, 1, 45.0, 'Failed'), -- Maria, CS304
(4, 2, 2, 2023, 1, 88.0, 'Passed'), -- Fatima, CS304
(5, 7, 5, 2023, 1, 95.0, 'Passed'), -- David, FA220
(3, 1, 1, 2023, 1, 76.0, 'Passed'), -- Chen, CS101
(1, 2, 2, 2023, 1, 68.0, 'Passed'), -- John, CS304
-- Current enrollments, no final grade yet
(3, 4, 3, 2023, 2, NULL, 'Enrolled'), -- Chen, BA101
(6, 1, 1, 2023, 2, NULL, 'Enrolled'), -- Emily, CS101
(6, 6, 5, 2023, 2, NULL, 'Enrolled'); -- Emily, FA100


-- 6. Insert into payments
-- student_id, amount_paid, payment_date, description
INSERT INTO payments (student_id, amount_paid, payment_date, description) VALUES
(1, 1500.00, '2022-09-05 10:00:00+00', 'Semester 1 2022 Fees'),
(1, 1400.00, '2023-01-15 11:00:00+00', 'Semester 2 2022 Fees'),
(2, 1500.00, '2022-09-06 14:00:00+00', 'Semester 1 2022 Fees'),
(2, 2500.00, '2023-01-16 15:00:00+00', 'Semester 1 2023 Fees'),
(3, 1500.00, '2023-09-02 09:00:00+00', 'Semester 1 2023 Fees'),
(4, 1400.00, '2021-09-03 16:00:00+00', 'Semester 2 2021 Fees'),
(5, 1200.00, '2022-09-04 12:00:00+00', 'Semester 2 2022 Fees');




-- Insert Roles (the 'ROLE_' prefix is a Spring Security convention)
INSERT INTO app_role (name) VALUES ('ROLE_VC'), ('ROLE_HOD'), ('ROLE_ADMIN');

-- Insert Users
-- The password for all users is 'password123', hashed with BCrypt.
-- This is the BCrypt hash for "password123": $2a$10$EblZqNptyY2s2u9gJBdG.Xssz2KCkCrK3e96j9R02p9e8Jd2U.8gG
INSERT INTO app_user (username, password) VALUES
('vchancellor', '$2a$10$EblZqNptyY2s2u9gJBdG.Xssz2KCkCrK3e96j9R02p9e8Jd2U.8gG'),
('hod_cs', '$2a$10$EblZqNptyY2s2u9gJBdG.Xssz2KCkCrK3e96j9R02p9e8Jd2U.8gG'),
('admin', '$2a$10$EblZqNptyY2s2u9gJBdG.Xssz2KCkCrK3e96j9R02p9e8Jd2U.8gG');

-- Link Users to Roles
-- user_id and role_id correspond to the SERIAL PKs from above (1, 2, 3)
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1), -- vchancellor gets ROLE_VC
(2, 2), -- hod_cs gets ROLE_HOD
(3, 3); -- admin gets ROLE_ADMIN






-- Add 20 new students
INSERT INTO students (first_name, last_name, date_of_birth, gender, nationality, email, enrollment_date) VALUES
('Liam', 'Wilson', '2004-05-14', 'Male', 'Australia', 'liam.wilson@email.com', '2022-09-01'),
('Olivia', 'Chen', '2003-08-22', 'Female', 'China', 'olivia.chen@email.com', '2022-09-01'),
('Noah', 'Kim', '2005-02-11', 'Male', 'South Korea', 'noah.kim@email.com', '2023-09-01'),
('Emma', 'Müller', '2002-12-03', 'Female', 'Germany', 'emma.muller@email.com', '2021-09-01'),
('Lucas', 'Moreau', '2003-06-28', 'Male', 'France', 'lucas.moreau@email.com', '2022-09-01'),
('Ava', 'Rossi', '2005-10-15', 'Female', 'Italy', 'ava.rossi@email.com', '2023-09-01'),
('William', 'Tanaka', '2004-03-07', 'Male', 'Japan', 'william.tanaka@email.com', '2022-09-01'),
('Sophia', 'Ivanova', '2003-01-25', 'Female', 'Russia', 'sophia.ivanova@email.com', '2021-09-01'),
('James', 'Silva', '2005-07-02', 'Male', 'Brazil', 'james.silva@email.com', '2023-09-01'),
('Isabella', 'O''Connor', '2004-09-19', 'Female', 'Ireland', 'isabella.oconnor@email.com', '2022-09-01'),
('Benjamin', 'Nielsen', '2003-04-09', 'Male', 'Denmark', 'benjamin.nielsen@email.com', '2021-09-01'),
('Mia', 'Johansson', '2005-11-21', 'Female', 'Sweden', 'mia.johansson@email.com', '2023-09-01'),
('Elijah', 'Patel', '2004-01-18', 'Male', 'India', 'elijah.patel@email.com', '2022-09-01'),
('Charlotte', 'Khan', '2003-06-05', 'Female', 'Pakistan', 'charlotte.khan@email.com', '2021-09-01'),
('Henry', 'Nowak', '2005-08-11', 'Male', 'Poland', 'henry.nowak@email.com', '2023-09-01'),
('Amelia', 'Popescu', '2004-10-29', 'Female', 'Romania', 'amelia.popescu@email.com', '2022-09-01'),
('Alexander', 'Horvath', '2003-02-17', 'Male', 'Hungary', 'alexander.horvath@email.com', '2021-09-01'),
('Evelyn', 'Novak', '2005-04-01', 'Female', 'Czech Republic', 'evelyn.novak@email.com', '2023-09-01'),
('Jack', 'Smith', '2004-06-23', 'Male', 'USA', 'jack.smith@email.com', '2022-09-01'),
('Lily', ' Tremblay', '2003-12-30', 'Female', 'Canada', 'lily.tremblay@email.com', '2021-09-01');

-- Add enrollments for the new students (IDs 7 through 26)
INSERT INTO enrollments (student_id, course_id, lecturer_id, academic_year, semester, final_grade, status) VALUES
-- 2022 Enrollments
(7, 1, 1, 2022, 1, 91.0, 'Passed'),
(8, 5, 4, 2022, 1, 85.0, 'Passed'),
(9, 1, 2, 2022, 2, 75.0, 'Passed'),
(10, 4, 4, 2022, 2, 68.0, 'Passed'),
(11, 6, 5, 2022, 1, 82.5, 'Passed'),
(12, 2, 2, 2022, 2, 44.0, 'Failed'),
(13, 7, 5, 2022, 1, 88.0, 'Passed'),
(14, 5, 6, 2022, 2, 79.0, 'Passed'),
(15, 1, 1, 2022, 1, 94.0, 'Passed'),
(16, 2, 2, 2022, 2, 71.0, 'Passed'),
-- 2023 Enrollments
(17, 1, 1, 2023, 1, 89.0, 'Passed'),
(18, 5, 4, 2023, 1, 92.0, 'Passed'),
(19, 2, 2, 2023, 2, NULL, 'Enrolled'),
(20, 6, 5, 2023, 2, NULL, 'Enrolled'),
(21, 4, 4, 2023, 1, 78.0, 'Passed'),
(22, 7, 5, 2023, 1, 95.5, 'Passed'),
(23, 1, 2, 2023, 2, NULL, 'Enrolled'),
(24, 3, 3, 2023, 1, 88.0, 'Passed'),
(25, 5, 6, 2023, 1, 76.0, 'Passed'),
(26, 6, 5, 2023, 2, NULL, 'Enrolled');

-- Add some payments for the new students
INSERT INTO payments (student_id, amount_paid, payment_date, description) VALUES
(7, 1500.00, '2022-09-08 10:00:00+00', 'Semester 1 2022 Fees'),
(8, 1400.00, '2022-09-09 11:00:00+00', 'Semester 1 2022 Fees'),
(17, 1500.00, '2023-09-07 12:00:00+00', 'Semester 1 2023 Fees');