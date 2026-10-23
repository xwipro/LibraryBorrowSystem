-- ============================================
-- 图书管理系统 2026 - 建表脚本 (Library 库)
-- 执行策略: IF NOT EXISTS 幂等, 由 spring.sql.init 在应用启动时执行
-- 表名按主题自行设计: library_* 前缀, 归属 Library 数据库
-- ============================================

-- 图书分类表
CREATE TABLE IF NOT EXISTS library_book_category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 图书表
CREATE TABLE IF NOT EXISTS library_book (
    id           BIGSERIAL PRIMARY KEY,
    isbn         VARCHAR(20)  NOT NULL UNIQUE,
    title        VARCHAR(200) NOT NULL,
    author       VARCHAR(100) NOT NULL,
    publisher    VARCHAR(100) NOT NULL,
    publish_date DATE,
    category_id  BIGINT       NOT NULL REFERENCES library_book_category (id),
    cover        VARCHAR(500),
    summary      TEXT,
    stock_count  INT          NOT NULL DEFAULT 0,
    total_count  INT          NOT NULL DEFAULT 0,
    location     VARCHAR(100),
    status       SMALLINT     NOT NULL DEFAULT 1,
    rating       NUMERIC(3, 1),
    borrow_count INT          NOT NULL DEFAULT 0,
    create_time  TIMESTAMP,
    update_time  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_book_title ON library_book (title);
CREATE INDEX IF NOT EXISTS idx_book_author ON library_book (author);
CREATE INDEX IF NOT EXISTS idx_book_category ON library_book (category_id);
CREATE INDEX IF NOT EXISTS idx_book_status ON library_book (status);

-- 读者表
CREATE TABLE IF NOT EXISTS library_reader (
    id           BIGSERIAL PRIMARY KEY,
    reader_no    VARCHAR(20)  NOT NULL UNIQUE,
    name         VARCHAR(50)  NOT NULL,
    gender       SMALLINT     NOT NULL DEFAULT 0,
    phone        VARCHAR(20),
    email        VARCHAR(100),
    avatar       VARCHAR(500),
    reader_type  VARCHAR(20)  NOT NULL,
    borrow_count INT          NOT NULL DEFAULT 0,
    max_borrow   INT          NOT NULL DEFAULT 5,
    status       SMALLINT     NOT NULL DEFAULT 1,
    create_time  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reader_name ON library_reader (name);
CREATE INDEX IF NOT EXISTS idx_reader_type ON library_reader (reader_type);
CREATE INDEX IF NOT EXISTS idx_reader_status ON library_reader (status);

-- 借阅记录表
CREATE TABLE IF NOT EXISTS library_borrow_record (
    id           BIGSERIAL PRIMARY KEY,
    reader_id    BIGINT       NOT NULL REFERENCES library_reader (id),
    book_id      BIGINT       NOT NULL REFERENCES library_book (id),
    borrow_time  TIMESTAMP    NOT NULL,
    due_time     TIMESTAMP    NOT NULL,
    return_time  TIMESTAMP,
    status       SMALLINT     NOT NULL DEFAULT 0,
    renew_count  INT          NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_borrow_reader ON library_borrow_record (reader_id);
CREATE INDEX IF NOT EXISTS idx_borrow_book ON library_borrow_record (book_id);
CREATE INDEX IF NOT EXISTS idx_borrow_status ON library_borrow_record (status);
CREATE INDEX IF NOT EXISTS idx_borrow_time ON library_borrow_record (borrow_time);

-- 预约记录表
CREATE TABLE IF NOT EXISTS library_reservation (
    id           BIGSERIAL PRIMARY KEY,
    reader_id    BIGINT       NOT NULL REFERENCES library_reader (id),
    book_id      BIGINT       NOT NULL REFERENCES library_book (id),
    reserve_time TIMESTAMP    NOT NULL,
    expire_time  TIMESTAMP    NOT NULL,
    status       SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_reservation_reader ON library_reservation (reader_id);
CREATE INDEX IF NOT EXISTS idx_reservation_book ON library_reservation (book_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON library_reservation (status);

-- 角色表
CREATE TABLE IF NOT EXISTS library_role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    code        VARCHAR(50) NOT NULL UNIQUE
);

-- 系统用户表
CREATE TABLE IF NOT EXISTS library_user (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    password         VARCHAR(100) NOT NULL,
    real_name        VARCHAR(50)  NOT NULL,
    avatar           VARCHAR(500),
    phone            VARCHAR(20),
    email            VARCHAR(100),
    role_id          BIGINT       NOT NULL REFERENCES library_role (id),
    status           SMALLINT     NOT NULL DEFAULT 1,
    create_time      TIMESTAMP,
    last_login_time  TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_user_role ON library_user (role_id);
CREATE INDEX IF NOT EXISTS idx_user_status ON library_user (status);

-- 操作日志表
CREATE TABLE IF NOT EXISTS library_log (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) NOT NULL,
    module      VARCHAR(50) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    ip          VARCHAR(50),
    time        TIMESTAMP   NOT NULL,
    status      SMALLINT    NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_log_module ON library_log (module);
CREATE INDEX IF NOT EXISTS idx_log_username ON library_log (username);
CREATE INDEX IF NOT EXISTS idx_log_time ON library_log (time);

-- 公告表(管理端发布 -> 读者端展示)
CREATE TABLE IF NOT EXISTS library_announcement (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(200) NOT NULL,
    content        TEXT         NOT NULL,
    publisher_name VARCHAR(50),
    status         SMALLINT     NOT NULL DEFAULT 1,
    create_time    TIMESTAMP,
    update_time    TIMESTAMP
);
-- 置顶标记(可多条置顶, 置顶的按时间倒序排前面)
ALTER TABLE library_announcement ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_announcement_status ON library_announcement (status);
CREATE INDEX IF NOT EXISTS idx_announcement_time ON library_announcement (create_time);

-- 通知表(管理端通知中心, 按登录用户隔离)
CREATE TABLE IF NOT EXISTS library_notification (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'system',
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notification_username ON library_notification (username, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_notification_unread ON library_notification (username, is_read);
