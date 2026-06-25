# MotorPH Payroll System

MO-IT113 - Advanced Object-oriented Programming  
MO-IT111 - Database Principles and Applications  
Term 3 SY 2025-26

## Repository Reference

This repository contains the finalized implementation and consolidated submission of the MotorPH Payroll System project.

For development history, collaboration progress, and earlier implementation commits contributed during the project development phase, please also refer to the repository maintained by project collaborator Claire Helery Noel:

- [MotorPH Payroll System V2 Repository](https://github.com/chnoel09/motorph-payroll-systemV2)

---

## Project Overview

The MotorPH Payroll System is a Java Swing payroll and workforce management application for the MotorPH case study. The project demonstrates an object-oriented, database-driven implementation with layered responsibilities for user interface, business logic, persistence, and reporting.

The current implementation uses MySQL through JDBC repositories. CSV files are no longer used for application persistence. CSV output is available only as an export format where applicable, such as payroll history export.

## Architecture

The application follows this layered structure:

```text
View -> Service -> Repository -> Database
```

### View Layer

Swing screens and dialogs handle user interaction and display. Examples include:

- `LoginFrame`
- `MainAppFrame`
- `DashboardPanel`
- `EmployeePanel`
- `AttendancePanel`
- `LeavePanel`
- `PayrollPanel`
- `HRPayrollHistoryPanel`
- `PayslipPanel`

### Service Layer

Service classes coordinate workflows, validation, and business rules. Examples include:

- `LoginService`
- `EmployeeService`
- `AttendanceService`
- `LeaveService`
- `PayrollService`
- `PayrollProcessor`
- `ReportService`
- `RoleAccessService`
- `WorkforceReadinessService`

### Repository Layer

Repository interfaces and JDBC implementations isolate database access from business logic. Examples include:

- `EmployeeRepository` / `EmployeeDatabaseRepository`
- `UserRepository` / `UserDatabaseRepository`
- `AttendanceRepository` / `AttendanceDatabaseRepository`
- `LeaveRepository` / `LeaveDatabaseRepository`
- `PayrollHistoryRepository` / `PayrollHistoryDatabaseRepository`
- `PayrollRunRepository` / `PayrollRunDatabaseRepository`
- `PayrollRunDetailRepository` / `PayrollRunDetailDatabaseRepository`

### Database Layer

The database schema is maintained in:

- `database/motorph_db.sql`
- `database/motorph_db.dbml`

These files define the current MySQL schema used by the application.

## Major Modules

- Login and authentication
- Role-based access control
- Employee management
- Attendance tracking
- Leave management
- Payroll processing
- Payroll history
- Dashboard summaries
- JasperReports report generation

## Role-Based Access Control

Authentication and access control use normalized RBAC tables:

- `users`
- `roles`
- `permissions`
- `user_roles`
- `role_permissions`

The login flow validates user credentials against `users.password_hash`, then resolves role access through the RBAC tables.

## Payroll and Workforce Data

Employee profile data is stored in normalized database tables. Employee government IDs are stored separately from the main employee record.

Payroll processing uses database-backed attendance, employee, payroll period, payroll run, payroll run detail, deduction, and payroll history records. Payroll history is treated as persisted payroll snapshot data for reporting and review.

## Reports

The application uses JasperReports for printable PDF reports. Report templates are stored in `src/main/resources/reports/`.

Current report templates:

- Payslip report: `payslip.jrxml`
- Timecard report: `timecard.jrxml`
- Payroll summary report: `payroll_summary.jrxml`

The report service uses JDBC connections and the JasperReports library to fill and export reports.

## AOOP Principles Demonstrated

### Encapsulation

Model classes keep fields private and expose controlled getters and setters.

### Abstraction

Interfaces such as `Payables` and repository interfaces define contracts without exposing implementation details.

### Inheritance

`RegularEmployee` extends the abstract `Employee` class.

### Polymorphism

Repository implementations are used through repository interfaces. Payroll-related model behavior is exposed through the `Payables` interface.

### Interfaces

The project uses interfaces to separate behavior contracts from implementation, especially in the repository layer.

### Layered Architecture

The application separates Swing views, service logic, repository persistence, and database schema responsibilities.

## Unit Tests

JUnit 5 starter tests are included under `src/test/java/`.

Current test classes:

- `PasswordUtilTest`
- `PayrollProcessorTest`
- `RegularEmployeeTest`

These tests focus on deterministic business and model behavior and do not require a live MySQL connection.

## How to Build and Test

Compile the project:

```bash
mvn clean compile
```

Run unit tests:

```bash
mvn test
```

## How to Run

1. Install and start MySQL.
2. Import the schema from `database/motorph_db.sql`.
3. Confirm database connection settings in `src/main/java/com/mycompany/oop/DatabaseConnection.java`.
4. Run the application using the configured Maven main class:

```bash
mvn exec:java
```

## Documentation Artifacts

Class diagram files are stored in:

- `docs/diagrams/motorph_class_diagram.puml`
- `docs/diagrams/motorph_class_diagram.png`

## Group Information

Section S2101

Members:

- Rosephil Muros
- Claire Helery Noel

## Final Notes

This project is prepared as an AOOP and DPA implementation of the MotorPH Payroll System. It demonstrates Java Swing UI development, object-oriented design, JDBC-based persistence, normalized database integration, report generation, and starter unit testing.
