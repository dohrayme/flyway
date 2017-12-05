--
-- Copyright 2010-2017 Boxfuse GmbH
--
-- INTERNAL RELEASE. ALL RIGHTS RESERVED.
--

CREATE TABLE usertable (
  id INT NOT NULL,
  name VARCHAR(25) NOT NULL,  -- this is a valid comment
  PRIMARY KEY(name)
);

CREATE TRIGGER uniqueidx_trigger BEFORE INSERT ON usertable
	REFERENCING NEW ROW AS newrow
    FOR EACH ROW WHEN (newrow.name is not null)
	BEGIN ATOMIC
      IF EXISTS (SELECT * FROM usertable WHERE usertable.name = newrow.name) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'duplicate name';
      END IF;
    END;
