--
-- Copyright 2010-2017 Boxfuse GmbH
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


CREATE TABLE test_user (
  id INT NOT NULL,
  name VARCHAR(25) NOT NULL,
  PRIMARY KEY(name)
);

CREATE MATERIALIZED VIEW user_data REFRESH WITH ROWID
   AS SELECT * FROM test_user;

CREATE MATERIALIZED VIEW LOG ON test_user
   WITH PRIMARY KEY
   INCLUDING NEW VALUES;

CREATE MATERIALIZED VIEW more_user_data
   REFRESH FAST NEXT sysdate + 7
   AS SELECT * FROM test_user; 