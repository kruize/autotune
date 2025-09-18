BEGIN;

DELETE FROM kruize_performance_profiles kpp
WHERE kpp.profile_version < 2;

COMMIT;
