-- P1：客户团队权限关系表（替代 rw_user_id / ro_user_id 的 FIND_IN_SET）
-- 列表可用索引：user_id + customer_id

CREATE TABLE IF NOT EXISTS `yshop_crm_customer_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `user_id` bigint NOT NULL COMMENT '成员用户ID',
  `perm` tinyint NOT NULL DEFAULT 0 COMMENT '权限：0-只读 1-读写',
  `creator` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_customer_user` (`customer_id`, `user_id`),
  KEY `idx_user_customer` (`user_id`, `customer_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '客户团队成员' ROW_FORMAT = Dynamic;

-- 从历史逗号串回填读写成员（perm=1）
INSERT IGNORE INTO `yshop_crm_customer_member`
  (`customer_id`, `user_id`, `perm`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  c.`id`,
  u.`id`,
  1,
  '1',
  NOW(),
  '1',
  NOW(),
  b'0',
  IFNULL(c.`tenant_id`, 0)
FROM `yshop_crm_customer` c
INNER JOIN `system_users` u ON FIND_IN_SET(u.`id`, c.`rw_user_id`) > 0
WHERE c.`deleted` = b'0'
  AND c.`rw_user_id` IS NOT NULL
  AND c.`rw_user_id` <> '';

-- 从历史逗号串回填只读成员（perm=0）；若已是读写则保留读写
INSERT IGNORE INTO `yshop_crm_customer_member`
  (`customer_id`, `user_id`, `perm`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT
  c.`id`,
  u.`id`,
  0,
  '1',
  NOW(),
  '1',
  NOW(),
  b'0',
  IFNULL(c.`tenant_id`, 0)
FROM `yshop_crm_customer` c
INNER JOIN `system_users` u ON FIND_IN_SET(u.`id`, c.`ro_user_id`) > 0
WHERE c.`deleted` = b'0'
  AND c.`ro_user_id` IS NOT NULL
  AND c.`ro_user_id` <> '';
