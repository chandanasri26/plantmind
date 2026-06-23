-- MySQL Schema for PlantMind AI - Industrial Knowledge Intelligence Platform
-- Create Database
CREATE DATABASE IF NOT EXISTS plantmind_db;
USE plantmind_db;

-- Create Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    upload_date DATETIME NOT NULL,
    content LONGTEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
