--
-- Copyright 2010-2018 Boxfuse GmbH
--
-- INTERNAL RELEASE. ALL RIGHTS RESERVED.
--
-- Must
-- be
-- exactly
-- 13 lines
-- to match
-- community
-- edition
-- license
-- length.
--

create table EMPLOYEE ( "ID" integer not null, "NAME" varchar(100) );
alter table EMPLOYEE add primary KEY ("ID");
ALTER TABLE EMPLOYEE ADD COLUMN SYS_START TIMESTAMP(12) NOT NULL GENERATED AS ROW BEGIN IMPLICITLY HIDDEN;
ALTER TABLE EMPLOYEE ADD COLUMN SYS_END TIMESTAMP(12) NOT NULL GENERATED AS ROW END IMPLICITLY HIDDEN;
ALTER TABLE EMPLOYEE ADD COLUMN TRANS_ID TIMESTAMP(12) GENERATED AS TRANSACTION START ID IMPLICITLY HIDDEN;
ALTER TABLE EMPLOYEE ADD PERIOD SYSTEM_TIME (sys_start, sys_end);

CREATE TABLE EMPLOYEE_HIST LIKE EMPLOYEE;

ALTER TABLE EMPLOYEE ADD VERSIONING USE HISTORY TABLE EMPLOYEE_HIST;
