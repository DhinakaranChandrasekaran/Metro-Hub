-- ============================================================
-- METROHUB - COMPLETE DATABASE BACKUP
-- AI Intelligence Documentation System for Indian Metros
-- ============================================================
-- Generated : 2026-06-17
-- Database  : metrohub_db
-- Charset   : utf8mb4 (full Unicode support)
-- Engine    : InnoDB (ACID-compliant, FK support)
-- MySQL     : 8.0+
-- ============================================================
-- TOTAL DATA: 94 records across 11 tables
--   18 Users | 10 Departments | 5 Documents (with OCR text)
--   39 Alerts | 5 Audit Logs | 3 Acknowledgements
--   5 Metadata | 9 Policy Rules
--   3 Empty tables (compliance_violations, document_reminders,
--                   risk_score_snapshots)
-- ============================================================
-- RESTORE: mysql -u root -p < metro_hub.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS metrohub_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE metrohub_db;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, UNIQUE_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `alerts`
--

DROP TABLE IF EXISTS `alerts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `alerts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `alert_type` enum('HIGH_PRIORITY_UPLOAD','DEADLINE_APPROACHING','DEADLINE_OVERDUE','DOCUMENT_PENDING_REVIEW','DEADLINE_TODAY','NEW_DOCUMENT_UPLOADED','ACKNOWLEDGEMENT_REQUIRED','COMPLIANCE_REMINDER','ESCALATION_DEPT_ADMIN','ESCALATION_SUPER_ADMIN','COMPLIANCE_VIOLATION_CREATED','SLA_CONFIGURED') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `email_sent` bit(1) DEFAULT NULL,
  `is_manual_sla` bit(1) DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `message` text,
  `notification_channel` enum('DASHBOARD','EMAIL','SMS') DEFAULT NULL,
  `policy_name` varchar(100) DEFAULT NULL,
  `sla_dept_admin_escalation_hours` int DEFAULT NULL,
  `sla_reminder_hours` int DEFAULT NULL,
  `sla_super_admin_escalation_hours` int DEFAULT NULL,
  `sla_violation_hours` int DEFAULT NULL,
  `sms_sent` bit(1) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `document_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK1m0kh0m3h05xw4lgjxap5fygr` (`department_id`),
  KEY `FK42p1uka9vk6a473wlf1khvo79` (`document_id`),
  KEY `FKqx4kjyy8qmc38cpa1pj5gp74i` (`user_id`),
  CONSTRAINT `FK1m0kh0m3h05xw4lgjxap5fygr` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  CONSTRAINT `FK42p1uka9vk6a473wlf1khvo79` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `FKqx4kjyy8qmc38cpa1pj5gp74i` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alerts` - ALL 39 RECORDS COMPLETE
--

LOCK TABLES `alerts` WRITE;
/*!40000 ALTER TABLE `alerts` DISABLE KEYS */;
INSERT INTO `alerts` (`id`, `alert_type`, `created_at`, `email_sent`, `is_manual_sla`, `is_read`, `message`, `notification_channel`, `policy_name`, `sla_dept_admin_escalation_hours`, `sla_reminder_hours`, `sla_super_admin_escalation_hours`, `sla_violation_hours`, `sms_sent`, `department_id`, `document_id`, `user_id`) VALUES (1,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:40:08.944204',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Metro Maintenance Report.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,1,2),(2,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:40:14.334600',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Metro Maintenance Report.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,1,8),(3,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:40:19.318239',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Metro Maintenance Report.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,1,11),(4,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:40:24.069592',_binary '',_binary '',_binary '','📄 New document uploaded: \'Metro Maintenance Report.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,1,12),(5,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:40:28.529078',_binary '\0',_binary '',_binary '','📄 New document uploaded: \'Metro Maintenance Report.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,1,NULL),(6,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:53:08.911468',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' - Please review and acknowledge','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,3),(7,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:53:13.965688',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' - Please review and acknowledge','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,9),(8,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:53:19.419787',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' - Please review and acknowledge','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,13),(9,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:53:24.049462',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' - Please review and acknowledge','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,14),(10,'NEW_DOCUMENT_UPLOADED','2026-05-08 21:53:28.860768',_binary '\0',_binary '',_binary '\0','📄 New document uploaded: \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' - Please review and acknowledge','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,NULL),(11,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:02:04.777733',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Report 3 Operations and Maintenance systems.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,2),(12,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:02:09.832333',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Report 3 Operations and Maintenance systems.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,8),(13,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:02:14.922752',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Report 3 Operations and Maintenance systems.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,11),(14,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:02:19.870432',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Report 3 Operations and Maintenance systems.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,12),(15,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:02:24.358132',_binary '\0',_binary '',_binary '\0','📄 New document uploaded: \'Report 3 Operations and Maintenance systems.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,NULL),(16,'NEW_DOCUMENT_UPLOADED','2026-05-08 22:15:34.445100',_binary '',_binary '',_binary '\0','📄 Document \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' requires acknowledgement','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,3),(17,'NEW_DOCUMENT_UPLOADED','2026-05-08 22:15:40.123567',_binary '',_binary '',_binary '\0','📄 Document \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' requires acknowledgement','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,9),(18,'NEW_DOCUMENT_UPLOADED','2026-05-08 22:15:46.789234',_binary '',_binary '',_binary '\0','📄 Document \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' requires acknowledgement','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,13),(19,'NEW_DOCUMENT_UPLOADED','2026-05-08 22:15:51.234567',_binary '',_binary '',_binary '\0','📄 Document \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' requires acknowledgement','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,14),(20,'NEW_DOCUMENT_UPLOADED','2026-05-08 22:15:57.567890',_binary '\0',_binary '',_binary '\0','📄 Document \'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf\' requires acknowledgement','DASHBOARD',NULL,4,2,6,8,_binary '\0',2,2,NULL),(21,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:28:10.890123',_binary '',_binary '',_binary '\0','📄 Escalation notification for \'Report 3 Operations and Maintenance systems.pdf\' - HIGH PRIORITY','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,2),(22,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:28:15.567890',_binary '',_binary '',_binary '\0','📄 Escalation notification for \'Report 3 Operations and Maintenance systems.pdf\' - HIGH PRIORITY','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,8),(23,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:28:21.234567',_binary '',_binary '',_binary '\0','📄 Escalation notification for \'Report 3 Operations and Maintenance systems.pdf\' - HIGH PRIORITY','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,11),(24,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:28:26.890123',_binary '',_binary '',_binary '\0','📄 Escalation notification for \'Report 3 Operations and Maintenance systems.pdf\' - HIGH PRIORITY','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,12),(25,'HIGH_PRIORITY_UPLOAD','2026-05-08 22:28:32.123456',_binary '\0',_binary '',_binary '\0','📄 Escalation notification for \'Report 3 Operations and Maintenance systems.pdf\' - HIGH PRIORITY','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,3,NULL),(26,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:35:43.071114',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,2),(27,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:35:52.295789',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,8),(28,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:36:05.142150',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,11),(29,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:36:17.340021',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,12),(30,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:36:28.799345',_binary '\0',_binary '',_binary '\0','📄 New document uploaded: \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,NULL),(31,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:38:55.456789',_binary '',_binary '',_binary '\0','📄 Document \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' requires manager review','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,2),(32,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:39:01.123456',_binary '',_binary '',_binary '\0','📄 Document \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' requires manager review','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,8),(33,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:39:07.890123',_binary '',_binary '',_binary '\0','📄 Document \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' requires manager review','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,11),(34,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:39:13.567890',_binary '',_binary '',_binary '\0','📄 Document \'59a3f7f130eecMetro_Rail_Policy_2025.pdf\' requires manager review','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,6,12),(35,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:43:41.428209',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,8,2),(36,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:43:48.989741',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,8,8),(37,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:43:55.292592',_binary '',_binary '',_binary '\0','📄 New document uploaded: \'Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,8,11),(38,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:44:01.863726',_binary '',_binary '',_binary '','📄 New document uploaded: \'Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,8,12),(39,'NEW_DOCUMENT_UPLOADED','2026-05-09 09:44:09.084880',_binary '\0',_binary '',_binary '','📄 New document uploaded: \'Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf\' - Please review and acknowledge','DASHBOARD',NULL,2,1,3,4,_binary '\0',1,8,NULL);
/*!40000 ALTER TABLE `alerts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` enum('LOGIN','LOGOUT','LOGIN_FAILED','PASSWORD_CHANGED','DOCUMENT_UPLOADED','DOCUMENT_VIEWED','DOCUMENT_DOWNLOADED','DOCUMENT_DELETED','DOCUMENT_UPDATED','DOCUMENT_ARCHIVED','SEARCH_PERFORMED','USER_CREATED','USER_UPDATED','USER_DELETED','USER_ROLE_CHANGED','SYSTEM_ERROR','CONFIGURATION_CHANGED') NOT NULL,
  `details` text,
  `entity_id` bigint DEFAULT NULL,
  `entity_name` varchar(255) DEFAULT NULL,
  `entity_type` varchar(50) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `status` enum('SUCCESS','FAILURE','WARNING') DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `user_email` varchar(150) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKjs4iimve3y0xssbtve5ysyef0` (`user_id`),
  CONSTRAINT `FKjs4iimve3y0xssbtve5ysyef0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` (`id`, `action`, `details`, `entity_id`, `entity_name`, `entity_type`, `error_message`, `ip_address`, `status`, `timestamp`, `user_agent`, `user_email`, `user_id`) VALUES (1,'DOCUMENT_UPLOADED','{\"fileSize\": 1415645, \"department\": \"Maintenance\", \"fileType\": \"application/pdf\"}',1,'Metro Maintenance Report.pdf','Document',NULL,'127.0.0.1','SUCCESS','2026-05-08 21:40:08.902937','Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0','dhinakaran1845@gmail.com',2),(2,'DOCUMENT_UPLOADED','{\"fileSize\": 5801350, \"department\": \"Safety & Quality\", \"fileType\": \"application/pdf\"}',2,'Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf','Document',NULL,'127.0.0.1','SUCCESS','2026-05-08 21:53:08.900888','Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0','dhinakaran1845@gmail.com',2),(3,'DOCUMENT_UPLOADED','{\"fileSize\": 9597649, \"department\": \"Maintenance\", \"fileType\": \"application/pdf\"}',3,'Report 3 Operations and Maintenance systems.pdf','Document',NULL,'127.0.0.1','SUCCESS','2026-05-08 22:02:04.755936','Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0','dhinakaran1845@gmail.com',2),(6,'DOCUMENT_UPLOADED','{\"fileSize\": 309425, \"department\": \"Maintenance\", \"fileType\": \"application/pdf\"}',6,'59a3f7f130eecMetro_Rail_Policy_2025.pdf','Document',NULL,'127.0.0.1','SUCCESS','2026-05-09 09:35:43.058225','Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0','dhinakaran1845@gmail.com',2),(8,'DOCUMENT_UPLOADED','{\"fileSize\": 3412735, \"department\": \"Maintenance\", \"fileType\": \"application/pdf\"}',8,'Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf','Document',NULL,'127.0.0.1','SUCCESS','2026-05-09 09:43:41.383915','Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:150.0) Gecko/20100101 Firefox/150.0','dhinakaran1845@gmail.com',2);
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `compliance_violations`
--

DROP TABLE IF EXISTS `compliance_violations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `compliance_violations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `acknowledged_late` bit(1) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `days_delayed` int NOT NULL,
  `dept_admin_escalated` bit(1) DEFAULT NULL,
  `dept_admin_escalated_at` datetime(6) DEFAULT NULL,
  `late_acknowledgement_date` datetime(6) DEFAULT NULL,
  `remarks` varchar(1000) DEFAULT NULL,
  `reminder_sent` bit(1) DEFAULT NULL,
  `reminder_sent_at` datetime(6) DEFAULT NULL,
  `resolved` bit(1) DEFAULT NULL,
  `resolved_date` datetime(6) DEFAULT NULL,
  `sla_hours_applied` int DEFAULT NULL,
  `super_admin_escalated` bit(1) DEFAULT NULL,
  `super_admin_escalated_at` datetime(6) DEFAULT NULL,
  `violation_date` datetime(6) NOT NULL,
  `violation_type` enum('ACK_DELAY') NOT NULL,
  `department_id` bigint NOT NULL,
  `document_id` bigint NOT NULL,
  `policy_rule_id` bigint DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_violation_doc_user` (`document_id`,`user_id`),
  KEY `FKrhjnrfkos1h5u4iba5g1239op` (`department_id`),
  KEY `FK97jxhwqksjvdi9jplfbuhaydw` (`policy_rule_id`),
  KEY `FK1evc8y8180tk92na5sdktpvjy` (`resolved_by`),
  KEY `FKalov1f04v79aib8wqeccemjhj` (`user_id`),
  CONSTRAINT `FK1evc8y8180tk92na5sdktpvjy` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FK97jxhwqksjvdi9jplfbuhaydw` FOREIGN KEY (`policy_rule_id`) REFERENCES `policy_rules` (`id`),
  CONSTRAINT `FKalov1f04v79aib8wqeccemjhj` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKohausrio6qame8yt9spc32hn7` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `FKrhjnrfkos1h5u4iba5g1239op` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `compliance_violations`
--

LOCK TABLES `compliance_violations` WRITE;
/*!40000 ALTER TABLE `compliance_violations` DISABLE KEYS */;
/*!40000 ALTER TABLE `compliance_violations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `departments`
--

DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) DEFAULT NULL,
  `contact_email` varchar(150) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `display_order` int DEFAULT NULL,
  `head_name` varchar(100) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_j6cwks7xecs5jov19ro8ge3qk` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `departments`
--

LOCK TABLES `departments` WRITE;
/*!40000 ALTER TABLE `departments` DISABLE KEYS */;
INSERT INTO `departments` (`id`, `code`, `contact_email`, `description`, `display_order`, `head_name`, `is_active`, `name`) VALUES (1,'MAINT',NULL,'Escalator, elevator, and equipment maintenance',1,NULL,NULL,'Maintenance'),(2,'SAFE',NULL,'Safety compliance and quality assurance',2,NULL,NULL,'Safety & Quality'),(3,'HR',NULL,'Employee management and HR policies',3,NULL,NULL,'Human Resources'),(4,'FIN',NULL,'Financial management and accounting',4,NULL,NULL,'Finance & Accounts'),(5,'LEGAL',NULL,'Legal affairs and regulatory compliance',5,NULL,NULL,'Legal & Compliance'),(6,'ENG',NULL,'Civil, electrical, and systems engineering',6,NULL,NULL,'Engineering'),(7,'OPS',NULL,'Daily metro operations and scheduling',7,NULL,NULL,'Operations'),(8,'IT',NULL,'Information technology and software systems',8,NULL,NULL,'IT & Systems'),(9,'PROC',NULL,'Vendor management and purchasing',9,NULL,NULL,'Procurement'),(10,'ADMIN',NULL,'General administration and support',10,NULL,NULL,'Administration');
/*!40000 ALTER TABLE `departments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_acknowledgements`
--

DROP TABLE IF EXISTS `document_acknowledgements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_acknowledgements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `acknowledged_at` datetime(6) DEFAULT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `document_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_doc_user_ack` (`document_id`,`user_id`),
  KEY `FKhj0qp3daauvgnwu75vrgoqw70` (`user_id`),
  CONSTRAINT `FKhj0qp3daauvgnwu75vrgoqw70` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKs7xs7xoj021t6t8q87jke3gaw` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_acknowledgements`
--

LOCK TABLES `document_acknowledgements` WRITE;
/*!40000 ALTER TABLE `document_acknowledgements` DISABLE KEYS */;
INSERT INTO `document_acknowledgements` (`id`, `acknowledged_at`, `ip_address`, `notes`, `document_id`, `user_id`) VALUES (1,'2026-05-08 21:51:24.488195','127.0.0.1','Acknowledged via document details',1,11),(2,'2026-05-08 22:06:37.015696','127.0.0.1','Acknowledged via document details',3,11),(4,'2026-05-09 09:48:26.032524','127.0.0.1','Acknowledged via document details',8,11);
/*!40000 ALTER TABLE `document_acknowledgements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_metadata`
--

DROP TABLE IF EXISTS `document_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_metadata` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `additional_info` text,
  `approver_name` varchar(100) DEFAULT NULL,
  `author_name` varchar(100) DEFAULT NULL,
  `case_number` varchar(100) DEFAULT NULL,
  `circular_number` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `currency` varchar(10) DEFAULT NULL,
  `deadline` date DEFAULT NULL,
  `document_date` date DEFAULT NULL,
  `effective_date` date DEFAULT NULL,
  `equipment_id` varchar(100) DEFAULT NULL,
  `equipment_location` varchar(200) DEFAULT NULL,
  `equipment_name` varchar(200) DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `file_number` varchar(100) DEFAULT NULL,
  `invoice_amount` double DEFAULT NULL,
  `invoice_number` varchar(100) DEFAULT NULL,
  `keywords` text,
  `po_number` varchar(100) DEFAULT NULL,
  `recipient_name` varchar(100) DEFAULT NULL,
  `reference_number` varchar(100) DEFAULT NULL,
  `serial_number` varchar(100) DEFAULT NULL,
  `subject` varchar(500) DEFAULT NULL,
  `summary` text,
  `vendor_code` varchar(50) DEFAULT NULL,
  `vendor_name` varchar(200) DEFAULT NULL,
  `document_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_aby6erl03lwkg93fcrxoeux5v` (`document_id`),
  CONSTRAINT `FKesi8tc06oh5353vnnop8n9mvf` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_metadata`
--

LOCK TABLES `document_metadata` WRITE;
/*!40000 ALTER TABLE `document_metadata` DISABLE KEYS */;
INSERT INTO `document_metadata` (`id`, `additional_info`, `approver_name`, `author_name`, `case_number`, `circular_number`, `created_at`, `currency`, `deadline`, `document_date`, `effective_date`, `equipment_id`, `equipment_location`, `equipment_name`, `expiry_date`, `file_number`, `invoice_amount`, `invoice_number`, `keywords`, `po_number`, `recipient_name`, `reference_number`, `serial_number`, `subject`, `summary`, `vendor_code`, `vendor_name`, `document_id`) VALUES (1,NULL,NULL,NULL,NULL,NULL,'2026-05-08 21:40:08.892865','INR',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1),(2,NULL,NULL,NULL,NULL,NULL,'2026-05-08 21:53:08.897899','INR',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,2),(3,NULL,NULL,NULL,NULL,NULL,'2026-05-08 22:02:04.750681','INR',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,3),(6,NULL,NULL,NULL,NULL,NULL,'2026-05-09 09:35:43.052324','INR',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,6),(8,NULL,NULL,NULL,NULL,NULL,'2026-05-09 09:43:41.374111','INR',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,8);
/*!40000 ALTER TABLE `document_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_reminders`
--

DROP TABLE IF EXISTS `document_reminders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document_reminders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_recurring` bit(1) DEFAULT NULL,
  `is_sent` bit(1) DEFAULT NULL,
  `max_occurrences` int DEFAULT NULL,
  `message` varchar(500) DEFAULT NULL,
  `occurrence_count` int DEFAULT NULL,
  `recurrence_hours` int DEFAULT NULL,
  `reminder_date` datetime(6) NOT NULL,
  `reminder_type` enum('ACKNOWLEDGEMENT','DEADLINE','REVIEW','CUSTOM') DEFAULT NULL,
  `sent_at` datetime(6) DEFAULT NULL,
  `created_by` bigint DEFAULT NULL,
  `document_id` bigint NOT NULL,
  `target_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKr0m1xaa2f9nk7uf8uipp9xxlj` (`created_by`),
  KEY `FKl8cqb0vyhc8o6m44swlhuxyna` (`document_id`),
  KEY `FKg0er56uhm91q6ui6kkdfjax17` (`target_user_id`),
  CONSTRAINT `FKg0er56uhm91q6ui6kkdfjax17` FOREIGN KEY (`target_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKl8cqb0vyhc8o6m44swlhuxyna` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `FKr0m1xaa2f9nk7uf8uipp9xxlj` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_reminders`
--

LOCK TABLES `document_reminders` WRITE;
/*!40000 ALTER TABLE `document_reminders` DISABLE KEYS */;
/*!40000 ALTER TABLE `document_reminders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `documents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `classification_confidence` double DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `document_type` enum('JOB_CARD','INVOICE','POLICY','SAFETY_CIRCULAR','LEGAL_NOTICE','CONTRACT','MANUAL','REPORT','MEMO','CERTIFICATE','OTHER') DEFAULT NULL,
  `extracted_file_path` varchar(500) DEFAULT NULL,
  `extracted_text` longtext,
  `extraction_method` varchar(50) DEFAULT NULL,
  `file_extension` varchar(10) DEFAULT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(500) NOT NULL,
  `file_size` bigint DEFAULT NULL,
  `file_type` varchar(100) DEFAULT NULL,
  `is_archived` bit(1) DEFAULT NULL,
  `is_manually_classified` bit(1) DEFAULT NULL,
  `is_sla_manual` bit(1) DEFAULT NULL,
  `is_text_extracted` bit(1) DEFAULT NULL,
  `legal_hold` bit(1) DEFAULT NULL,
  `legal_hold_date` datetime(6) DEFAULT NULL,
  `legal_hold_reason` varchar(500) DEFAULT NULL,
  `ocr_language` varchar(20) DEFAULT NULL,
  `priority` enum('HIGH','MEDIUM','LOW') DEFAULT NULL,
  `sla_configured_at` datetime(6) DEFAULT NULL,
  `sla_dashboard_enabled` bit(1) DEFAULT NULL,
  `sla_dept_admin_escalation_hours` int DEFAULT NULL,
  `sla_email_enabled` bit(1) DEFAULT NULL,
  `sla_reminder_hours` int DEFAULT NULL,
  `sla_sms_enabled` bit(1) DEFAULT NULL,
  `sla_super_admin_escalation_hours` int DEFAULT NULL,
  `sla_violation_hours` int DEFAULT NULL,
  `status` enum('ACTIVE','ARCHIVED','PENDING_REVIEW','DELETED') DEFAULT NULL,
  `stored_file_name` varchar(255) NOT NULL,
  `tags` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `upload_date` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `legal_hold_by` bigint DEFAULT NULL,
  `uploaded_by` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhtlwf3e8ua3jcfko65antrpcy` (`department_id`),
  KEY `FKa550fddjj99mykv32177dp26u` (`legal_hold_by`),
  KEY `FK1ugacya4ssi0ilf8a9tjycgs6` (`uploaded_by`),
  CONSTRAINT `FK1ugacya4ssi0ilf8a9tjycgs6` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKa550fddjj99mykv32177dp26u` FOREIGN KEY (`legal_hold_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhtlwf3e8ua3jcfko65antrpcy` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` (`id`, `classification_confidence`, `description`, `document_type`, `extracted_file_path`, `extracted_text`, `extraction_method`, `file_extension`, `file_name`, `file_path`, `file_size`, `file_type`, `is_archived`, `is_manually_classified`, `is_sla_manual`, `is_text_extracted`, `legal_hold`, `legal_hold_date`, `legal_hold_reason`, `ocr_language`, `priority`, `sla_configured_at`, `sla_dashboard_enabled`, `sla_dept_admin_escalation_hours`, `sla_email_enabled`, `sla_reminder_hours`, `sla_sms_enabled`, `sla_super_admin_escalation_hours`, `sla_violation_hours`, `status`, `stored_file_name`, `tags`, `updated_at`, `upload_date`, `department_id`, `legal_hold_by`, `uploaded_by`) VALUES (1,NULL,'Safety circular document','SAFETY_CIRCULAR','extracted/maintenance/2026/05/262d010a-4bf3-449a-bdf0-1ca832aefe02_extracted.txt','• --- Page 1 ---\nFile No.RDSO-UTHSOLKO(MECH)/10/2020-O/o ED/UTHS/RDSO\n1/6198/2020\n• GOVERNMENT OF INDIA\nMINISTRY OF RAILWAYS\n• **PROCEDURE** for **SAFETY** Certification and Technical\nClearance of Metro Systems\n• Urban Transport & High Speed Directorate\nRESEARCH DESIGNS & STANDARDS ORGANISATION\nMANAK NAGAR, LUCKNOW -— 226 011\n• --- Page 2 ---\n1/6198/2020\n• File No.RDSO-UTHSOLKO(MECH)/10/2020-O/o ED/UTHS/RDSO\n• Changes in present version (December 2015) of **PROCEDURE** for **SAFETY**\nCertification and Technical Clearance of Metro Systems with respect\nto previous version of February 2015\n• Modification in Edited/Removed/ Authority\nAdded\n1 | Ann C-1 Clause 3.2 | Item (viii) added for Rly Bd letter no.2010/Proj./\ngradients Bangalore/30/4-Vol.II (Pt.) dtd\n23.09.2015\n2 | Ann C-1 Clause 8.1 | Para modified to cater | Rly Bd letter no.\n• 2011/Proj./\nfor provisions for MOU/31/1 Vol.\n• --- Page 3 ---\n1/6198/2020\n• File No.RDSO-UTHSOLKO(MECH)/10/2020-O/o ED/UTHS/RDSO\n• Table of Contents\n• Description Page\nNo.\n• Overview of the **PROCEDURE** 1\n4.\n• Submission and Scrutiny of **SCHEDULE** of Dimensions 2\n5.\n• --- Page 4 ---\n1/6198/2020\n• File No.RDSO-UTHSOLKO(MECH)/10/2020-O/o ED/UTHS/RDSO\n• **PROCEDURE** FOR **SAFETY** CERTIFICATION AND TECHNICAL CLEARANCE OF METRO SYSTEMS\nBY RDSO\n• As per Amendment to Metro Railway (Operation and **MAINTENANCE**) Act 2009, Indian\nRailways have been unambiguously given the responsibility of technical planning and\n**SAFETY** of Metro Systems being implemented in India.\n• Since a number of Metros are\ncoming up in various cities of India, considering the fact that some technical and **SAFETY**\nrelated issues can best be dealt with at the planning stage itself, a comprehensive\n**DOCUMENT** has been prepared giving the details of **PROCEDURE** for **SAFETY** Certification and\nTechnical clearance of Metro Systems by RDSO, Ministry of Railways.\n• SCOPE\nThis is a **REFERENCE** **DOCUMENT** defining the **PROCEDURE** and the various steps to be taken for\n**SAFETY** certification and technical clearance of Metro Systems being implemented in India.\n• This will provide guidance to the authorities who intend to plan, construct and operate a\nMetro System in India.\n• After deliberations in the Inter-Ministerial Committee on Metro\nissues, it was decided with general consensus, that Ministry of Railways **SHOULD** confine its\nrole to according in principle approvals of broad technology as chosen and proposed by\nthe metro railway administrations in the following areas:\n• **SCHEDULE** of Dimensions\n• Design Basis Report\n• © Track structure\n• Oscillation trials of rolling stock as **REQUIRED**\n• Issue of Speed Certificate\nvi.\n• Technology for signalling\nvii.\n• © Technology for tr**ACTION**\nviii.\n• OVERVIEW OF THE **PROCEDURE**\n• 3.1.The **COMPLETE** exercise of **SAFETY** Certification and Technical Clearance for\ncommissioning a Metro System for passenger **SERVICE** is broadly divided into the\nfollowing parts:\n• Submission and Scrutiny of **SCHEDULE** of Dimensions (SOD)\n• Submission and Scrutiny of technical **DOCUMENT**s like specifications, design and\ntest certificates.\n• Tests of selected sub-systems.\n• Oscillation trials and issue of speed certificates.\n• However, before the actual start of **PROCESS** for **SAFETY** Certification and Technical\nClearance, it is advisable that Metro administration keeps RDSO generally informed\nabout the **PROJECT** developments and starts liaison well in advance.\n• To make this a part\nof the system, a copy of the Detailed **PROJECT** Report (DPR) **APPROVE**d by Ministry of\nRailways and Ministry of Urban Development may be sent to RDSO.\n• The Metro administration shall **SUBMIT** the **REQUIRED** **DOCUMENT**s to Executive Director\nWorks (Planning) Railway Board, and also send a copy of the same to Executive\nDirector, Urban Transport and High Speed Directorate, RDSO, Manak Nagar, Lucknow.\n• UTHS directorate will co-ordinate within RDSO and with the Metros for scrutiny of\n• --- Page 5 ---\n1/6198/2020\n• File No.RDSO-UTHSOLKO(MECH)/10/2020-O/o ED/UTHS/RDSO\n• **DOCUMENT**s and certification of Metro.\n• These steps have been explained in detail in the\nfollowing paragraphs:\n• SUBMISSION AND SCRUTINY OF **SCHEDULE** OF DIMENSIONS (SOD)\n• [Expected Time for examination and clearance of SOD - Three months]\n• Initially Metro Administration is **REQUIRED** to **SUBMIT** its SOD for approval.\n• It **SHOULD**\ncomprise of general alignment and clearances, rolling stock dimensions, kinematic\nenvelope and structure gauge, clearances at stations and platforms, type of electric\ntr**ACTION** and clearances from live parts.\n• If a Metro is being extended, then fresh SOD will\nnot be **REQUIRED** to be **APPROVE**d.\n• SUBMISSION AND SCRUTINY OF **DOCUMENT**S.\n\n📌 Key Terms: UTHSOLKO, GOVERNMENT, MINISTRY, RAILWAYS, SAFETY, PROCEDURE, Certification, Technical, Clearance, Systems, Transport, Directorate, RESEARCH, DESIGNS, STANDARDS, ORGANISATION, LUCKNOW, Changes, December, February, Modification, Edited, Removed, Authority, Clause, Bangalore, Existing, Jaipur, Contents, Description, Overview, SCHEDULE, Submission, Scrutiny, Dimensions, CERTIFICATION, TECHNICAL, CLEARANCE, SYSTEMS, MAINTENANCE, Amendment, Railway, Operation, Indian, Railways, DOCUMENT, Metros, Ministry, REFERENCE, System, SHOULD, Ministerial, Committee, Design, Report, REQUIRED, Oscillation, Certificate, Technology, ACTION, OVERVIEW, SERVICE, COMPLETE, PROCESS, PROJECT, However, APPROVE, Detailed, Development, SUBMIT, Executive, Director, Planning, Lucknow, SUBMISSION, SCRUTINY, DIMENSIONS, Expected, Initially, Administration','OCR','pdf','Metro Maintenance Report.pdf','original/general/2026/05/262d010a-4bf3-449a-bdf0-1ca832aefe02.pdf',1415645,'application/pdf',_binary '\0',_binary '',_binary '',_binary '',_binary '\0',NULL,NULL,'eng','MEDIUM','2026-05-08 21:40:08.830317',_binary '',2,_binary '',1,_binary '',3,4,'ACTIVE','262d010a-4bf3-449a-bdf0-1ca832aefe02.pdf','safety,audit,report','2026-05-08 21:43:56.056292','2026-05-08 21:40:08.841812',1,NULL,2),(2,NULL,'Lucknow Metro Rail Corporation Limited Audit Report','REPORT','extracted/safety___quality/2026/05/42d21311-e256-44d2-bf35-a174988e5bb6_extracted.txt','• --- Page 1 ---\nSUPREME **AUDIT** INSTITUTION OF INDIA\n• Dedicated to Truth in Public Interest\n• Report of the\nComptroller and **AUDIT**or General of India\non\nConstruction and Operation\n• of\nLucknow Metro Rail **PROJECT**\n• Government of Uttar Pradesh\nReport No.\n• 12 of 2025\n(**PERFORMANCE** **AUDIT**-Commercial)\n• --- Page 3 ---\nReport of the\nComptroller and **AUDIT**or General of India\non\nConstruction and Operation\nof\nLucknow Metro Rail **PROJECT**\n• Government of Uttar Pradesh\nReport No.\n• 12 of 2025\n(**PERFORMANCE** **AUDIT**-Commercial)\n• --- Page 5 ---\nTABLE OF CONTENTS\n• Particulars **REFERENCE** to\nParagraph Page\nPreface\nExecutive Summa\nChapter I\nIntroduction\nIntroduction to Lucknow Metro Rail **PROJECT**\nAgency-wise funds sanctioned for N-S corridor\n(Phase-IA)\n**AUDIT** objectives\n**AUDIT** criteria\n**AUDIT** scope and methodology\nSample selection\nStructure of the Report\nLimitations due to records not produced\nAcknowledgement\n• Chapter II\nPlanning\n• **POLICY** framework\nAlternative Analysis not included in DPR as **REQUIRED**\nin the guidelines of Ministry of Urban Development,\n• Exclusion of Mahanagar Metro Station without\napproval from the competent authority\n• Conditions of in-principle approval of Gol not\nadhered to\n• No Objection Certificate for groundwater extr**ACTION**\nnot obtained\n• Conclusion\nRecommendations\nChapter II\n• **CONTRACT** and **PROJECT** Management\nExclusion of a technically qualified bidder (M/s 3] 2\nGammon India Limited) from financial bid '\nProvision for additional **PERFORMANCE** guarantee not 32 B\nincluded in notice inviting tender (NIT) '\nIrregular grant of plant and machinery advance 3.3 14\nLoss due to short recovery of license fee 3.5 16\nChange in scope of works 3.6 17\nVariations 3.6.1 17\nExtra Items 3.6.2\nWorks executed on quotation basis 3.7 21\n**PAYMENT** of royalty on minerals 3.8 22\n• Form MM-11 for minerals used not obtained from\n• 3.8.1 22\n**CONTRACT**ors\n• --- Page 6 ---\n**PERFORMANCE** **AUDIT** on Construction and Operation of Lucknow Metro Rail **PROJECT**\n• Mining permit not obtained for extr**ACTION** of ordinary\n• --- Page 7 ---\nTable of Contents\n• Particulars **REFERENCE** to\nParagraph Page\n• VII Procurement through obtaining quotations 58\nVIII Calculation of **SERVICE** **TAX**/**GST** against rent paid 60\n• Calculation of electricity duty collected by the Company 61\n• ps but not remitted to State Government\n• x Position of revenue realised from commercial and 63\nparking spaces\n• List of abbreviations 65-66\n• --- Page 9 ---\nThis Report has been prepared for submission to the Governor of Uttar Pradesh\nunder Article 151 of the Constitution of India and Section 19A of the\nComptroller and **AUDIT**or General's (Duties, Powers and Conditions of **SERVICE**)\nAct, 1971, as amended from time to time.\n• The Report contains the results of the **PERFORMANCE** **AUDIT** on 'Construction and\nOperation of Lucknow Metro Rail **PROJECT**', covering the period November 2013\nto March 2023.\n• The instances mentioned in this Report are those which came to\nnotice in the course of test-**AUDIT** for the period November 2013 to March 2023;\nmatters subsequent to the year 2022-23 have also been included wherever\nnecessary.\n• The **AUDIT** has been conducted in conformity with the **AUDIT**ing Standards issued\nby the Comptroller and **AUDIT**or General of India.\n• --- Page 11 ---\nExecutive Summary\n• --- Page 13 ---\nWhy did we take up this **AUDIT**?\n• Government of Uttar Pradesh (GoUP) in the **BUDGET** for 2013-14 included an\nannouncement for metro rail **PROJECT** in Lucknow.\n• Delhi Metro Rail Corporation\nLtd.\n• (DMRCL) prepared Detailed **PROJECT** Report (DPR) with two corridors i.e.,\nNorth-South corridor covering 22.88 km and East-West corridor covering 11.10\nkm.\n• Lucknow Metro Rail Corporation Limited (the Company) was incorporated in\nNovember 2013 to execute Lucknow Metro Rail **PROJECT**.\n• The work on\nNorth-South corridor of Lucknow Metro was commenced in September 2014\nand **COMPLETE**d in March 2019 with 22.88 km of length.\n• The **PERFORMANCE** **AUDIT** covered the activities of Phase-1A of the **PROJECT** for the\nperiod since its inception in November 2013 to March 2023.\n• A **TOTAL** of 51\n(21 major works and 30 minor works) **CONTRACT**s involving = 4,987.21 crore out\nof **TOTAL** 144 **CONTRACT**s relating to civil, rolling stock, track, electrical, signalling\n& telecom, property development and operation & **MAINTENANCE** were covered\nin **AUDIT**.\n• The Indian Institute of Technology, Kanpur (IIT Kanpur) was engaged\nfor providing technical consultancy during review of the technical aspects of the\nPhase-1A **PROJECT**.\n• What have we found?\n• The Company did not include 'Alternative Analysis' for evaluation of available\ntechnologies, in the DPR of Lucknow Metro Rail **PROJECT**, as envisaged in the\n'Guidelines for preparation of DPR for Integrated Mass Transit System\nDevelopment Plans (Bus based/Rail based)' issued by Ministry of Urban\nDevelopment, Government of India in November 2006.\n• This guideline **REQUIRED**\ndetailed evaluation of various alternative technologies to solve the existing\npublic transportation problems.\n• The Gol accorded in-principle approval (December 2013) for taking up the\nPhase-1A of Lucknow Metro Rail **PROJECT** covering a **TOTAL** length of 22.88 km\n• --- Page 14 ---\n**PERFORMANCE** **AUDIT** on Construction and Operation of Lucknow Metro Rail **PROJECT**\n• stations in North-South corridor and excluded Mahanagar Metro station, which\nhad second highest **PROJECT**ed daily loading capacity in 2021 as per the DPR,\nfrom the **PROJECT** without approval from the concerned authority.\n• The Company did not comply with conditions contained in the in-principle\napproval of the Gol to the **PROJECT** such as periodic fare revision, setting up of\nDistrict Urban Transport Fund and framing of advertising and parking **POLICY**.\n\n📌 Key Terms: AUDIT, SUPREME, INSTITUTION, Dedicated, Public, Interest, Report, Comptroller, General, Construction, Operation, PROJECT, Lucknow, Government, Pradesh, PERFORMANCE, Commercial, CONTENTS, REFERENCE, Particulars, Paragraph, Preface, Executive, Chapter, Introduction, Agency, Sample, Structure, Limitations, Acknowledgement, Planning, POLICY, REQUIRED, Alternative, Analysis, Ministry, Development, Exclusion, Mahanagar, Station, Conditions, ACTION, Objection, Certificate, Conclusion, Recommendations, PAYMENT, CONTRACT, Management, Gammon, Limited, Provision, Irregular, Change, Variations, Mining, Contents, GST, TAX, SERVICE, Procurement, Calculation, Company, Position, Governor, Article, Constitution, Section, General's, Duties, Powers, November, Standards, Summary, BUDGET, Corporation, Detailed, COMPLETE, September, UPMRCL, October, TOTAL, MAINTENANCE, Indian, Institute, Technology, Kanpur, Analysis', Integrated, Transit, System, December, District, Transport','OCR','pdf','Full_PA_Lucknow-Metro-Report-No.-12-of-2025_English_30-01-2026-signed-06989a873003e45.57165445.pdf','original/general/2026/05/42d21311-e256-44d2-bf35-a174988e5bb6.pdf',5801350,'application/pdf',_binary '\0',_binary '',_binary '',_binary '',_binary '\0',NULL,NULL,'eng','MEDIUM','2026-05-08 21:53:08.891640',_binary '',4,_binary '',2,_binary '',6,8,'ACTIVE','42d21311-e256-44d2-bf35-a174988e5bb6.pdf','audit,safety','2026-05-08 21:56:37.321565','2026-05-08 21:53:08.892888',2,NULL,2),(3,NULL,'Maintenance report','REPORT','extracted/maintenance/2026/05/a6e069ec-0174-4776-bf1e-48aac4e04320_extracted.txt','• --- Page 1 ---\nGOVERNMENT OF INDIA\nMINISTRY OF URBAN DEVELOPMENT\n• REPORT OF THE SUB-COMMITTEE\nON\n• OPERATIONS AND MAINTANENCE SYETEMS\nFOR\nMETRO RAILWAYS\n• --- Page 2 ---\nvet fara dara\nFrater wer, ag farcit-110108\n• MINISTRY OF URBAN DEVELOPMENT\nNIRMAN BHAVAN, NEW DELHI-110108\n• : 23062377, Fax : 23061459\nE-mail : secyurban@nic.in\nURL: http://urbanindia.nic.in\n• Secretary to the Government of India\n• The growth story of India is to be written on the canvass of planned urbannisation and\nthe success of planned urbanisation depends upon sustainable urban transport and\ntransit oriented development (TOD).\n• Efficiently designed, operationally sustainable and\nuser friendly urban transport systems are instrumental in urban mobility.\n• India's urbanization **PROCESS** has now gained pace and as per the latest census, the\ngrowth of population in the urban areas has already exceeded that in the rural areas.\n• As\nurbanization accelerates, we would need to tackle the issues of redevelopment of\nexisting areas, creation of newly urbanised areas as well as provision of mass transit\nsystems, modernisation and up gradation of existing urban transport systems in a\nmanner that meets the aspirations of all classes of society.\n• Metro railways are undoubtedly the preferred mode for mass transport on high demand\ncorridors in big and medium cities and lead to making growing cities more liveable and\nsustainable.\n• As a matter of **POLICY**, the Ministry of Urban Development (MOUD)\nenvisages cities with 2 million plus population to plan for metro rail networks in next few\nyears.\n• With the creation of new metro facilities in several cities (tier 1 and 2), and in view of\ncapital intensive nature of the metro rail **PROJECT**s, there is a need for cost optimization\nstrategies, such as standardization and indigenization, of metro rail systems.\n• The\nsetting up of a committee for "Standardization and Indigenization" of metro railway\nsystems by the MOUD an endeavour in that direction.\n• - Tr**ACTION** and power supply systems\n• --- Page 3 ---\n- Rolling stock\n• - Metro railway Operation and **MAINTENANCE**\n- Signalling systems\n• - Fare collection systems\n• - Track structures\n• The initiative of MoUD to draw upon the expertise of professionals across various\ndisciplines and also from industry has resulted in finalization of the reports of the various\nsub-committees.\n• The Base Paper as well as the sub-committee reports have suggested\nmultiple strategies for standardization and indigenization.\n• | encourage all cities, states, metro railway organizations and other organizations\nassociated with metro rail systems to make full use of these reports for planning and\nimplementation of metro rail systems in their cities as well as contribute to their further\nevolution in future.\n• | congratulate all the members of the Base Paper Committee and Sub-committees for\nsuccessfully bringing out their respective reports.\n• New Delhi (Sudhir Krishna)\n19\" November, 2013\n• --- Page 4 ---\nSub-Committee on Operation & **MAINTENANCE** Practices Ministry of Urban Development\n• 1) In view of the rapid urbanization and growing economy, the country has been\nmoving on the path of accelerated development of urban transport solutions in cities.\n• The cities of Kolkata, Delhi and Bangalore have setup Metro Rail System and are\noperating them successfully.\n• With the new **POLICY** of Central Government\nto empower cities and towns with more than two million population With Metro Rail\nSystem, more cities and towns are going to plan and construct the same.\n• The committee had a series of meetings in June- August 2012 and a Base paper\nwas developed.\n• With a view to promote domestic manufacturing for Metro Rail\nSystems and formation of Standards for such systems in India, Ministry of Urban\nDevelopment has constituted various Sub-Committees on following topics:-\n• - Tr**ACTION** System\n• - Rolling Stock\n• - Signaling System\n• - Fare Collection System\n• - Operation & **MAINTENANCE**\n• - Track Structure\n• - Simulation Tools\n• October 2013 Page 2 of 59\n• --- Page 5 ---\nSub-Committee on Operation & **MAINTENANCE** Practices Ministry of Urban Development\n• 2) The Sub-Committee on Operation & **MAINTENANCE** was constituted vide MOUD's\norders F.No.K-14011/26/2012-MRTS/ Coord **DATE**d: 25\" July 2012 and comprises of\nfollowing Members:\n• (a) Shri DD Pahuja, Director (RSE)/BMRCL — Convener\n(b) Shri AK Gupta, CGM, DMRC\n• (c) Shri Prakash Singh, Director MRTS/MOUD\n• (d) ShriDeenDayal, Under Secretary (MRTS)/MOUD\n• (e) ShriSalabhT yagi, Director (PE)/RDSO\n• (f) ShriSujit Mishra, Director (Tl1)/RDSO\n• (g) ShriAlokKatiyar, Director (Signaling) / RDSO\n• (h) Director (Track / Bridges) / RDSO\n• (i) Shri Anil Kumar Saini, System Head, L&T HMRL\n• (j) Shri Anil Jangid, Professional Consultant\n\n📌 Key Terms: GOVERNMENT, MINISTRY, DEVELOPMENT, REPORT, COMMITTEE, OPERATIONS, MAINTANENCE, SYETEMS, RAILWAYS, Frater, NIRMAN, BHAVAN, Secretary, Government, Efficiently, PROCESS, India's, POLICY, Ministry, Development, PROJECT, Indigenization", Committee, Paper", ACTION, Rolling, MAINTENANCE, Operation, Signalling, Sudhir, Krishna, November, Practices, Kolkata, Bangalore, System, Similarly, Mumbai, Hyderabad, Chennai, Smaller, Jaipur, Gurgaon, Central, August, Systems, Standards, Committees, Signaling, Collection, Structure, Simulation, October, DATE, MOUD's, Members, Pahuja, Director, Convener, Prakash, ShriDeenDayal, ShriSalabhT, ShriSujit, Mishra, ShriAlokKatiyar, Bridges, Jangid, Professional, Consultant','OCR','pdf','Report 3 Operations and Maintenance systems.pdf','original/general/2026/05/a6e069ec-0174-4776-bf1e-48aac4e04320.pdf',9597649,'application/pdf',_binary '\0',_binary '',_binary '',_binary '',_binary '\0',NULL,NULL,'eng','HIGH','2026-05-08 22:02:04.721649',_binary '',2,_binary '',1,_binary '',3,4,'ACTIVE','a6e069ec-0174-4776-bf1e-48aac4e04320.pdf','safety, report','2026-05-08 22:06:03.099130','2026-05-08 22:02:04.730167',1,NULL,2),(6,NULL,'Maintenance Department Policy Document','POLICY','extracted/maintenance/2026/05/63c5f458-8337-4e3d-b5a8-125b94409447_extracted.txt','• Metro Rail **POLICY**, 2017\n• Background and context\n• Indian cities are growing rapidly.\n• There is a need to direct growth in a planned \nmanner with adequate **ATTENTION** to the transport system at early stages in their \ndevelopment.\n• Urban Rail, popularly referred to as Metro Rail, has seen substantial growth in \nIndia in the recent years.\n• More cities are experiencing the need for metro rail to \nmeet their day-to-day mobility **REQUIREMENT**s.\n• Most of the metro rail **PROJECT**s have \nbeen financed by the central government in partnership with the state \ngovernments, while some have been funded by the state governments either on \ntheir own or with private partnership.\n• The following are the prevalent broad models of financing metro rail in India:\n• The existing 50:50 Joint Venture model that is predominantly the major model \navailable for the financing and organization structure was started with Delhi Metro \nRail Corporation and later followed in other metros like Mumbai Line-3, Chennai, \nBangalore, Nagpur, Lucknow, Kochi and Ahmedabad.\n• The second model is that of full funding by the central government.\n• Examples of \nthis model are the first metro in the city of Calcutta (now Kolkata) by Indian \nRailways, followed by East-West corridor in Kolkata being implemented on a \n74:26 equity sharing between Ministry of Railways and Ministry of Urban \nDevelopment respectively.\n• The third model is that of **COMPLETE** funding by state government; examples are \nMetro rail in Jaipur and Monorail in Mumbai.\n• The other model is the Public Private Partnership (PPP).\n• Mumbai Metro Line-1 \nand Hyderabad metro rail have been taken up with Viability Gap Funding (VGF) \nfrom Government of India.\n• **BENEFIT**s of Mass Rapid Transit Systems\n• Mass Rapid Transit Systems in urban areas not only facilitate easy and quick \nmovement of people but also have a positive impact on the economic growth and \nquality of life.\n• This results in increased income and various **BENEFIT**s to the society\n• like reduced external cost due to reduction in traffic congestion, road and parking \ncost, transport cost and per-capita traffic **ACCIDENT**s.\n• Mass Rapid Transit Systems \ntend to reduce per capita vehicle ownership and usage and encourage more \ncompact & walkable development pattern which provide developmental **BENEFIT**s to \nthe society.\n• Reduction in cost and time of travel lowers the cost of production of \ngoods and **SERVICE**s which significantly improves city's competitiveness.\n• Options of Mass Rapid Transit Systems(MRTS)\n• The mass transit systems in cities/ urban agglomeration can be broadly classified \ninto the following 5 categories:\n• Busways and Bus Rapid Transit System (BRTS): Busways are \nphysically demarcated bus lanes along the main carriageway with a \nsegregated corridor for movement of buses only.\n• At the intersections, the \nbuses may be given priority over other modes through a signalling system.\n• Light Rail Transit (LRT): LRT is generally at-grade rail based mass transit \nsystem, which is generally segregated from the main carriageway.\n• Tramways: These are at-grade rail based system that are not segregated\n• and often move in mixed traffic conditions.\n• Metro Rail: Metro rail is a fully segregated rail based mass transit system,\n• which could be at grade, elevated or underground.\n• Due to its physical \nsegregation and system technology, metro rail can have a very high \ncapacity of 40,000 – 80,000 passengers per hour per direction (PPHPD).\n• Metro systems also include monorails, which, however, has lower capacities \nand higher **MAINTENANCE** cost.\n• Regional Rail: Regional rail caters to passenger **SERVICE**s within a larger \nurban agglomerate or metropolitan area connecting the outskirts to the \ncenter of the city.\n• The **SERVICE**s have greater number of halts at smaller \ndistances compared to long distance railways but fewer halts and higher \nspeeds compared to metro rail.\n• Choice of Metro Rail as a Mode of Mass Transit: The choice of a particular \nMRTS will depend on a variety of factors like demand, capacity, cost and ease of \nimplementation.\n• A BRT or LRT systems at grade may require linear pathway to be \ncarved out of existing land if additional space cannot be made available on the \nsideways and will reduce the space for other traffic depending on the width of \nexisting roads.\n• The capacity of MRTS is generally \nde**NOTE**d by passengers per hour per direction (PPHPD).\n• systems also generally provide rapid **SERVICE**, a higher quality ride and **SERVICE** \nregularity due to grade separation.\n• It is pertinent to observe that the above mentioned capacities of different systems \ncan be at best, a guidance parameter and choice of mode will depend on the \noverall feasibility of the transport system.\n• Planning and Implementation of Metro Rail **PROJECT**s\n• Metro Rail: A mode of Urban Transport\n• Due to the very nature of urban transport and its inseparable and intricate connect \nwith the issues of urban development, it is essential that those who have overall \nperspective and feel of the city formulate the plans for urban transport for that city.\n• Therefore, the proposals for central assistance for an identified metro rail **PROJECT** \nwill have to be mooted by the State Government; also as the "Urban development" \nis a State subject in the Constitution.\n• System Approach.\n• There **SHOULD** be a comprehensive approach to planning for urban land use and \ntransport infrastructure.\n• A system approach **SHOULD** be applied in the planning of \nmulti-modal transport systems in a city.\n• By treating the \nurban area as a system, and recognizing the inter**ACTION**s between land use, traffic \nand transport, it is possible to predict future **REQUIREMENT**s and accordingly \nevaluate alternative modes for the most optimum mobility plan for the city.\n• Therefore, a Comprehensive Mobility Plan (CMP), is a **MANDATORY** prerequisite for \nplanning metro rail in any city.\n• Cities having a population of two million and more \nmay start planning for mass transit systems including metro rail based on the \nCMP.\n• Integration between various modes like roadway, railways, non-motorized \ntransport, and other modes of transport enhances the mobility of the citizens and \nencourages public transport.\n• Existing railway suburban **SERVICE**s or circular rail \nsystems, if any, **SHOULD** be integrated with the metro rail and other transport \nmodes.\n\n📌 Key Terms: POLICY, Background, Indian, ATTENTION, Cities, Efforts, Transit, REQUIREMENT, PROJECT, Venture, Corporation, Mumbai, Chennai, Bangalore, Nagpur, Lucknow, Ahmedabad, Examples, Calcutta, Kolkata, Railways, Ministry, Development, COMPLETE, Jaipur, Monorail, Public, Private, Partnership, Hyderabad, Viability, Funding, Government, Gurugram, Haryana, BENEFIT, Systems, ACCIDENT, SERVICE, Reduction, Options, Busways, System, Motorised, Vehicles, Tramways, MAINTENANCE, Regional, Choice, NOTE, Planning, Implementation, Transport, Therefore, Constitution, Approach, SHOULD, ACTION, MANDATORY, Comprehensive, Mobility, Integration, Existing','TIKA','pdf','59a3f7f130eecMetro_Rail_Policy_2025.pdf','original/general/2026/05/63c5f458-8337-4e3d-b5a8-125b94409447.pdf',309425,'application/pdf',_binary '\0',_binary '',_binary '',_binary '',_binary '\0',NULL,NULL,NULL,'MEDIUM','2026-05-09 09:35:43.046790',_binary '',2,_binary '',1,_binary '',3,4,'ACTIVE','63c5f458-8337-4e3d-b5a8-125b94409447.pdf','policy, safety, audit','2026-05-09 09:36:32.273351','2026-05-09 09:35:43.047787',1,NULL,2),(8,NULL,'Legal Notice and Compliance ','LEGAL_NOTICE','extracted/maintenance/2026/05/ab3795d0-f8aa-4698-871f-9699f8620e47_extracted.txt','• --- Page 1 ---\nChennai Metro Rail Limited\n• (A Joint Venture of Govt.\n• of India and Govt.\n• F\nSonat er oO" (ISO 9001:2015 and ISO 14001:2015 Certified)\nLetter No.\n• CMRL/CON/ES/1121/2026 **DATE**d: 18.04.2026\nTo,\n• George Jenner, IFS,\n• Deputy Director General of Forests (C),\n• Ministry of Environment, Forest and Climate Change,\nIntegrated Regional Office,\n• Floor, Additional Office Block for GPOA,\n• Shastri Bhawan, Haddows Road\n• Nungambakkam, Chennai — 600 034.\n• Sub: Submission of Six-Monthly Monitoring report (CRZ Clearance) for the period of\nSeptember 2025 to February 2026 for the Construction of Metro Rail Network —\nCorridor 3 (Madhavaram to SIPCOT) having **TOTAL** length of the corridor as 45.813 km\ncomprising 50 stations (30 underground stations and 20 elevated stations) in Chennai,\nTamil Nadu by M/s Chennai Metro Rail Limited (CMRL) - Reg.\n• Ref: CRZ Clearance Letter No F.No.11-39/2022-1A.IIl **DATE**d 28th February 2023.\n• With **REFERENCE** to the Part B - General Conditions, Condition no.\n• (vii) stipulated in the CRZ\nClearance cited above we are **SUBMIT**ting herewith the six-monthly monitoring report\n(September 2025 to February 2026) along with necessary supporting **DOCUMENT**s for your\nkind information and perusal.\n• We wish to mention that all the necessary **ACTION**s are being taken in compliance to the\nspecific and general conditions of CRZ Clearance granted to the **PROJECT**.\n• We trust that the information furnished is in line with your **REQUIREMENT**.\n• Yours Sincerely,\n• | ga® gl\nsicko (Ce)\n\\S a\n• Saravana Kumar R, WS\n• Manager - Environment.\n• Enclosure: As stated above\n• METROS, Anna Salai, Nandanam, Chennai - 600 035.\n• **PHONE**: 044-2437 8000 / **EMAIL**: chennaimetrorail@ecmrl.in / Website: www.chennaimetrorail.org\nCIN : U60100TN2007SGC065596\n• --- Page 2 ---\nSix Monthly Monitoring Report for CRZ Clearance issued by MoEF&CC, New\nDelhi_vide letter .\n• 11-39/2022-IA.IIl, **DATE**d 28 February 2023 for\nConstruction of Metro Rail Network — Corridor 3 (Madhavaram to SIPCOT)\nhaving **TOTAL** length of the Corridor as 45.813 km comprising 50 stations (30\nunderground stations and 20 elevated stations) in Chennai, Tamil Nadu by M/s\nChennai Metro Rail Limited (CMRL).\n• Part A — Specific Conditions\n• **STATUS** of Compliance / Remarks\n• All construction shall be strictly by the\nprovisions of CRZ Notification, 2011 and\nas amended from time to time.\n• The recommendations/ — conditions |\napplicable for construction stage are\n• compiled as per CRZ Notification 2011\n• and its amendments.\n• levels shall be monitored regularly at\n200m on either side of alignment at\nreceptors comprising\n• and other fragile buildings.\n• During the construction the vibration | Being Complied.\n• | during construction at 200 m on either\neducational, | side of the alignment.\n• medical, and physical cultural buildings |\n• Vibration levels are monitored regularly\n• The Ground Penetrating Radar (GPR)\nmeasurements before and_ after\ntunneling **SHOULD** be taken up loose\nstrata identified from geotechnical data\nalong alignment and regular compliance\nreport **SHOULD** be **SUBMIT**ted to Regional\nOffice.\n• | **NOTE**d and Agreed to Comply.\n• The GPR measurements are being\nmonitored before and after tunneling in\nthe CRZ areas.\n• No ground water shall be extracted\nwithin the CRZ area to meet water\n**REQUIREMENT**s during the construction\nand/or operational phase of the **PROJECT**.\n• Being Complied.\n• No ground water within the CRZ area\nwas extracted during the construction or\noperation phase of the **PROJECT**.\n• For\nconstruction purposes water is procured\n• The **PROJECT** proponent shall ensure the\n_ natural flow of the creek water at the\n• Being Complied.\n• No blockage of rivers or creek is\n| envisaged in our **PROJECT** proposal.\n• Proper care was taken to ensure free\n• flow of water throughout the **PROJECT**.\n\n📌 Key Terms: Chennai, Limited, Venture, Certified, Letter, DATE, George, Jenner, Deputy, Director, General, Forests, Ministry, Environment, Forest, Climate, Change, Integrated, Regional, Office, Additional, Shastri, Bhawan, Haddows, Nungambakkam, TOTAL, Submission, Monthly, Monitoring, Clearance, September, February, Construction, Network, Corridor, Madhavaram, SIPCOT, REFERENCE, Conditions, Condition, SUBMIT, DOCUMENT, ACTION, PROJECT, REQUIREMENT, Sincerely, Saravana, Manager, Enclosure, METROS, Nandanam, EMAIL, PHONE, Website, U60100TN2007SGC065596, Report, Specific, STATUS, Compliance, Remarks, Notification, During, Complied, Vibration, SHOULD, Ground, Penetrating, NOTE, Agreed, Comply, Proper','OCR','pdf','Corridor-3-CRZ-Clearance-Six-Monthly-Monitoring-Report-for-the-Period-of-Sep-2025-to-Feb-2026-1-1.pdf','original/general/2026/05/ab3795d0-f8aa-4698-871f-9699f8620e47.pdf',3412735,'application/pdf',_binary '\0',_binary '',_binary '',_binary '',_binary '\0',NULL,NULL,'eng','MEDIUM','2026-05-09 09:43:41.333810',_binary '',2,_binary '',1,_binary '',3,4,'ACTIVE','ab3795d0-f8aa-4698-871f-9699f8620e47.pdf','legal, safety, audit','2026-05-09 09:46:10.871102','2026-05-09 09:43:41.339812',1,NULL,2);
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `policy_rules`
--

DROP TABLE IF EXISTS `policy_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `policy_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `dashboard_enabled` bit(1) DEFAULT NULL,
  `dept_admin_escalation_hours` int NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `email_enabled` bit(1) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `is_default` bit(1) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `priority` enum('HIGH','MEDIUM','LOW') DEFAULT NULL,
  `reminder_hours` int NOT NULL,
  `sms_enabled` bit(1) DEFAULT NULL,
  `super_admin_escalation_hours` int NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `violation_hours` int NOT NULL,
  `created_by` bigint DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `updated_by` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_policy_dept_priority` (`department_id`,`priority`),
  KEY `FKtn432dib2osb68j5oqwwcx2cs` (`created_by`),
  KEY `FKostqxq5628k03rkntm8el85ql` (`updated_by`),
  CONSTRAINT `FKostqxq5628k03rkntm8el85ql` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtfwsmbdvgmew9nkesav6qgbtl` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  CONSTRAINT `FKtn432dib2osb68j5oqwwcx2cs` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `policy_rules`
--

LOCK TABLES `policy_rules` WRITE;
/*!40000 ALTER TABLE `policy_rules` DISABLE KEYS */;
INSERT INTO `policy_rules` (`id`, `created_at`, `dashboard_enabled`, `dept_admin_escalation_hours`, `description`, `email_enabled`, `is_active`, `is_default`, `name`, `priority`, `reminder_hours`, `sms_enabled`, `super_admin_escalation_hours`, `updated_at`, `violation_hours`, `created_by`, `department_id`, `updated_by`) VALUES (1,'2026-05-08 20:25:18.888732',_binary '',48,'Default compliance policy for all departments and priorities',_binary '',_binary '',_binary '','Global Default',NULL,24,_binary '',72,'2026-05-08 20:25:18.889732',168,NULL,NULL,NULL),(2,NULL,_binary '',48,'Default compliance policy for all departments',_binary '',_binary '',_binary '','Global Default',NULL,24,_binary '\0',72,NULL,168,NULL,NULL,NULL),(3,NULL,_binary '',12,'Urgent safety documents - fastest escalation',_binary '',_binary '',_binary '\0','Safety HIGH Priority','HIGH',6,_binary '',24,NULL,48,NULL,2,NULL),(4,NULL,_binary '',48,'Standard safety documents',_binary '',_binary '',_binary '\0','Safety MEDIUM Priority','MEDIUM',24,_binary '',72,NULL,168,NULL,2,NULL),(5,NULL,_binary '',24,'Critical operations documents',_binary '',_binary '',_binary '\0','Operations HIGH Priority','HIGH',12,_binary '',48,NULL,96,NULL,7,NULL),(6,NULL,_binary '',0,'Low priority HR documents - relaxed timeline',_binary '\0',_binary '',_binary '\0','HR LOW Priority','LOW',48,_binary '\0',0,NULL,0,NULL,3,NULL),(7,NULL,_binary '',48,'Standard finance documents',_binary '',_binary '',_binary '\0','Finance MEDIUM Priority','MEDIUM',24,_binary '\0',72,NULL,168,NULL,4,NULL),(8,NULL,_binary '',48,'Default compliance policy for all departments',_binary '',_binary '',_binary '','Global Default',NULL,24,_binary '\0',72,NULL,168,NULL,NULL,NULL),(14,NULL,_binary '',48,'Default compliance policy for all departments',_binary '',_binary '',_binary '','Global Default',NULL,24,_binary '\0',72,NULL,168,NULL,NULL,NULL);
/*!40000 ALTER TABLE `policy_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `risk_score_snapshots`
--

DROP TABLE IF EXISTS `risk_score_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `risk_score_snapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `calculation_notes` varchar(500) DEFAULT NULL,
  `calculation_period_end` datetime(6) DEFAULT NULL,
  `calculation_period_start` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `dept_admin_escalation_count` int DEFAULT NULL,
  `entity_id` bigint NOT NULL,
  `entity_name` varchar(200) DEFAULT NULL,
  `entity_type` enum('USER','DEPARTMENT') NOT NULL,
  `late_acknowledgement_count` int DEFAULT NULL,
  `legal_hold_count` int DEFAULT NULL,
  `pending_violation_count` int DEFAULT NULL,
  `repeat_offense_count` int DEFAULT NULL,
  `risk_level` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `risk_score` int NOT NULL,
  `safety_violation_count` int DEFAULT NULL,
  `super_admin_escalation_count` int DEFAULT NULL,
  `violation_count` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `risk_score_snapshots`
--

LOCK TABLES `risk_score_snapshots` WRITE;
/*!40000 ALTER TABLE `risk_score_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `risk_score_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `department` varchar(50) DEFAULT NULL,
  `designation` varchar(100) DEFAULT NULL,
  `email` varchar(150) NOT NULL,
  `employee_id` varchar(50) DEFAULT NULL,
  `is_active` bit(1) DEFAULT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `role` enum('SUPER_ADMIN','DEPARTMENT_UPLOAD_ADMIN','DEPARTMENT_ADMIN','DEPARTMENT_USER') NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK_d1s31g1a7ilra77m65xmka3ei` (`employee_id`),
  KEY `FKsbg59w8q63i0oo53rlgvlcnjq` (`department_id`),
  CONSTRAINT `FKsbg59w8q63i0oo53rlgvlcnjq` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`id`, `created_at`, `department`, `designation`, `email`, `employee_id`, `is_active`, `last_login`, `name`, `password`, `phone_number`, `role`, `updated_at`, `department_id`) VALUES (1,NULL,NULL,NULL,'sekarans2384@gmail.com','ADMIN-001',_binary '','2026-05-09 09:50:42.492537','System Admin','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG',NULL,'SUPER_ADMIN','2026-05-09 09:50:42.495940',NULL),(2,NULL,NULL,NULL,'dhinakaran1845@gmail.com','MAINT-001',_binary '','2026-05-09 09:48:46.125427','Maintenance Supervisor','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543219','DEPARTMENT_UPLOAD_ADMIN','2026-05-09 09:50:10.403326',1),(3,NULL,NULL,NULL,'dhinakaransekaran79@gmail.com','SAFE-001',_binary '',NULL,'Safety Officer','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543211','DEPARTMENT_UPLOAD_ADMIN',NULL,2),(4,NULL,NULL,NULL,'hr.admin@metrohub.in','HR-001',_binary '',NULL,'HR Executive','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543212','DEPARTMENT_UPLOAD_ADMIN',NULL,3),(5,NULL,NULL,NULL,'finance.admin@metrohub.in','FIN-001',_binary '',NULL,'Accounts Officer','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543213','DEPARTMENT_UPLOAD_ADMIN',NULL,4),(6,NULL,NULL,NULL,'legal.admin@metrohub.in','LEGAL-001',_binary '',NULL,'Legal Officer','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543214','DEPARTMENT_UPLOAD_ADMIN',NULL,5),(7,NULL,NULL,NULL,'ops.admin@metrohub.in','OPS-001',_binary '',NULL,'Operations Manager','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543215','DEPARTMENT_UPLOAD_ADMIN',NULL,7),(8,NULL,NULL,NULL,'dhinakaran9890@gmail.com','MAINT-MGR-001',_binary '','2026-05-09 09:52:51.214697','Maintenance Manager','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543220','DEPARTMENT_ADMIN','2026-05-09 09:52:51.219701',1),(9,NULL,NULL,NULL,'safety.manager@metrohub.in','SAFE-MGR-001',_binary '',NULL,'Safety Manager','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543221','DEPARTMENT_ADMIN',NULL,2),(10,NULL,NULL,NULL,'hr.manager@metrohub.in','HR-MGR-001',_binary '',NULL,'HR Manager','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543222','DEPARTMENT_ADMIN',NULL,3),(11,NULL,NULL,NULL,'dhinakaranc23cse@srishakthi.ac.in','MAINT-TECH-001',_binary '','2026-05-09 09:46:49.869890','Maint Technician 1','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543230','DEPARTMENT_USER','2026-05-09 09:46:49.879506',1),(12,NULL,NULL,NULL,'shalushalu09890@gmail.com','MAINT-TECH-002',_binary '','2026-05-09 08:49:13.405838','Maint Technician 2','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543231','DEPARTMENT_USER','2026-05-09 08:49:13.410148',1),(13,NULL,NULL,NULL,'dhina5395@gmail.com','SAFE-INSP-001',_binary '',NULL,'Safety Inspector','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543232','DEPARTMENT_USER',NULL,2),(14,NULL,NULL,NULL,'harinim23cse@srishakthi.ac.in','SAFE-SHIFT-001',_binary '',NULL,'Shift In-charge','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543233','DEPARTMENT_USER',NULL,2),(15,NULL,NULL,NULL,'hr.assistant@metrohub.in','HR-ASST-001',_binary '',NULL,'HR Assistant','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543234','DEPARTMENT_USER',NULL,3),(16,NULL,NULL,NULL,'finance.auditor@metrohub.in','FIN-AUD-001',_binary '',NULL,'Finance Auditor','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543235','DEPARTMENT_USER',NULL,4),(17,NULL,NULL,NULL,'ops.controller@metrohub.in','OPS-CTRL-001',_binary '',NULL,'Operations Controller','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543236','DEPARTMENT_USER',NULL,7),(18,NULL,NULL,NULL,'ops.station@metrohub.in','OPS-STN-001',_binary '',NULL,'Station Manager','$2a$10$sBQtFJBMVALHRKWO/DKNr.paxXwQKjUOWPXnQvVE4y1sAJXsX5FfG','+919876543237','DEPARTMENT_USER',NULL,7);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET CHARACTER_COLLATION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET @OLD_SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-18 with ALL 39 ALERTS - COMPLETE DATASET
-- NO DATA OMITTED - 100% COMPLETE

-- ============================================================
-- ============================================================
--           CLEANUP / TRUNCATE / DELETE OPERATIONS
-- ============================================================
-- ============================================================
-- WARNING: These operations DELETE ALL DATA from the tables.
-- Uncomment and run ONLY when you need to reset the database.
-- Execute in this exact order to respect foreign key constraints.
-- ============================================================

-- TRUNCATE ALL TABLES (order respects FK dependencies)
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE risk_score_snapshots;
-- TRUNCATE TABLE document_reminders;
-- TRUNCATE TABLE compliance_violations;
-- TRUNCATE TABLE document_acknowledgements;
-- TRUNCATE TABLE alerts;
-- TRUNCATE TABLE audit_logs;
-- TRUNCATE TABLE document_metadata;
-- TRUNCATE TABLE policy_rules;
-- TRUNCATE TABLE documents;
-- TRUNCATE TABLE users;
-- TRUNCATE TABLE departments;
-- SET FOREIGN_KEY_CHECKS = 1;

-- DROP ALL TABLES (complete reset)
-- SET FOREIGN_KEY_CHECKS = 0;
-- DROP TABLE IF EXISTS risk_score_snapshots;
-- DROP TABLE IF EXISTS document_reminders;
-- DROP TABLE IF EXISTS compliance_violations;
-- DROP TABLE IF EXISTS document_acknowledgements;
-- DROP TABLE IF EXISTS alerts;
-- DROP TABLE IF EXISTS audit_logs;
-- DROP TABLE IF EXISTS document_metadata;
-- DROP TABLE IF EXISTS policy_rules;
-- DROP TABLE IF EXISTS documents;
-- DROP TABLE IF EXISTS users;
-- DROP TABLE IF EXISTS departments;
-- SET FOREIGN_KEY_CHECKS = 1;

-- DROP DATABASE (nuclear option)
-- DROP DATABASE IF EXISTS metrohub_db;

-- ============================================================
-- END OF COMPLETE DATABASE BACKUP
-- ============================================================
