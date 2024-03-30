ALTER SESSION SET ddl_lock_timeout = 15
/

DROP TABLE EVENT_TAG CASCADE CONSTRAINT
/

DROP TABLE EVENT_JOURNAL CASCADE CONSTRAINT
/

DROP TABLE SNAPSHOT CASCADE CONSTRAINT
/

DROP TABLE DURABLE_STATE CASCADE CONSTRAINT
/

DROP SEQUENCE EVENT_JOURNAL__ORDERING_SEQ
/

DROP TRIGGER EVENT_JOURNAL__ORDERING_TRG
/

DROP TRIGGER DURABLE_STATE__GLOBAL_OFFSET_SEQ
/
