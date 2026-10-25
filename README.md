# Digital Complaint Priority System

A Java web application for managing campus complaints from submission through resolution. Students can report an issue and follow its progress; administrators can review all reports, assign staff, update statuses, and export a CSV report. A rule-based priority service classifies each new complaint, and a Java `PriorityQueue` demonstrates how the most urgent items can be ordered first.

## Features

- Separate student and administrator experiences with session-based sign-in.
- Student dashboard with complaint totals and recent submissions.
- Complaint form with title, category, description, and location.
- Automatic `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW` priority assignment on the server.
- Student complaint history and status tracking.
- Administrator dashboard with complaint and status counts.
- Staff assignment and status updates: `PENDING`, `ASSIGNED`, `IN_PROGRESS`, and `RESOLVED`.
- In-memory priority queue ordered by priority, then submission time and complaint ID.
- Administrator-only CSV export.
- MySQL persistence using JDBC and prepared statements.
- BCrypt password hashing for the seeded demo accounts.

## How priority is assigned

Priority is calculated from the complaint's category, title, description, and location. Students do not select their own priority. The classifier uses explicit keyword and category rules; it is not a machine-learning model.

| Priority | Example signals |
| --- | --- |
| `CRITICAL` | Exposed or live wires, sparks, smoke, fire, gas leaks, flooding, or other safety hazards |
| `HIGH` | Water leaks or bursts, security issues, broken locks, power outages, or major service disruption |
| `MEDIUM` | Common classroom, hostel, or equipment maintenance issues such as a noisy fan or broken furniture |
| `LOW` | Routine cleaning and minor maintenance, or issues that do not match a higher-priority rule |

The administrator dashboard also builds a Java `PriorityQueue` from the complaint list. When polled, it places higher priorities first, then earlier submissions, then complaint IDs. The dashboard table itself displays the retrieved complaint list.

## Technology

- **Language:** Java 11+
- **Web:** Jakarta Servlets 6.0, JSP 3.1, Jakarta Tags (JSTL) 3.0
- **Database:** MySQL 8+
- **Build:** Apache Maven; packaged as a WAR
- **Application server:** Apache Tomcat 10.1
- **Libraries:** MySQL Connector/J, jBCrypt, JUnit 5
- **Interface:** JSP, HTML, CSS, and JavaScript

## Requirements

- Java JDK 11 or newer
- MySQL Server 8 or newer
- Apache Tomcat 10.1 for manual deployment
- Apache Maven for a manual build

The downloadable package contains Maven, Tomcat, and a Maven dependency cache. Java and MySQL are not bundled. `start.command` is for macOS; on Windows, use the PowerShell commands below with the bundled Windows Maven and Tomcat launchers.

## Run the bundled package on macOS

1. Extract the complete `Digital-Complaint-System-Package` folder to a location where you can write files. Keep the `project`, `tools`, and `start.command` items together.
2. Start MySQL Server.
3. Open `project/src/main/resources/db.properties` and set `db.url`, `db.user`, and `db.password` to match your local MySQL setup. The default schema name is `campus_complaints`.
4. For a **new demo database**, open `project/src/main/resources/db.sql` in MySQL Workbench and run it once. It creates the schema and tables and inserts demo accounts and complaints.
5. From the extracted package folder, run:

   ```bash
   ./start.command
   ```

   If macOS does not launch the script when double-clicked, open Terminal, change to the extracted package folder, and run the command above.

6. The script builds the WAR, starts Tomcat, and opens the login page:

   <http://localhost:8080/digital-complaint-system/login.jsp>

Keep the Terminal window open while using the application. Press **Control+C** in that window to stop Tomcat. If port `8080` is already in use, stop the other local server first.

## Run the bundled package on Windows

1. Extract the complete `Digital-Complaint-System-Package` folder to a location where you can write files. Keep `project`, `tools`, and the other package files together.
2. Install Java JDK 11 or newer and MySQL Server 8 or newer. Make sure Java is available in PowerShell; if needed, set `JAVA_HOME` to your JDK installation.
3. Start MySQL. Open `project/src/main/resources/db.properties` and set `db.url`, `db.user`, and `db.password` for your MySQL setup.
4. For a **new demo database**, run `project/src/main/resources/db.sql` once in MySQL Workbench. Read the [database safety note](#database-safety) before running it against an existing database.
5. Open PowerShell and change to the extracted package folder. Build the project offline using the bundled Maven and dependency cache:

   ```powershell
   $mavenRepo = (Resolve-Path .\tools\maven-repository).Path
   .\tools\apache-maven-3.9.16\bin\mvn.cmd -o "-Dmaven.repo.local=$mavenRepo" -f .\project\pom.xml -DskipTests package
   ```

6. Copy the generated WAR into the bundled Tomcat webapps folder:

   ```powershell
   Copy-Item .\project\target\digital-complaint-system.war .\tools\apache-tomcat-10.1.61\webapps\digital-complaint-system.war -Force
   ```

7. Start Tomcat in the same PowerShell window:

   ```powershell
   .\tools\apache-tomcat-10.1.61\bin\catalina.bat run
   ```

8. When Tomcat has started, open <http://localhost:8080/digital-complaint-system/login.jsp> in your browser.

Keep PowerShell open while using the application. Press **Ctrl+C** in that window to stop Tomcat. If port `8080` is already in use, stop the other local server first. The `start.command` file is a macOS shell script and is not used on Windows.

### Database safety

Do not run `db.sql` blindly against an existing or important database. Although it does not drop the database or tables, it refreshes the seeded demo users' password hashes and adds demo records where their IDs are unused. Review the script and back up the database before using it on data you need to keep. The application does not execute `db.sql` automatically.

## Build and deploy manually

From the `project` directory, build the WAR with Maven:

```bash
mvn clean package
```

The output is `target/digital-complaint-system.war`. Deploy that WAR to a Tomcat 10.1 server and start Tomcat. The application context is `digital-complaint-system`, so the login page is:

```text
http://localhost:8080/digital-complaint-system/login.jsp
```

To run the unit tests separately:

```bash
mvn test
```

The bundled `start.command` uses offline Maven and skips tests while packaging.

## Demo accounts

These accounts are seeded by `db.sql` and work after the demo database has been initialized:

| Role | Username | Password |
| --- | --- | --- |
| Administrator | `admin` | `password` |
| Student | `student1` | `password` |
| Student | `student2` | `password` |

These are demonstration credentials. Change them before using the application with real users or data, and do not reuse the demo password.

## Typical workflow

### Student

1. Sign in with a student account.
2. Open **Submit complaint**, describe the issue, choose a category, and enter its location.
3. Submit the complaint. The server assigns its ID, priority, and initial `PENDING` status.
4. View it on **My complaints** or the student dashboard to follow status and staff assignment.

### Administrator

1. Sign in with an administrator account.
2. Review complaint totals, statuses, priorities, and complaint details on the dashboard.
3. Assign a staff member and update the complaint status.
4. Use **Export CSV** to download the complaint report.

## Project layout

```text
project/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/campus/complaint/
    │   │   ├── dao/          # JDBC data access
    │   │   ├── filter/       # Login and administrator access checks
    │   │   ├── model/        # User and complaint models
    │   │   ├── service/      # Authentication, complaint, export, and priority logic
    │   │   ├── servlet/      # HTTP request handlers
    │   │   └── util/         # Database connection helper
    │   ├── resources/
    │   │   ├── db.properties # MySQL connection settings
    │   │   └── db.sql        # Schema and demo data setup
    │   └── webapp/           # JSP pages, CSS, JavaScript, and web.xml
    └── test/java/            # JUnit tests
```

## Security and deployment notes

- Students see their own complaints; administrator pages and actions require the administrator role.
- Database operations use parameterized statements, and seeded passwords are stored as BCrypt hashes.
- Keep database credentials private. Do not commit real credentials or use demo accounts in a live environment.
- This project is intended for learning and local demonstration. A production deployment would need a security and operations review, including HTTPS, production credential management, and appropriate account administration.

## License

No license file is included. Ask the project owner before reusing or redistributing this project.
