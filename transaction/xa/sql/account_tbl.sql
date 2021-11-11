/*
 Navicat Premium Data Transfer

 Source Server         : 110
 Source Server Type    : MySQL
 Source Server Version : 50719
 Source Host           : 10.10.13.110:3306
 Source Schema         : demo

 Target Server Type    : MySQL
 Target Server Version : 50719
 File Encoding         : 65001

 Date: 11/11/2021 09:43:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account_tbl
-- ----------------------------
DROP TABLE IF EXISTS `account_tbl`;
CREATE TABLE `account_tbl` (
  `id` int(11) DEFAULT NULL,
  `money` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ----------------------------
-- Records of account_tbl
-- ----------------------------
BEGIN;
INSERT INTO `account_tbl` VALUES (NULL, '10000', 'U100000');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
