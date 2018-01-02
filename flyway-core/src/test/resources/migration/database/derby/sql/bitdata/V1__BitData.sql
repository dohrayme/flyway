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

CREATE TABLE my_table (my_uuid CHAR (16) FOR BIT DATA NOT NULL,
  CONSTRAINT pk_my_table PRIMARY KEY (my_uuid));

ALTER TABLE my_table ADD my_other_uuid CHAR (16) FOR BIT DATA
  NOT NULL DEFAULT X'0000';

INSERT INTO my_table (my_uuid) VALUES (X'0123');