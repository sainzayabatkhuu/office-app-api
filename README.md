# Office Report Platform

Байгууллагын дотоод тайлангийн веб систем — **Java 21 · Spring Boot 3.5 · Spring Cloud 2025.0 · MySQL 8 · RabbitMQ · JasperReports**, microservice архитектуртай.
`../office/office-app-api` монолитын тайлан, хэрэглэгч, эрхийн логикийг үйлчилгээ бүрд хувааж, томоохон төсөлд шаардлагатай дэд бүтэцтэйгээр дахин бичсэн.

## Архитектур

```
                 ┌──────────────┐   ┌──────────────────┐
  Angular/Next ─►│ api-gateway  │──►│ discovery-server │  (Eureka)
   (4300/3000)   │   :8080      │   └──────────────────┘
                 │ JWT шалгах,  │   ┌──────────────────┐
                 │ CORS, route, │   │  config-server   │  (config-repo/*.yml)
                 │ circuit-brk  │   └──────────────────┘
                 └──┬────┬────┬─┘
        ┌───────────┘    │    └──────────────┐
        ▼                ▼                   ▼
 identity-service   report-service   notification-service
   :8081              :8082              :8083
 ажилтан, хэрэглэгч,  тайлангийн каталог,   мэдэгдэл хадгалах,
 эрх, салбар, JWT     Jasper, түүх          WebSocket (STOMP) push
        │                │  ▲  │                 ▲
   office_identity  office_report  └── RabbitMQ ──┘
                         │  (ASYNC тайлан, мэдэгдэл)
                    reporting DB (read-only; жишээ нь core banking replica)
```

| Модуль | Үүрэг |
|---|---|
| `common/office-common` | Бүх servlet үйлчилгээний нийтлэг код: RFC 7807 алдааны хариу, `BaseEntity` (audit + optimistic lock), JWT resource-server тохиргоо, `CurrentUser`, Feign token дамжуулагч, RabbitMQ event-ууд. Auto-configuration тул үйлчилгээнд dependency нэмэхэд л ажиллана. |
| `infrastructure/config-server` | Төвлөрсөн тохиргоо (`config-repo/`). `application.yml` = бүгдэд, `<service>.yml` = тухайн үйлчилгээнд, `*-docker.yml` = docker профайл. |
| `infrastructure/discovery-server` | Eureka service registry. |
| `infrastructure/api-gateway` | Ганц нэвтрэх цэг: routing (`lb://`), JWT шалгалт, CORS, circuit breaker + fallback, WebSocket proxy, бүх үйлчилгээний Swagger-ийг нэгтгэсэн UI. |
| `services/identity-service` | Ажилтан, хэрэглэгч, role/privilege, салбар/хэлтэс/албан тушаал, нэвтрэлт (RS256 JWT + refresh token rotation), lockout. |
| `services/report-service` | `.jrxml`/`.zip` загвар upload (upload үед compile), параметр, role/салбараар эрх, SYNC/ASYNC үүсгэлт, PDF/XLSX/DOCX/CSV/HTML, түүх, татах, retention. |
| `services/notification-service` | RabbitMQ-с мэдэгдэл хүлээн авч хадгалах, WebSocket-оор тухайн хэрэглэгч рүү push хийх, уншсан/уншаагүй. |

### `app_user` ба `employee`

`app_user` нь `employee`-ийн primary key-г хуваалцдаг (`app_user.employee_id = employee.id`, JPA `@MapsId`).
Хүснэгтийн бүтэц нь `AppUser extends Employee` (JOINED inheritance)-тэй яг ижил, гэхдээ:

* хэрэглэгч бүр заавал ажилтан байна (ажилтан бүр хэрэглэгч биш);
* **одоо байгаа ажилтанд дараа нь эрх олгож болно** — JPA inheritance-д entity төрлөө сольж чадахгүй тул энэ боломжгүй байдаг;
* ажилтныг ажлаас чөлөөлөхөд (`POST /employees/{id}/terminate`) хэрэглэгч автоматаар идэвхгүй болж, бүх session цуцлагдана.

### Эрхийн загвар

* **Privilege** (`REPORT_EXECUTE`, `USER_MANAGE`, …) — кодонд `@PreAuthorize("hasAuthority('...')")`-ээр шалгагдана; зөвхөн Flyway migration-оор нэмэгдэнэ.
* **Role** = privilege-ийн багц, API-аар удирдана. Built-in: `ADMIN`, `REPORT_ADMIN`, `REPORT_USER`, `HR_MANAGER`.
* **Салбарын хязгаар**: `multiBranchAccess=false` хэрэглэгч зөвхөн өөрийн салбарын ажилтан/хэрэглэгчийг харна; тайлангийн `branchId` параметрийг сервер өөрөө хэрэглэгчийн салбараар дүүргэнэ (client-ээс өөрчлөх боломжгүй).
* **Тайлангийн эрх**: тайлан бүрд `role` (+ сонголтоор салбар) олгоно; role-д `ASYNC` горим албадаж болно.
* Gateway token-ийг шалгадаг ч үйлчилгээ бүр дахин шалгана (zero-trust). Token-ийг зөвхөн identity-service private key-ээр гарын үсэг зурна; бусад нь `/.well-known/jwks.json`-ээс public key авна.

## Ажиллуулах

Шаардлага: JDK 21, Maven 3.9+, Docker.

```bash
make up
```

Энэ нь jar-уудыг build хийж, `.env` болон JWT түлхүүр (`keys/`) үүсгээд бүх зүйлийг Docker-т асаана. Дараа нь:

```bash
./scripts/smoke-test.sh
```

| | URL |
|---|---|
| API gateway | http://localhost:8080 |
| Swagger UI (бүх үйлчилгээ) | http://localhost:8080/swagger-ui.html |
| Eureka | http://localhost:8761 |
| RabbitMQ | http://localhost:15672 (office / office) |
| Zipkin (trace) | http://localhost:9411 |
| Prometheus / Grafana | http://localhost:9090 / http://localhost:3001 (admin / admin) |
| MySQL | localhost:**3316** (office / office) |

Анхны нэвтрэлт: **admin / Admin@123** — dev-ээс өөр орчинд заавал солих.

### IDE-ээс ажиллуулах

```bash
make infra
```

MySQL, RabbitMQ, Zipkin-г Docker-т асаана. Дараа нь IDE-ээс дарааллаар нь асаана: `ConfigServerApplication` → `DiscoveryServerApplication` → `IdentityServiceApplication` → `ReportServiceApplication`, `NotificationServiceApplication` → `GatewayApplication`. Default тохиргоо localhost руу заасан.

## Тайлан нэмэх

1. Jaspersoft Studio-д `.jrxml` зурна. Кирилл үсэгт **DejaVu Sans** фонт ашиглана (`jasperreports-fonts`-д багтсан; өөрийн фонтыг font-extension jar болгож report-service-ийн dependency-д нэмнэ).
2. Сервер өөрөө дүүргэх параметрүүд: `reportTitle`, `maker` (username), `makerName`, `branchId` (дээрхийг үзнэ үү). Бусад параметр хэрэглэгчийн форм болно; `parameterDescription` нь label болно.
3. Subreport/зурагтай бол бүгдийг `.zip` болгож upload хийнэ (үндсэн файл `main.jrxml` эсвэл `mainTemplate=` параметр). Subreport-ыг `"sub.jasper"` эсвэл `$P{SUBREPORT_DIR} + "sub.jasper"` гэж зааж болно.
4. Upload:
   ```bash
   curl -X POST http://localhost:8080/api/v1/reports -H "Authorization: Bearer $TOKEN" \
     -F 'report={"code":"EMPLOYEE_LIST","metadata":{"title":"Ажилтны жагсаалт","allowedFormats":["PDF","XLSX"],"defaultFormat":"PDF","executionMode":"SYNC","active":true}};type=application/json' \
     -F 'file=@employee_list.jrxml'
   ```
   Загвар compile болохгүй бол upload шууд 422 буцаана. Шинэ хувилбар upload хийхэд (`POST /reports/{id}/template`) параметрийн тохиргоо хадгалагдана, хуучин хувилбар файлууд устахгүй.
5. `POST /reports/{id}/permissions` `{"roleCode":"REPORT_USER"}` — эрх олгоно.

Тайлангийн өгөгдлийн сан нь `office.report.datasource` (`REPORT_DB_URL`…) — үйлчилгээний өөрийн DB-ээс тусдаа pool тул хүнд тайлан бусад API-г гацаахгүй. Нэг instance дээр зэрэг ажиллах тайлангийн тоо `REPORT_MAX_CONCURRENT`; хуудас/хугацааны дээд хязгаарыг Jasper governor-оор бүх тайланд хэрэгжүүлнэ.

## API (gateway-ээр)

| Method | Path | Privilege |
|---|---|---|
| POST | `/api/v1/auth/login`, `/refresh` | — |
| POST | `/api/v1/auth/logout` · GET `/me` · PUT `/me/password`, `/me/preferences` | нэвтэрсэн |
| GET/POST/PUT | `/api/v1/employees`, `/{id}`, `/{id}/terminate` | `EMPLOYEE_VIEW` / `EMPLOYEE_MANAGE` |
| GET/POST/PUT | `/api/v1/users`, `/{id}`, `/{id}/reset-password`, `/{id}/unlock` | `USER_VIEW` / `USER_MANAGE` |
| GET/POST/PUT/DELETE | `/api/v1/roles`, GET `/api/v1/privileges` | `ROLE_VIEW` / `ROLE_MANAGE` |
| GET (POST/PUT) | `/api/v1/branches`, `/departments`, `/positions` | нэвтэрсэн (`ORG_MANAGE`) |
| GET | `/api/v1/reports`, `/{id}` | `REPORT_VIEW` |
| POST | `/api/v1/reports/{id}/executions` → SYNC 200, ASYNC 202 | `REPORT_EXECUTE` |
| GET | `/api/v1/report-executions`, `/{id}`, `/{id}/download` (`?all=true` → `REPORT_HISTORY_ALL`) | `REPORT_VIEW` |
| GET/POST/PUT/DELETE | `/api/v1/reports/manage`, `/reports`, `/reports/{id}/template`, `/parameters`, `/permissions` | `REPORT_MANAGE` |
| GET/POST | `/api/v1/notifications`, `/unread-count`, `/{id}/read`, `/read-all` | нэвтэрсэн (илгээх: `NOTIFICATION_SEND`) |
| WS | `/ws` (STOMP, CONNECT header `Authorization: Bearer …`, subscribe `/user/queue/notifications`) | нэвтэрсэн |

Алдаа бүр RFC 7807 хэлбэртэй, `errorCode` талбартай (`INVALID_CREDENTIALS`, `ACCOUNT_TEMPORARILY_LOCKED`, `REQUIRED_PARAMETER`, `FORMAT_NOT_ALLOWED`, …), validation алдаанд `fieldErrors`.

## Тест

```bash
make test     # unit
make verify   # unit + integration (Testcontainers: MySQL, RabbitMQ — Docker хэрэгтэй)
```

Integration test-үүд (`*IT`) бодит MySQL дээр Flyway migration + Hibernate `validate`-аар schema-г, мөн нэвтрэлт/lockout/refresh rotation, тайлан upload → SYNC PDF/CSV → ASYNC RabbitMQ → download, RabbitMQ → WebSocket push-ийг шалгадаг.

## Шинэ үйлчилгээ нэмэх

1. `services/<name>`-д module үүсгэж root `pom.xml`-д нэмнэ; `office-common`-ийг dependency болгоно (identity-service-ийн `pom.xml`-ийг загвар болгож болно).
2. `application.yml`-д зөвхөн `spring.application.name`, `server.port`, `spring.config.import: configserver:…`.
3. `config-repo/<name>.yml`-д datasource (`office_<name>` DB) болон бусад тохиргоо; `docker/mysql/01-databases.sql`-д DB нэмнэ.
4. Schema-г `src/main/resources/db/migration/V1__*.sql`-д бичнэ (`ddl-auto: validate`).
5. `config-repo/api-gateway.yml`-д route болон Swagger URL нэмнэ; `docker-compose.yml`-д service нэмнэ.

## Production-д анхаарах

* `keys/` болон `.env`-ийг secret manager-ээр (Vault, K8s Secret) өгнө; dev нууц үгсийг солино.
* Тайлангийн файлын storage (`LocalFileStorage`) олон instance-д shared volume шаардана — эсвэл `FileStorage`-ийн S3/MinIO хэрэгжүүлэлт нэмнэ.
* notification-service олон instance-тэй бол in-memory STOMP broker-ыг RabbitMQ STOMP relay болгоно (`WebSocketConfig`-д тайлбартай).
* Gateway-д rate limiting (Redis), төвлөрсөн log (Loki/ELK) нэмэх боломжтой.
=======
# office-app



## Getting started

To make it easy for you to get started with GitLab, here's a list of recommended next steps.

Already a pro? Just edit this README.md and make it your own. Want to make it easy? [Use the template at the bottom](#editing-this-readme)!

## Add your files

* [Create](https://docs.gitlab.com/user/project/repository/web_editor/#create-a-file) or [upload](https://docs.gitlab.com/user/project/repository/web_editor/#upload-a-file) files
* [Add files using the command line](https://docs.gitlab.com/topics/git/add_files/#add-files-to-a-git-repository) or push an existing Git repository with the following command:

```
cd existing_repo
git remote add origin https://gitlab.com/sainzaya.b/office-app.git
git branch -M main
git push -uf origin main
```

## Integrate with your tools

* [Set up project integrations](https://gitlab.com/sainzaya.b/office-app/-/settings/integrations)

## Collaborate with your team

* [Invite team members and collaborators](https://docs.gitlab.com/user/project/members/)
* [Create a new merge request](https://docs.gitlab.com/user/project/merge_requests/creating_merge_requests/)
* [Automatically close issues from merge requests](https://docs.gitlab.com/user/project/issues/managing_issues/#closing-issues-automatically)
* [Enable merge request approvals](https://docs.gitlab.com/user/project/merge_requests/approvals/)
* [Set auto-merge](https://docs.gitlab.com/user/project/merge_requests/auto_merge/)

## Test and Deploy

Use the built-in continuous integration in GitLab.

* [Get started with GitLab CI/CD](https://docs.gitlab.com/ci/quick_start/)
* [Analyze your code for known vulnerabilities with Static Application Security Testing (SAST)](https://docs.gitlab.com/user/application_security/sast/)
* [Deploy to Kubernetes, Amazon EC2, or Amazon ECS using Auto Deploy](https://docs.gitlab.com/topics/autodevops/requirements/)
* [Use pull-based deployments for improved Kubernetes management](https://docs.gitlab.com/user/clusters/agent/)
* [Set up protected environments](https://docs.gitlab.com/ci/environments/protected_environments/)

***

# Editing this README

When you're ready to make this README your own, just edit this file and use the handy template below (or feel free to structure it however you want - this is just a starting point!). Thanks to [makeareadme.com](https://www.makeareadme.com/) for this template.

## Suggestions for a good README

Every project is different, so consider which of these sections apply to yours. The sections used in the template are suggestions for most open source projects. Also keep in mind that while a README can be too long and detailed, too long is better than too short. If you think your README is too long, consider utilizing another form of documentation rather than cutting out information.

## Name
Choose a self-explaining name for your project.

## Description
Let people know what your project can do specifically. Provide context and add a link to any reference visitors might be unfamiliar with. A list of Features or a Background subsection can also be added here. If there are alternatives to your project, this is a good place to list differentiating factors.

## Badges
On some READMEs, you may see small images that convey metadata, such as whether or not all the tests are passing for the project. You can use Shields to add some to your README. Many services also have instructions for adding a badge.

## Visuals
Depending on what you are making, it can be a good idea to include screenshots or even a video (you'll frequently see GIFs rather than actual videos). Tools like ttygif can help, but check out Asciinema for a more sophisticated method.

## Installation
Within a particular ecosystem, there may be a common way of installing things, such as using Yarn, NuGet, or Homebrew. However, consider the possibility that whoever is reading your README is a novice and would like more guidance. Listing specific steps helps remove ambiguity and gets people to using your project as quickly as possible. If it only runs in a specific context like a particular programming language version or operating system or has dependencies that have to be installed manually, also add a Requirements subsection.

## Usage
Use examples liberally, and show the expected output if you can. It's helpful to have inline the smallest example of usage that you can demonstrate, while providing links to more sophisticated examples if they are too long to reasonably include in the README.

## Support
Tell people where they can go to for help. It can be any combination of an issue tracker, a chat room, an email address, etc.

## Roadmap
If you have ideas for releases in the future, it is a good idea to list them in the README.

## Contributing
State if you are open to contributions and what your requirements are for accepting them.

For people who want to make changes to your project, it's helpful to have some documentation on how to get started. Perhaps there is a script that they should run or some environment variables that they need to set. Make these steps explicit. These instructions could also be useful to your future self.

You can also document commands to lint the code or run tests. These steps help to ensure high code quality and reduce the likelihood that the changes inadvertently break something. Having instructions for running tests is especially helpful if it requires external setup, such as starting a Selenium server for testing in a browser.

## Authors and acknowledgment
Show your appreciation to those who have contributed to the project.

## License
For open source projects, say how it is licensed.

## Project status
If you have run out of energy or time for your project, put a note at the top of the README saying that development has slowed down or stopped completely. Someone may choose to fork your project or volunteer to step in as a maintainer or owner, allowing your project to keep going. You can also make an explicit request for maintainers.
