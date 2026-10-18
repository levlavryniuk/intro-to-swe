# Admissions, Recruitment & Outreach Platform (Team 4)

Team 4's module of the AUK university management platform: inquiries, event registration,
applications with documents, review and decisions, email to leads, and the pipeline overview.
Team: Bogdan Borachuk, Lev Lavryniuk, Antonii Maltsev.

This is the **Phase II initial prototype**, built on the course's unified stack:

- **Front end:** JavaScript + React + HTML5 (React 18, Vite, React Router) in `frontend/`
- **Back end:** Java 17+ with Spring Boot 3 (REST API, Spring Security) in `src/`
- **Database:** MySQL 8 (H2 in memory for a quick start and for the automated tests)

## Repository layout

```
pom.xml, mvnw, mvnw.cmd   Maven build: builds the back end and the React front end together
frontend/                 React front end (see "Front end" below)
src/main/java/...         Spring Boot back end, a REST API (see "Code structure" below)
src/main/resources/       configuration
src/test/java/...         automated tests (JUnit 5)
docs/                     reports, team journals, time logs, meeting links
docs/phase-2/             Phase II: functional test plan
```

## Running it

You need **JDK 17 or newer**. The Maven wrapper does the rest: it also downloads Node.js and builds the
React front end, so you don't need Node installed just to run the app. It uses the JDK in `JAVA_HOME` if that is
set, otherwise `java` on your PATH, so check whichever one applies (an old Java 8 on PATH is a common
cause of "release version 17 not supported"). Tested with JDK 17, 24 and 25.

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

Then open <http://localhost:8080>. The first build takes a few minutes because it downloads Node.js and the
npm packages; later builds are quick.

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
| `/signup` | Visitor | creates an applicant account (local sign-in) |
| `/login` | Applicant, staff | sign in with email address and password |
| `/applicant` | Applicant | UC-3 Submit, UC-4 Edit, UC-5 Withdraw, UC-6 Track Application Status |
| `/staff` | Staff | UC-7 to UC-14, and UC-15 runs when an applicant is accepted |

## Front end

A React single-page app in `frontend/`. It only talks to the back end through the REST API under `/api`.

| File | Contents |
|---|---|
| `src/App.jsx` | routes and the top bar |
| `src/api.js` | calls to the REST API (sends the CSRF token, returns the server's messages) |
| `src/auth.jsx` | who is signed in; `RequireRole` keeps staff and applicant pages apart |
| `src/components.jsx` | form fields with error messages, status badges, message boxes |
| `src/pages/` | `HomePage`, `SignUpPage`, `SignInPage`, `ApplicantPage`, `StaffPage` |
| `src/pages/staff/` | one component per staff tab: overview, inquiries, applications, email, events, programs |

To work on the front end with instant reload, run the back end (`./mvnw spring-boot:run`) and, in a second
terminal, the Vite dev server (needs Node.js 18+):

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>; API calls are passed on to the back end on port 8080.

## Code structure

The classes follow the Phase II class diagram (`docs/` report, section "Class Diagram").

| Package | Contents |
|---|---|
| `domain` | `User`, `Applicant`, `StaffMember`, `Lead`, `Inquiry`, `Program`, `Event`, `EventRegistration`, `Application`, `Document`, `StatusChange`, `EmailMessage`, `PipelineReport` and the enums. The rules live here, e.g. `Application.changeStatus` writes the decision log entry and `Program.isOpen` decides the deadline. |
| `repository` | Spring Data JPA repositories (MySQL / H2). |
| `service` | One service per group of use cases; they load data, call the domain methods and save. `DocumentStorage` checks uploads (PDF, at most 15 MB) and keeps files in a private folder. |
| `integration` | `MailGateway` and `SisGateway` from the class diagram. For now mail is written to the log (or sent to a test SMTP server with `app.mail.mode=smtp`) and the SIS is a stub, until Team 2 and Team 5 publish their APIs. |
| `security` | Local sign-in (Spring Security) until the Core Platform's shared sign-in exists. Kept out of `domain` on purpose. |
| `web` | REST controllers (`/api/public`, `/api/applicant`, `/api/staff`) and request/response records. `SpaController` serves the React app for its page addresses. |

The operations the class diagram puts on `Applicant` and `StaffMember` (for example
`StaffMember.recordDecision`) are carried out by the services on behalf of the signed-in user.

## Tests

```bash
./mvnw test
```

80 tests: unit tests of the domain rules, sign-in tests, and tests that start the whole module and call the REST API
the way the React app does. They also pass against a real MySQL 8 server. Each test name starts with the test case ID from the functional test plan
(`docs/phase-2/functional-test-plan.docx`), e.g. `TC-3.4 A file that is not a PDF is rejected`.

## Settings worth knowing

| Property | Default | Meaning |
|---|---|---|
| `app.storage.dir` | `./data/uploads` | where uploaded documents are kept (not served publicly) |
| `app.mail.mode` | `log` | `log` prints emails; `smtp` sends via `spring.mail.host` |
| `app.sis.available` | `true` | set `false` to see the warning when the SIS cannot be reached |
| `app.demo-data` | `true` | load the demo data on first start |
