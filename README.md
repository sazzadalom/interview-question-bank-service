# interview-question-bank-service

Interview Roulette **Question Bank Service** (port `9090`): manages the shared technical question catalogue, topics, candidate contributions, Excel question imports, manual question creation, duplicate detection and question retrieval.

The central MySQL question bank is the source of truth for question content. Candidate-uploaded questions are attributed to the candidate and can be prioritised by the Practice Service.

**Technology stack:** Java 21, Spring Boot 3.x, Spring Web, Spring Data JPA, Hibernate, MySQL 8, Maven, Apache POI for `.xlsx` parsing, Jakarta Validation, Lombok (where used), JUnit 5 and Mockito.

## Package layout

The runtime package prefix confirmed by application logs is `com.interviewroulette.questionbank`.

```text
com.interviewroulette.questionbank
 ├── config          Application, database, JPA and CORS configuration
 ├── controller      TopicController, QuestionController, QuestionUploadController,
 │                   candidate-question endpoints
 ├── dto             Import/manual-create request and response DTOs
 ├── entity          Topic, Question, QuestionContribution, QuestionUploadBatch and enums
 ├── repository      Spring Data repositories and duplicate/eligibility queries
 ├── service         TopicService, QuestionService and question lookup services
 ├── service.upload  ExcelQuestionImportService and workbook parsing/validation
 ├── util             QuestionNormalizer and SHA-256 helper
 ├── exception       Domain exceptions and global REST exception handling
 └── QuestionBankServiceApplication.java
```

## Purpose and responsibilities

- Maintain active technical topics.
- Store question text, difficulty and optional expected answers.
- Import question rows from Excel `.xlsx` files.
- Allow a candidate to create one or more questions manually.
- Normalize question text and calculate a SHA-256 digest for duplicate detection.
- Reuse an existing central question when the same normalized question is submitted again.
- Store candidate-to-question contribution records, including duplicate contributions.
- Return topic questions and candidate-contributed questions.
- Provide question data to the Practice Service.

The service is not responsible for practice session timing, attempt status or dashboard aggregation. Those responsibilities belong to `interview-practice-service`.

## Architecture and approach

```text
React Frontend
    |
    | REST / JSON and multipart/form-data
    v
Question Bank Service :9090
    |
    `---- MySQL question-bank database
          |-- topics
          |-- questions
          |-- question_upload_batches
          `-- question_contributions

Practice Service :9091
    |
    `---- REST calls to retrieve eligible question data
```

### Why a central question bank?

A central catalogue prevents the same question from being stored separately for every candidate. Each question is stored once per topic, while contribution rows record which candidate supplied it. This separates shared content from user-specific provenance.

Advantages:

- Less duplicate storage.
- Shared questions become available to candidates who did not upload a workbook.
- Candidate contributions remain traceable.
- Question content can be maintained independently from practice attempts.
- The practice service can prioritise candidate-contributed questions without copying them.

### Excel import flow

1. The frontend submits a `.xlsx` file, candidate UUID and topic ID as multipart form data.
2. The controller validates the request and delegates to the import service.
3. Apache POI reads workbook cell values as data.
4. Rows are validated; blank or invalid question rows are counted as invalid.
5. Question text is normalised.
6. A SHA-256 hash is calculated from the normalised comparison text.
7. The service attempts to insert the question.
8. If the question already exists under the same topic, the existing question is reused.
9. A `question_contributions` row records the candidate's contribution even when the question is a duplicate.
10. The upload batch is updated with totals and status.
11. A summary response is returned to the frontend.

The import is transactional so that related question, contribution and batch updates remain consistent according to the implemented transaction boundary.

### Manual question creation

Manual entry uses the same normalisation, duplicate detection and contribution attribution approach as Excel import. It avoids maintaining two different duplicate-prevention rules.

Request shape:

```json
{
  "candidateId": "6a305976-38a1-4310-a323-fda449837d49",
  "topicId": 1,
  "questions": [
    {
      "question": "What is the difference between HashMap and Hashtable?",
      "difficulty": "INTERMEDIATE",
      "expectedAnswer": "HashMap is not synchronized by default."
    }
  ]
}
```

Difficulty values accepted by the implemented manual-creation service:

- `BEGINNER`
- `INTERMEDIATE`
- `ADVANCED`

`expectedAnswer` is optional.

Representative response:

```json
{
  "totalQuestions": 1,
  "validQuestions": 1,
  "newQuestions": 1,
  "duplicateQuestions": 0
}
```

The exact response status and any additional response properties should be checked against `CandidateQuestionCreationResponse`.

## Duplicate detection: normalization and SHA-256

SHA-256 is used as a deterministic comparison key. It is **not encryption** and cannot be reversed to recover the question text.

The comparison process:

1. Trim leading and trailing whitespace.
2. Collapse repeated whitespace into a single ordinary space.
3. Apply Unicode NFKC normalization.
4. Convert the comparison value to locale-independent lowercase.
5. Preserve punctuation and technical symbols.
6. Encode the normalized text as UTF-8.
7. Calculate the SHA-256 digest.
8. Enforce uniqueness within the same topic using the normalized hash.

The original question text is retained for display. The hash is used for matching and indexing, not as the display text.

### Why SHA-256 is useful here

- **Deterministic:** the same normalized input produces the same digest.
- **Fast lookup:** compare a fixed-length digest instead of repeatedly comparing long text values.
- **Case/spacing duplicate handling:** text differences limited to case and whitespace can resolve to the same comparison key.
- **Topic isolation:** the uniqueness scope includes the topic, so the same wording can exist under separate topics.
- **Concurrency protection:** a database unique constraint is the final guard against two concurrent uploads inserting the same normalized question.

SHA-256 does not prove that two differently worded questions have the same meaning. Semantic duplicate detection is outside the current MVP.

### Hashing vs encryption vs signing

| Technique | Purpose | Reversible? | Interview Roulette use |
| --- | --- | --- | --- |
| SHA-256 hashing | Deterministic fingerprint / comparison | No practical reversal | Normalized-question duplicate detection |
| Encryption (AES, etc.) | Confidentiality of stored/transmitted data | Yes, with the correct key | Not confirmed as implemented for question data |
| Digital signature | Prove authenticity and integrity of signed data | Verification uses public key | Not confirmed as implemented in this MVP |

Do not call the question hash "encrypted question data". Do not store an encryption key in source control. For sensitive production data, use TLS in transit and database/storage encryption at rest.

## Database model

| Table | Purpose |
| --- | --- |
| `topics` | Topic catalogue and active/inactive state |
| `questions` | Central question content, topic, difficulty, expected answer and normalized hash |
| `question_upload_batches` | Candidate import/manual-entry batch metadata and counts |
| `question_contributions` | Candidate-to-question-to-batch attribution |

Important integrity rules:

- A question belongs to a topic.
- A normalized question should be unique within a topic.
- A contribution links a candidate, question and batch.
- The same central question may have multiple contribution records from different candidates or batches.
- Database uniqueness is the final concurrency guard; application-side pre-checks alone are insufficient.

## Run it

1. Install Java 21 and Maven.
2. Create the Question Bank MySQL database and user.
3. Configure the database URL, username and password in `application.yml` or environment variables.
4. Start the service on port `9090`.

```bash
mvn clean package
mvn spring-boot:run
mvn test
```

If a Docker Compose file is present:

```bash
docker compose up --build
```

### Configuration

Use the actual values from `src/main/resources/application.yml` as the authoritative defaults.

| Configuration | Local example | Purpose |
| --- | --- | --- |
| Server port | `9090` | Question Bank HTTP port |
| Database URL | `jdbc:mysql://localhost:3306/interview_question_bank_db` | Question Bank database |
| Database username/password | local development credentials | Database access |
| JPA DDL mode | project configuration | Schema update/validation policy |
| Multipart file limit | project configuration | Maximum upload size |
| CORS allowed origins | frontend local URL | Browser access from Vite |

Do not commit production credentials or upload secrets to source control.

## Endpoints

The following paths are confirmed by the frontend service integration and the manual-question implementation discussed during development.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/topics` | Get active topics |
| `GET` | `/api/v1/topics/{topicId}` | Get topic details |
| `GET` | `/api/v1/topics/{topicId}/questions` | Get questions belonging to a topic |
| `GET` | `/api/v1/questions/{questionId}` | Get one question |
| `GET` | `/api/v1/candidates/{candidateId}/topics/{topicId}/questions` | Get questions contributed by a candidate for a topic |
| `POST` | `/api/v1/question-uploads/excel` | Upload and import an Excel workbook |
| `POST` | `/api/v1/questions/bulk` | Create one or more questions manually |

### 1. Get active topics

`GET /api/v1/topics`

Representative response (`200 OK`):

```json
[
  {
    "id": 1,
    "name": "Core Java",
    "description": "Java language and platform concepts",
    "active": true
  },
  {
    "id": 2,
    "name": "Spring Boot",
    "description": "Spring Boot and Spring ecosystem",
    "active": true
  }
]
```

### 2. Get a topic by ID

`GET /api/v1/topics/{topicId}`

Example:

```bash
curl http://localhost:9090/api/v1/topics/1
```

Representative response (`200 OK`):

```json
{
  "id": 1,
  "name": "Core Java",
  "description": "Java language and platform concepts",
  "active": true
}
```

### 3. Get questions by topic

`GET /api/v1/topics/{topicId}/questions`

Representative response (`200 OK`):

```json
[
  {
    "id": "e0a36c15-7f4c-4e67-a8b5-2d25c4f1c111",
    "topicId": 1,
    "question": "What is the difference between HashMap and Hashtable?",
    "difficulty": "INTERMEDIATE",
    "expectedAnswer": "HashMap is not synchronized by default."
  }
]
```

### 4. Get one question

`GET /api/v1/questions/{questionId}`

Representative response (`200 OK`):

```json
{
  "id": "e0a36c15-7f4c-4e67-a8b5-2d25c4f1c111",
  "topicId": 1,
  "question": "What is the difference between HashMap and Hashtable?",
  "difficulty": "INTERMEDIATE",
  "expectedAnswer": "HashMap is not synchronized by default."
}
```

### 5. Get candidate-contributed questions for a topic

`GET /api/v1/candidates/{candidateId}/topics/{topicId}/questions`

Example:

```bash
curl http://localhost:9090/api/v1/candidates/6a305976-38a1-4310-a323-fda449837d49/topics/1/questions
```

Representative response (`200 OK`):

```json
[
  {
    "id": "e0a36c15-7f4c-4e67-a8b5-2d25c4f1c111",
    "topicId": 1,
    "question": "What is the difference between HashMap and Hashtable?",
    "difficulty": "INTERMEDIATE",
    "expectedAnswer": "HashMap is not synchronized by default."
  }
]
```

### 6. Upload Excel questions

`POST /api/v1/question-uploads/excel`

Content type: `multipart/form-data`.

Form fields:

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `file` | `.xlsx` file | Yes | Excel workbook |
| `candidateId` | UUID string | Yes | Candidate who contributed the questions |
| `topicId` | Numeric ID | Depends on upload mapping | Selected topic when the workbook is uploaded for one topic |

Example:

```bash
curl -X POST http://localhost:9090/api/v1/question-uploads/excel \
  -F "file=@questions.xlsx" \
  -F "candidateId=6a305976-38a1-4310-a323-fda449837d49" \
  -F "topicId=1"
```

Representative response (`200 OK`):

```json
{
  "batchId": 101,
  "status": "COMPLETED",
  "totalRows": 50,
  "validRows": 50,
  "newQuestions": 35,
  "duplicateQuestions": 12,
  "invalidRows": 3
}
```

The counts above are examples. The exact response DTO and status code should be verified in the current `QuestionUploadController`.

### 7. Create questions manually

`POST /api/v1/questions/bulk`

Content type: `application/json`.

Request:

```json
{
  "candidateId": "6a305976-38a1-4310-a323-fda449837d49",
  "topicId": 1,
  "questions": [
    {
      "question": "What is the difference between HashMap and Hashtable?",
      "difficulty": "INTERMEDIATE",
      "expectedAnswer": "HashMap is not synchronized by default."
    },
    {
      "question": "What is the purpose of the volatile keyword?",
      "difficulty": "ADVANCED",
      "expectedAnswer": "It provides visibility guarantees for reads and writes."
    }
  ]
}
```

Representative response:

```json
{
  "totalQuestions": 2,
  "validQuestions": 2,
  "newQuestions": 2,
  "duplicateQuestions": 0
}
```

Validation:

- `candidateId` must be a valid candidate UUID.
- `topicId` must identify an existing topic.
- `questions` must contain question entries.
- Question text is required.
- Difficulty must be one of `BEGINNER`, `INTERMEDIATE`, `ADVANCED`.
- Expected answer is optional.
- Duplicate questions are reused rather than inserted again.

## How the important rules are implemented

- **One central question per topic/hash.** The normalized hash and database uniqueness constraint prevent duplicate central question rows.
- **Contribution attribution is retained.** A candidate is associated with the question even when the question already existed.
- **Excel and manual entry share duplicate logic.** Both flows use the same normalisation/hash strategy.
- **Candidate uploads can be prioritised.** The Practice Service can query candidate contributions before using the remaining shared questions.
- **Question content is not duplicated per user.** Contribution records express provenance separately from the central question.
- **Original text is preserved.** Normalization is used for matching; the original question remains the display value.
- **Upload data is treated as untrusted.** Validate file extension and workbook structure; do not render uploaded text as trusted HTML.

## Deviations and implementation notes

1. Manual question creation was added after the initial Excel-based import flow through `POST /api/v1/questions/bulk`.
2. Manual entry creates a batch/contribution record so that manual and Excel contributions can be tracked consistently.
3. The current MVP does not include an administrative question-management UI; questions are added through Excel or manual candidate entry.
4. The current implementation uses SHA-256 for duplicate detection. It does not imply that question text is encrypted.
5. Confirm exact DTO names, response fields, multipart limits and error status codes from the current source before external API publication.

## Verification status

- The service was run locally on port `9090` during development.
- Excel import was exercised and database persistence issues involving ID column types were encountered and corrected during development.
- Manual question creation service and DTOs were added and compiled according to the development conversation.
- Run `mvn test` and repeat both Excel and manual import tests against the current database before release.
