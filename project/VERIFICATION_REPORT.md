# Verification report — Digital Complaint Priority System

## Tests performed

1. **Project/source inspection — passed.** Inspected the submission JSP, `SubmitComplaintServlet`, `ComplaintService`, `ComplaintDAO`, `Complaint` model, `DBConnection`, database properties/schema, student complaint list/controller, student dashboard/controller, administrator dashboard/controller, authentication filters, deployment descriptor, Maven configuration, and project documentation.
2. **Submission contract static check — passed.** The form posts to `/submit-complaint`; `title`, `category`, `description`, and `location` form names match the servlet parameters.
3. **View flow static check — passed.** Student listing filters by the authenticated user's ID. Administrator listing calls `getAllComplaints()`. Both use the same DAO and configured `DBConnection` against `complaints`.
4. **Targeted Java compilation — passed.** Compiled the modified DAO, model, DB connection, service classes, and submission servlet with JDK 21. The servlet was compiled against temporary minimal Jakarta Servlet API stubs because the actual Maven dependencies were not available locally. This is a syntax/type check, not a full dependency-integrated build.
5. **Non-destructive-change check — passed.** Searched source and schema scripts for `DROP DATABASE`, `DROP TABLE`, `TRUNCATE`, destructive complaint deletes, and complaint-ID rewrite statements. The revised sequence initializer no longer rewrites any existing complaint IDs. No SQL was executed.
6. **Archive integrity — to be checked during packaging.**

## Tests not performed

- **Full Maven `clean package`: not performed.** Maven is not installed/available in this environment.
- **Live MySQL submission and readback: not performed.** No MySQL client/server connection to the user's configured database is available here. Therefore I cannot claim that a new complaint was actually committed to the user's database.
- **Browser/JSP rendering test: not performed.** No servlet container/JSP engine is running here.
- **Tomcat deployment: not performed.** The existing Tomcat installation is not accessible and was not modified.

## Root-cause conclusion

The code has a concrete submission-blocking risk: on the first submission, `ComplaintDAO.insert()` calls `ensureSequence()`, which previously migrated existing complaint primary keys and deliberately aborted if another table had a foreign key to `complaints.id`. It also returned a generic failure after swallowing SQL exceptions. This can prevent a new complaint from being saved on databases with such a foreign key. The live database condition could not be checked, so this is a confirmed defect/risk in the source, not a verified diagnosis of the exact live runtime failure.

The repair keeps existing complaint IDs unchanged, initializes only the separate sequence counter, performs the new complaint insert transactionally, and logs real submission errors server-side. Confirm the actual result by building and deploying locally, submitting one complaint with a student account, then checking **My complaints**, the student dashboard, and the administrator dashboard. No test complaint was inserted by this repair.
