-- MotorPH historical attendance payroll governance seed
-- Creates canonical payroll periods and initial readiness rows from completed attendance.

SET SESSION lock_wait_timeout = 10;

-- Stop when date-range duplicates must be resolved manually before governance seeding.
SELECT COUNT(*) INTO @duplicate_period_ranges
FROM (
    SELECT period_start, period_end
    FROM payroll_periods
    GROUP BY period_start, period_end
    HAVING COUNT(*) > 1
) duplicate_ranges;

SET @duplicate_guard_sql := IF(
    @duplicate_period_ranges = 0,
    'DO 0',
    'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Duplicate payroll period date ranges detected'''
);
PREPARE duplicate_guard_statement FROM @duplicate_guard_sql;
EXECUTE duplicate_guard_statement;
DEALLOCATE PREPARE duplicate_guard_statement;

-- Add date-range uniqueness for databases created before this migration.
SELECT COUNT(*) INTO @has_period_range_key
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'payroll_periods'
  AND index_name = 'uq_payroll_period_date_range';

SET @range_key_sql := IF(
    @has_period_range_key = 0,
    'ALTER TABLE payroll_periods ADD UNIQUE KEY uq_payroll_period_date_range (period_start, period_end)',
    'DO 0'
);
PREPARE range_key_statement FROM @range_key_sql;
EXECUTE range_key_statement;
DEALLOCATE PREPARE range_key_statement;

START TRANSACTION;

SELECT COUNT(*) INTO @periods_before
FROM payroll_periods;

SELECT COUNT(*) INTO @payroll_history_before
FROM payroll_history;

DROP TEMPORARY TABLE IF EXISTS historical_attendance_cutoffs;
CREATE TEMPORARY TABLE historical_attendance_cutoffs AS
SELECT
    CONCAT(
        DATE_FORMAT(attendance_date, '%b-%Y'),
        IF(DAY(attendance_date) <= 15, '-1st', '-2nd')
    ) AS cutoff_period,
    CASE
        WHEN DAY(attendance_date) <= 15
            THEN DATE_SUB(attendance_date, INTERVAL DAY(attendance_date) - 1 DAY)
        ELSE DATE_ADD(
            DATE_SUB(attendance_date, INTERVAL DAY(attendance_date) - 1 DAY),
            INTERVAL 15 DAY
        )
    END AS period_start,
    CASE
        WHEN DAY(attendance_date) <= 15
            THEN DATE_ADD(
                DATE_SUB(attendance_date, INTERVAL DAY(attendance_date) - 1 DAY),
                INTERVAL 14 DAY
            )
        ELSE LAST_DAY(attendance_date)
    END AS period_end
FROM attendance_records
GROUP BY cutoff_period, period_start, period_end;

INSERT INTO payroll_periods (
    cutoff_period,
    period_start,
    period_end,
    status
)
SELECT
    cutoff_period,
    period_start,
    period_end,
    'OPEN_WORKFORCE_REVIEW'
FROM historical_attendance_cutoffs
ON DUPLICATE KEY UPDATE
    cutoff_period = VALUES(cutoff_period);

SELECT COUNT(*) - @periods_before INTO @payroll_periods_created
FROM payroll_periods;

-- Keep existing payroll snapshots aligned when a legacy full-month label is normalized.
UPDATE payroll_history ph
JOIN historical_attendance_cutoffs hac
  ON hac.period_start = ph.period_start
 AND hac.period_end = ph.period_end
SET ph.cutoff_period = hac.cutoff_period
WHERE ph.cutoff_period <> hac.cutoff_period;

-- Readiness is valid only for employees with attendance inside the payroll period.
DELETE wpr
FROM workforce_payroll_readiness wpr
JOIN payroll_periods pp
  ON pp.period_id = wpr.payroll_period_id
WHERE NOT EXISTS (
    SELECT 1
    FROM attendance_records ar
    WHERE ar.employee_id = wpr.employee_id
      AND ar.attendance_date BETWEEN pp.period_start AND pp.period_end
);

INSERT INTO workforce_payroll_readiness (
    payroll_period_id,
    employee_id,
    readiness_status,
    current_owner_role,
    supervisor_employee_id
)
SELECT DISTINCT
    pp.period_id,
    ar.employee_id,
    'OPEN_WORKFORCE_ISSUES',
    CASE
        WHEN e.supervisor_employee_id IS NULL THEN 'HR'
        ELSE 'SUPERVISOR'
    END,
    e.supervisor_employee_id
FROM payroll_periods pp
JOIN attendance_records ar
  ON ar.attendance_date BETWEEN pp.period_start AND pp.period_end
JOIN employees e
  ON e.employee_id = ar.employee_id
JOIN historical_attendance_cutoffs hac
  ON hac.period_start = pp.period_start
 AND hac.period_end = pp.period_end
ON DUPLICATE KEY UPDATE
    readiness_id = readiness_id;

SET @readiness_rows_created := ROW_COUNT();

DROP TEMPORARY TABLE historical_attendance_cutoffs;

COMMIT;

SELECT
    @payroll_periods_created AS payroll_periods_created,
    @readiness_rows_created AS readiness_rows_created,
    @payroll_history_before AS payroll_history_before,
    (SELECT COUNT(*) FROM payroll_history) AS payroll_history_after;

-- Audit report: periods and attendance-backed readiness created by cutoff.
SELECT
    pp.period_id,
    pp.cutoff_period,
    pp.period_start,
    pp.period_end,
    pp.status,
    COUNT(DISTINCT wpr.employee_id) AS readiness_employees,
    COUNT(DISTINCT ar.employee_id) AS attendance_employees
FROM payroll_periods pp
LEFT JOIN workforce_payroll_readiness wpr
  ON wpr.payroll_period_id = pp.period_id
LEFT JOIN attendance_records ar
  ON ar.attendance_date BETWEEN pp.period_start AND pp.period_end
GROUP BY
    pp.period_id,
    pp.cutoff_period,
    pp.period_start,
    pp.period_end,
    pp.status
ORDER BY pp.period_start;

-- Audit report: pre-existing processed approvals remain visible; newly seeded rows are unset.
SELECT
    pp.cutoff_period,
    COUNT(*) AS readiness_rows,
    SUM(wpr.supervisor_cleared_at IS NOT NULL) AS supervisor_approvals,
    SUM(wpr.hr_validated_at IS NOT NULL) AS hr_approvals,
    SUM(wpr.finance_validated_at IS NOT NULL) AS finance_approvals
FROM workforce_payroll_readiness wpr
JOIN payroll_periods pp
  ON pp.period_id = wpr.payroll_period_id
GROUP BY pp.cutoff_period, pp.period_start
ORDER BY pp.period_start;

-- Audit report: this query must return no duplicate period date ranges.
SELECT
    period_start,
    period_end,
    COUNT(*) AS duplicate_count,
    GROUP_CONCAT(cutoff_period ORDER BY cutoff_period) AS cutoff_labels
FROM payroll_periods
GROUP BY period_start, period_end
HAVING COUNT(*) > 1;
