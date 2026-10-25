# Repair notes — Digital Complaint Priority System

## Original defects

### Complaint list inconsistency

The administrator dashboard count queries read from `complaints`, while the original list retrieval could run an ID migration during a read and convert migration failures into an empty list. The list also used an `INNER JOIN users`, which could hide complaint rows lacking a matching user row. The existing correction removes migration from read-only list/detail operations, uses `LEFT JOIN`, and surfaces database retrieval errors rather than representing them as empty results.

### Complaint submission failure risk

The submission path is `submit-complaint.jsp` → `SubmitComplaintServlet` → `ComplaintService.submitComplaint()` → `ComplaintDAO.insert()` → MySQL `complaints` table. The form field names and servlet parameter names match, and the servlet mapping is `/submit-complaint`.

The original `ComplaintDAO.ensureSequence()` performed a legacy-ID migration before every first insert in a JVM. It rewrote existing complaint primary keys and deliberately failed if another table had a foreign key referencing `complaints.id`. That could block all new submissions on databases with such references, despite the submission needing only a next-ID counter. The insert path also swallowed SQL exceptions into `false`, leaving the user with a generic message and only a stack trace in logs.

## Changes made

- `src/main/java/com/campus/complaint/dao/ComplaintDAO.java`
  - Kept the previous pending/listing fix: read-only methods do not initialize/migrate the ID sequence and list queries use `LEFT JOIN`.
  - Reworked sequence initialization to create/advance only the dedicated `complaint_sequence` row, using the highest existing `CMP-<number>` ID as a lower bound. It no longer rewrites, renumbers, or otherwise updates existing complaint records, and it no longer blocks submission due to foreign keys referencing complaint IDs.
  - Kept sequence increment and complaint insert in one transaction. Insert SQL failures now throw a descriptive application exception with the SQL exception as its cause, rather than silently returning `false`.
- `src/main/java/com/campus/complaint/servlet/SubmitComplaintServlet.java`
  - Logs real submission failures through the servlet context, without displaying SQL/credential details to the browser.
  - Shows a safe user-facing error and retains entered form values after validation or database failures.
- `src/main/webapp/submit-complaint.jsp`
  - Retains the submitted title/category/description/location on failure; existing form action and field names are preserved.
- `README.md`
  - Updated with safe build/deployment and manual verification instructions. It explicitly warns not to run initialization SQL blindly against existing data.

## Database and deployment protection

No database connection was available in this environment, and no SQL script was executed. No database, table, or complaint record was deleted, reset, or overwritten. The revised sequence initialization may create `complaint_sequence` if it does not exist and initialize/update its counter row; it does not change existing complaint rows. The configured `db.properties` was not changed. Review it locally and confirm it targets the intended database before deployment.

This is a Servlet/JSP WAR project using Jakarta Servlet 6 / JSP 3.1 and is intended for Tomcat 10.1. No Tomcat files were touched.

## Verification performed

- Source inspection: submission form, `SubmitComplaintServlet`, `ComplaintService`, `ComplaintDAO`, complaint model, database configuration/schema, student complaint view/controller, administrator dashboard view/controller, filters, and deployment descriptor inspected.
- Static contract checks: form action matches `/submit-complaint`; the four form names match the servlet parameter names; the student view queries by the authenticated session user's ID; the administrator view calls `getAllComplaints()`; both views are backed by the same `complaints` table and `DBConnection` configuration. The code path is consistent, but a saved-row readback was not possible without MySQL.
- Java compilation: passed for `Complaint`, `User`, `DBConnection`, `ComplaintDAO`, `PriorityService`, `ComplaintService`, and `SubmitComplaintServlet` against temporary minimal Jakarta Servlet API stubs. This verifies Java syntax/type use in the edited submission path but is not a substitute for compiling against the actual Maven dependencies.
- Live database insert/readback, browser submission, full Maven build, and Tomcat deployment: not performed in this environment. Do not treat those as passed.
