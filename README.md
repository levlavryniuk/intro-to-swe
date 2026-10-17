# Admissions, Recruitment & Outreach Platform (Team 4)

Team 4's module of the AUK university management platform: inquiries, event registration,
applications with documents, review and decisions, email to leads, and the pipeline overview.
Team: Bogdan Borachuk, Lev Lavryniuk, Antonii Maltsev.

This is the **Phase II initial prototype**. It is built in Java 17 with Spring Boot 3 and MySQL,
and has a REST API with simple HTML pages for the demo.

## Repository layout

```
pom.xml, mvnw, mvnw.cmd   Maven build (the wrapper downloads Maven, nothing to install but a JDK)
src/main/java/...         the module (see "Code structure" below)
src/main/resources/       configuration and the web pages (static/)
src/test/java/...         automated tests (JUnit 5)
docs/                     reports, team journals, time logs, meeting links
docs/phase-2/             Phase II: functional test plan
```

## Running it

You need **JDK 17 or newer** (`java -version`). The Maven wrapper does the rest.

### With MySQL (the project's database)

1. Start MySQL 8 and make sure you know a user that can create a database.
2. Run (the `admissions` database is created on first start):

   ```bash
   DB_USER=root DB_PASSWORD=yourpassword ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

   On Windows PowerShell:

   ```powershell
   $env:DB_USER="root"; $env:DB_PASSWORD="yourpassword"; .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```

   `DB_HOST`, `DB_PORT` and `DB_NAME` can be set the same way (defaults: `localhost`, `3306`, `admissions`).

### Quick start without MySQL

```bash
./mvnw spring-boot:run
```

This uses an in-memory H2 database, so all data is gone when the app stops. Handy for a quick demo.

Then open <http://localhost:8080>.

### Demo accounts

Demo data (programs, events, inquiries, three sample applications) is loaded on first start.
These are test values for the prototype only, set in `src/main/resources/application.properties`:

| Role | Email | Password |
|---|---|---|
| Admissions staff | `staff@auk.edu.ua` | `staff-demo-2026` |
| Applicant (no application yet) | `applicant@example.com` | `applicant-demo-2026` |

Anyone can also create a new applicant account on the site.

## Pages

| Page | Who | Use cases |
|---|---|---|
| `/` | Visitor | UC-1 Submit Inquiry, UC-2 Register for Event |
| `/signup.html` | Visitor | creates an applicant account (local sign-in) |
| `/login.html` | Applicant, staff | sign in with email address and password |
| `/applicant.html` | Applicant | UC-3 Submit, UC-4 Edit, UC-5 Withdraw, UC-6 Track Application Status |
| `/staff.html` | Staff | UC-7 to UC-14, and UC-15 runs when an applicant is accepted |

## Code structure

The classes follow the Phase II class diagram (`docs/` report, section "Class Diagram").

| Package | Contents |
|---|---|
| `domain` | `User`, `Applicant`, `StaffMember`, `Lead`, `Inquiry`, `Program`, `Event`, `EventRegistration`, `Application`, `Document`, `StatusChange`, `EmailMessage`, `PipelineReport` and the enums. The rules live here, e.g. `Application.changeStatus` writes the decision log entry and `Program.isOpen` decides the deadline. |
| `repository` | Spring Data JPA repositories (MySQL / H2). |
| `service` | One service per group of use cases; they load data, call the domain methods and save. `DocumentStorage` checks uploads (PDF, at most 15 MB) and keeps files in a private folder. |
| `integration` | `MailGateway` and `SisGateway` from the class diagram. For now mail is written to the log (or sent to a test SMTP server with `app.mail.mode=smtp`) and the SIS is a stub, until Team 2 and Team 5 publish their APIs. |
| `security` | Local sign-in (Spring Security) until the Core Platform's shared sign-in exists. Kept out of `domain` on purpose. |
| `web` | REST controllers (`/api/public`, `/api/applicant`, `/api/staff`) and request/response records. |

The operations the class diagram puts on `Applicant` and `StaffMember` (for example
`StaffMember.recordDecision`) are carried out by the services on behalf of the signed-in user.

## Tests

```bash
./mvnw test
```

80 tests: unit tests of the domain rules, sign-in tests, and tests that start the whole module and call the REST API
the way the pages do. Each test name starts with the test case ID from the functional test plan
(`docs/phase-2/functional-test-plan.docx`), e.g. `TC-3.4 A file that is not a PDF is rejected`.

## Settings worth knowing

| Property | Default | Meaning |
|---|---|---|
| `app.storage.dir` | `./data/uploads` | where uploaded documents are kept (not served publicly) |
| `app.mail.mode` | `log` | `log` prints emails; `smtp` sends via `spring.mail.host` |
| `app.sis.available` | `true` | set `false` to see the warning when the SIS cannot be reached |
| `app.demo-data` | `true` | load the demo data on first start |
