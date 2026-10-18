## office-app-api

## Running locally

Secrets are no longer in `application.properties`. Either export environment variables
(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` — at least 32 characters) or copy
`application-local.properties.example` to `application-local.properties` (git-ignored) and fill it in.

```
mvn spring-boot:run
mvn verify          # unit + integration tests (needs Docker for Testcontainers)
```

After deploying, run `data/query/2026-10-18_frontend_privileges.sql` once (new privilege for Excel export monitoring).

### API notes for the front-end

* `POST /api/v1/auth/login` → `{accessToken, refreshToken, expiresIn}`; access token lives 30 min.
  Renew with `POST /api/v1/auth/refresh {refreshToken}` (the old refresh token stops working); sign out with
  `POST /api/v1/auth/logout {refreshToken}`.
* Every `/api/v1` call is checked against the caller's privilege rules (`method:path`, e.g. `get:/roles` also
  covers `/roles/5`). Errors are RFC 7807 JSON with `errorCode` and `message`.
* Report files: `GET /api/v1/report-histories/{id}/download` (owner, or users who may view report histories).
  `/generated-reports/**` is no longer served.
* Notifications: STOMP at `/ws` (SockJS), CONNECT header `Authorization: Bearer …`, subscribe to
  `/user/queue/notifications`.



# Steps to Fix Font Issue in JasperReports (iReport 6.x)

1.	Install the Font Extension

    •	Download the font you want (e.g., Arial.ttf or Unicode-safe font like DejaVu Sans).
    •	Open iReport Designer.
    •	Go to Settings → Jaspersoft Studio → Fonts (sometimes called Fonts → Install Font).

2.	Configure Font in iReport

	•	After adding, you will see an option to:
	•	Check “PDF Embedded” ✅
	•	Choose encoding → select Identity-H (important for Unicode).
	•	Save it.

3.	Export Font to JAR

	•	When you add the font in iReport, it creates a font extension JAR (usually xxx-fonts.jar).
	•	Find this JAR in your iReport installation directory (or in ~/.jasperreports/fonts).

4.	Add Font JAR to Spring Boot Project

	•	Copy the generated font JAR into your project’s src/main/resources or add it as a dependency in your build.
	•	If using Maven, install the JAR into your local repo:

'''
mvn install:install-file \
-Dfile=/Users/sainzayabatkhuu/Desktop/gitprojects/office-app-api/data/OopsAF-fonts.jar \
-DgroupId=com.oopsAF \
-DartifactId=jasper-fonts \
-Dversion=1.0 \
-Dpackaging=jar
'''

Then add in pom.xml:

<dependency>
  <groupId>com.oopsAF</groupId>
  <artifactId>jasper-fonts</artifactId>
  <version>1.0</version>
</dependency>