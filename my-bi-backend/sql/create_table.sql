# 数据库初始化
# @author
# @from

-- 创建库
create database if not exists my_db;

-- 切换库
use my_db;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_unionId (unionId)
) comment '用户' collate = utf8mb4_unicode_ci;

create table if not exists chart
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint null comment '创建者',
    goal         text null comment '分析目标',
    name         char(128) null comment '图表名字',
    chartData    text null comment '图表数据',
    chartType    varchar(128) null comment '图标类型',
    Genchart     text null comment '生成的数据',
    GenResult    text null comment '生成的分析结论',
    status       ENUM('pending','processing','completed','failed') NOT NULL DEFAULT 'pending' COMMENT '状态',
    execMessage  text null comment '执行信息',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除'

    ) comment '图表信息表' collate = utf8mb4_unicode_ci;

