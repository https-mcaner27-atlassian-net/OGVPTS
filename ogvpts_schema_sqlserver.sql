-- Öğrenci–Danışman Görüşme ve Proje Takip Sistemi (OGVPTS)
-- Veritabanı şeması: Microsoft SQL Server (SSMS ile çalıştırılır)
-- Jira: OGVPTS-54 (DB-01) / OGVPTS-55 (DB-02)
-- Kullanım: SSMS'te New Query aç, dosyanın tamamını yapıştır, F5 ile çalıştır.

IF DB_ID('OGVPTS') IS NULL
    CREATE DATABASE OGVPTS;
GO
USE OGVPTS;
GO

-- ---------- Kullanıcılar (FR-01, ROL-01..03) ----------
CREATE TABLE users (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    full_name     NVARCHAR(150) NOT NULL,
    email         NVARCHAR(255) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,              -- NFR-02: düz metin şifre tutulmaz
    role          NVARCHAR(20)  NOT NULL
                  CONSTRAINT ck_users_role CHECK (role IN ('ogrenci','danisman','yonetici')),
    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- ---------- Gruplar ve projeler (FR-05, FR-10, FR-12, BR-01) ----------
CREATE TABLE project_groups (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    name        NVARCHAR(150) NOT NULL,
    advisor_id  INT NULL REFERENCES users(id),         -- grubun sorumlu danışmanı
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE group_members (
    group_id  INT NOT NULL REFERENCES project_groups(id) ON DELETE CASCADE,
    user_id   INT NOT NULL UNIQUE REFERENCES users(id),     -- BR-01: öğrenci tek grupta
    PRIMARY KEY (group_id, user_id)
);
GO

-- FR-12: bir grupta en fazla 2 öğrenci
CREATE TRIGGER trg_group_max_two
ON group_members
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (
        SELECT 1
        FROM group_members gm
        WHERE gm.group_id IN (SELECT group_id FROM inserted)
        GROUP BY gm.group_id
        HAVING COUNT(*) > 2
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 50001, N'Bir grupta en fazla 2 öğrenci olabilir', 1;
    END
END;
GO

CREATE TABLE projects (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    group_id    INT NOT NULL UNIQUE REFERENCES project_groups(id) ON DELETE CASCADE,
    title       NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    status      NVARCHAR(20) NOT NULL DEFAULT 'devam_ediyor'
                CONSTRAINT ck_projects_status CHECK (status IN ('baslamadi','devam_ediyor','tamamlandi')),
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE project_milestones (                      -- proje aşamaları (FR-05, FR-06)
    id         INT IDENTITY(1,1) PRIMARY KEY,
    project_id INT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title      NVARCHAR(200) NOT NULL,
    due_date   DATE NOT NULL,
    status     NVARCHAR(20) NOT NULL DEFAULT 'baslamadi'
               CONSTRAINT ck_milestones_status CHECK (status IN ('baslamadi','devam_ediyor','tamamlandi'))
);
GO

CREATE TABLE project_comments (                        -- danışman yorumları (FR-07)
    id         INT IDENTITY(1,1) PRIMARY KEY,
    project_id INT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    author_id  INT NOT NULL REFERENCES users(id),
    body       NVARCHAR(MAX) NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- ---------- Görüşme planlama (FR-02, FR-03, FR-04, FR-09, BR-02..04) ----------
CREATE TABLE availability_slots (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    advisor_id INT NOT NULL REFERENCES users(id),
    start_time DATETIME2 NOT NULL,
    end_time   DATETIME2 NOT NULL,
    CONSTRAINT ck_slot_time CHECK (end_time > start_time),
    CONSTRAINT uq_slot_advisor_start UNIQUE (advisor_id, start_time)   -- BR-02
);
GO

CREATE TABLE meetings (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    slot_id       INT NOT NULL REFERENCES availability_slots(id),
    group_id      INT NOT NULL REFERENCES project_groups(id),
    requested_by  INT NOT NULL REFERENCES users(id),
    topic         NVARCHAR(300) NOT NULL,
    status        NVARCHAR(30) NOT NULL DEFAULT 'beklemede'
                  CONSTRAINT ck_meetings_status CHECK (status IN
                      ('beklemede','onaylandi','reddedildi','yeni_saat_onerildi','tamamlandi','iptal')),
    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    -- BR-02 için yardımcı sütun: onaylı/tamamlanmış görüşme "aktif" (1) sayılır.
    -- Filtered index computed column üzerinde kurulamadığı için gerçek sütun + CHECK ile status'e bağlandı.
    is_active     BIT NOT NULL DEFAULT 0,
    CONSTRAINT ck_meetings_active CHECK (
        (status IN ('onaylandi','tamamlandi') AND is_active = 1) OR
        (status NOT IN ('onaylandi','tamamlandi') AND is_active = 0))
);
GO

-- BR-02: aynı slot için yalnızca bir aktif görüşme
CREATE UNIQUE INDEX ux_meeting_slot_active ON meetings(slot_id) WHERE is_active = 1;
GO

CREATE TABLE meeting_notes (                           -- BR-04: not yalnızca ilgili danışmanın
    id          INT IDENTITY(1,1) PRIMARY KEY,
    meeting_id  INT NOT NULL UNIQUE REFERENCES meetings(id) ON DELETE CASCADE,
    advisor_id  INT NOT NULL REFERENCES users(id),
    note        NVARCHAR(MAX) NOT NULL,
    decisions   NVARCHAR(MAX) NULL,
    updated_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- ---------- Görev, teslim ve geri bildirim (FR-13, FR-14, FR-15) ----------
CREATE TABLE tasks (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    group_id    INT NOT NULL REFERENCES project_groups(id),
    advisor_id  INT NOT NULL REFERENCES users(id),
    title       NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX) NULL,
    due_date    DATE NOT NULL,
    status      NVARCHAR(30) NOT NULL DEFAULT 'atandi'
                CONSTRAINT ck_tasks_status CHECK (status IN
                    ('atandi','teslim_edildi','duzeltme_istendi','onaylandi')),
    created_at  DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE submissions (                             -- her teslim ayrı kayıt: geçmiş korunur
    id           INT IDENTITY(1,1) PRIMARY KEY,
    task_id      INT NOT NULL REFERENCES tasks(id),
    submitted_by INT NOT NULL REFERENCES users(id),
    version_no   INT NOT NULL DEFAULT 1,
    file_name    NVARCHAR(255) NOT NULL,
    file_path    NVARCHAR(500) NOT NULL,
    file_size_kb INT NOT NULL CONSTRAINT ck_file_size CHECK (file_size_kb <= 20480),  -- FR-06: en fazla 20 MB
    is_late      BIT NOT NULL DEFAULT 0,
    submitted_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_submission_version UNIQUE (task_id, version_no)
);
GO

CREATE TABLE feedback (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    submission_id INT NOT NULL REFERENCES submissions(id),
    advisor_id    INT NOT NULL REFERENCES users(id),
    decision      NVARCHAR(20) NOT NULL CONSTRAINT ck_feedback_decision CHECK (decision IN ('onay','duzeltme')),
    comment       NVARCHAR(MAX) NULL,
    created_at    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT ck_feedback_comment CHECK (decision = 'onay' OR (comment IS NOT NULL AND LEN(comment) > 0))
);
GO

-- ---------- Bildirimler (FR-08) ----------
CREATE TABLE notifications (
    id         INT IDENTITY(1,1) PRIMARY KEY,
    user_id    INT NOT NULL REFERENCES users(id),
    type       NVARCHAR(50) NOT NULL,
    message    NVARCHAR(500) NOT NULL,
    is_read    BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

-- Kontrol: oluşturulan tabloları listele (ekran görüntüsü için)
SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME;
GO
