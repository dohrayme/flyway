--
-- Copyright 2010-2017 Boxfuse GmbH
--
-- INTERNAL RELEASE. ALL RIGHTS RESERVED.
--

CREATE TABLE couple (
  id INT NOT NULL,
  name1 VARCHAR(25) NOT NULL,
  name2 VARCHAR(25) NOT NULL,
  PRIMARY KEY(id),
  CONSTRAINT couple_user1_fk FOREIGN KEY (name1) REFERENCES test_user(name),
  CONSTRAINT couple_user2_fk FOREIGN KEY (name2) REFERENCES test_user(name)
);