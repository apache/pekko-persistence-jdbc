-- (ddl lock timeout in seconds) this allows tests which are still writing to the db to finish gracefully
ALTER SESSION SET ddl_lock_timeout = 150
/

DROP TRIGGER JOURNAL__ORDERING_TRG
/

DROP PROCEDURE "reset_legacy_sequence"
/

DROP SEQUENCE ORDERING__SEQ
/

DROP TABLE JOURNAL CASCADE CONSTRAINT
/

DROP TABLE LEGACY_SNAPSHOT CASCADE CONSTRAINT
/
