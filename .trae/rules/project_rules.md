项目规则:
1. 角色权限管理
   tenant_id = 0  运营商/平台方/开发者（拥有全平台管理权限）
   role_level = 0  超级管理员（租户内最高权限）
   role_level = 1  管理员（租户内普通管理权限）
   role_level = 2  操作员（租户内基础操作权限）
   role_level=0 是超级管理员权限依然受到tenant_id的约束，只能管理当前租户的信息!!
   我已经设计了LevelCode作为权限的枚举类