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

 Date: 11/11/2021 09:44:24
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for stock_tbl
-- ----------------------------
DROP TABLE IF EXISTS `stock_tbl`;
CREATE TABLE `stock_tbl` (
  `id` int(11) DEFAULT NULL,
  `count` int(11) DEFAULT NULL,
  `commodity_code` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- ----------------------------
-- Records of stock_tbl
-- ----------------------------
BEGIN;
INSERT INTO `stock_tbl` VALUES (NULL, 100, 'C100000');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
