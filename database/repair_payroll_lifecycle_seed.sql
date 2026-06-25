-- MotorPH AOOP payroll lifecycle seed repair
-- Purpose:
--   Complete the payroll lifecycle chain for existing processed payroll_history
--   rows in the demo seed data.
--
-- Expected chain:
--   payroll_periods
--   -> payroll_runs
--   -> payroll_run_details
--   -> payroll_deductions
--   -> payroll_history
--
-- Scope:
--   Repairs period_id = 1 only.
--   Uses the latest payroll_runs row for period_id = 1.
--   Does not create payroll_history rows or modify payroll formulas.

START TRANSACTION;

SET @repair_period_id := 1;

SELECT @repair_run_id := pr.run_id
FROM payroll_runs pr
WHERE pr.period_id = @repair_period_id
ORDER BY pr.processed_at DESC, pr.run_id DESC
LIMIT 1;

-- Create run details for processed history rows that are not yet linked.
INSERT INTO payroll_run_details (
    run_id,
    employee_id,
    hours_worked,
    overtime_hours,
    basic_component,
    allowance_component
)
SELECT
    @repair_run_id,
    ph.employee_id,
    COALESCE(ph.hours_worked, 0.00),
    0.00,
    ph.basic_component,
    ph.allowance_component
FROM payroll_history ph
JOIN payroll_periods pp
  ON pp.cutoff_period = ph.cutoff_period
 AND pp.period_start = ph.period_start
 AND pp.period_end = ph.period_end
LEFT JOIN payroll_run_details existing_detail
  ON existing_detail.run_id = @repair_run_id
 AND existing_detail.employee_id = ph.employee_id
WHERE pp.period_id = @repair_period_id
  AND ph.run_detail_id IS NULL
  AND @repair_run_id IS NOT NULL
  AND existing_detail.run_detail_id IS NULL;

-- Link payroll_history snapshots to the matching run details.
UPDATE payroll_history ph
JOIN payroll_periods pp
  ON pp.cutoff_period = ph.cutoff_period
 AND pp.period_start = ph.period_start
 AND pp.period_end = ph.period_end
JOIN payroll_run_details prd
  ON prd.run_id = @repair_run_id
 AND prd.employee_id = ph.employee_id
SET ph.run_detail_id = prd.run_detail_id
WHERE pp.period_id = @repair_period_id
  AND ph.run_detail_id IS NULL
  AND @repair_run_id IS NOT NULL;

-- Persist statutory deduction breakdowns from payroll_history snapshots.
INSERT INTO payroll_deductions (
    run_detail_id,
    deduction_id,
    deduction_amount,
    remarks
)
SELECT
    ph.run_detail_id,
    d.deduction_id,
    CASE d.deduction_name
        WHEN 'SSS' THEN COALESCE(ph.sss, 0.00)
        WHEN 'PhilHealth' THEN COALESCE(ph.philhealth, 0.00)
        WHEN 'Pag-IBIG' THEN COALESCE(ph.pagibig, 0.00)
        WHEN 'Tax' THEN COALESCE(ph.tax, 0.00)
        ELSE 0.00
    END AS deduction_amount,
    'Seed repair from payroll_history snapshot'
FROM payroll_history ph
JOIN payroll_periods pp
  ON pp.cutoff_period = ph.cutoff_period
 AND pp.period_start = ph.period_start
 AND pp.period_end = ph.period_end
JOIN payroll_run_details prd
  ON prd.run_detail_id = ph.run_detail_id
 AND prd.run_id = @repair_run_id
JOIN deductions d
  ON d.deduction_name IN ('SSS', 'PhilHealth', 'Pag-IBIG', 'Tax')
WHERE pp.period_id = @repair_period_id
  AND ph.run_detail_id IS NOT NULL
  AND @repair_run_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    deduction_amount = VALUES(deduction_amount),
    remarks = VALUES(remarks);

COMMIT;

-- Verification: latest run selected for repair.
SELECT
    @repair_period_id AS repaired_period_id,
    @repair_run_id AS repaired_run_id;

-- Verification: run detail rows for the repaired period.
SELECT
    prd.run_detail_id,
    prd.run_id,
    prd.employee_id,
    prd.hours_worked,
    prd.basic_component,
    prd.allowance_component
FROM payroll_run_details prd
WHERE prd.run_id = @repair_run_id
ORDER BY prd.employee_id;

-- Verification: payroll_history rows should now have non-null run_detail_id values.
SELECT
    ph.history_id,
    ph.run_detail_id,
    ph.employee_id,
    ph.cutoff_period,
    ph.gross,
    ph.total_deductions,
    ph.net
FROM payroll_history ph
JOIN payroll_periods pp
  ON pp.cutoff_period = ph.cutoff_period
 AND pp.period_start = ph.period_start
 AND pp.period_end = ph.period_end
WHERE pp.period_id = @repair_period_id
ORDER BY ph.employee_id;

-- Verification: deduction breakdown rows linked to run details.
SELECT
    prd.employee_id,
    pd.run_detail_id,
    d.deduction_name,
    pd.deduction_amount,
    pd.remarks
FROM payroll_deductions pd
JOIN payroll_run_details prd
  ON prd.run_detail_id = pd.run_detail_id
JOIN deductions d
  ON d.deduction_id = pd.deduction_id
WHERE prd.run_id = @repair_run_id
ORDER BY prd.employee_id, d.deduction_name;
