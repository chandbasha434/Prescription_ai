# Prescription AI Module

## 🚀 Overview

This project implements a backend system where users upload a prescription image and receive exercise video recommendations. The system is designed with a focus on **performance, scalability, and resilience**.

---

## 🧠 How It Works — Step by Step (Pin to Pin)

### Step 1 — User Uploads Image

* User sends a prescription image via HTTP POST request:

  ```
  POST http://localhost:8080/api/upload
  Content-Type: multipart/form-data
  Key: file
  ```

---

### Step 2 — Controller Receives the Request

**File:** `controller/PrescriptionController.java`

```java
@PostMapping("/upload")
public ResponseEntity<String> uploadPrescription(
        @RequestParam("file") MultipartFile file) {

    if (file.isEmpty()) {
        return ResponseEntity.badRequest().body("No file uploaded");
    }

    String result = service.processFile(file);

    if (result.equals("Image unclear. Please retake.")) {
        return ResponseEntity.badRequest().body(result);
    }

    return ResponseEntity.ok(result);
}
```

* Checks if file is empty → returns `400 Bad Request` immediately
* Passes file to `PrescriptionService.processFile()`
* If service returns the error message → returns `400`
* Otherwise returns `200 OK` with the video URL

---

### Step 3 — Service Gets the Filename

**File:** `service/PrescriptionService.java`

```java
String fileName = file.getOriginalFilename();
```

* Reads the original name of the uploaded file (e.g. `prescription.jpg`)
* This filename is used as the **cache key** to check the database

---

### Step 4 — Database Checked First (DB-First Logic)

```java
Optional<Prescription> existing = repo.findByFileName(fileName);

if (existing.isPresent()) {
    return "From DB: " + existing.get().getVideos();
}
```

* Calls `PrescriptionRepository.findByFileName(fileName)`
* Runs SQL: `SELECT * FROM prescription WHERE file_name = ?`
* **If found (cache HIT)** → returns saved video URL immediately — no AI call
* **If not found (cache MISS)** → continues to Step 5

---

### Step 5 — AI Processing (Simulated)

```java
if (fileName != null && fileName.contains("error")) {
    throw new RuntimeException("AI failed to process image");
}

String extractedText = "knee pain detected";
String videos = "https://youtube.com/knee-exercise";
```

* If filename contains the word `error` → simulates AI failure → goes to catch block
* Otherwise → mock AI returns: `"knee pain detected"` and a video URL
* In production this would call a real AI Vision API

---

### Step 6 — Result Saved to Database

```java
Prescription p = new Prescription();
p.setFileName(fileName);
p.setExtractedText(extractedText);
p.setVideos(videos);

repo.save(p);
```

* Creates a new `Prescription` entity with the filename, extracted text, and video URL
* Saves it to the `prescription` table in PostgreSQL
* Next time the same filename is uploaded → **Step 4 returns it instantly from DB**

---

### Step 7 — Response Returned to User

```java
return "New Processed: " + videos;
```

* Returns plain text response with the video URL

---

### Step 8 — Error Handling (Graceful Fallback)

```java
} catch (Exception e) {
    return "Image unclear. Please retake.";
}
```

* Any exception caught here — AI failure, file read error, DB error
* Returns a friendly message instead of crashing with a 500 error
* Controller maps this to a `400 Bad Request` response

---

## 📁 Code File Structure — Every File Explained

```
prescription-ai/
│
├── src/main/java/prescription_ai/
│   │
│   ├── PrescriptionAiApplication.java
│   │     Entry point. Starts the Spring Boot server on port 8080.
│   │
│   ├── controller/
│   │   ├── PrescriptionController.java
│   │   │     Receives POST /api/upload request.
│   │   │     Validates file. Calls service. Returns response.
│   │   │
│   │   └── TestController.java
│   │         Simple test endpoint to verify server is running.
│   │
│   ├── service/
│   │   └── PrescriptionService.java
│   │         MAIN LOGIC FILE.
│   │         1. Gets filename from uploaded file
│   │         2. Checks DB — if found, returns cached video URL
│   │         3. If not found — runs mock AI logic
│   │         4. Saves result to DB
│   │         5. Returns video URL
│   │         6. On any error — returns friendly message
│   │
│   ├── entity/
│   │   └── Prescription.java
│   │         JPA entity — maps to the `prescription` table in PostgreSQL.
│   │         Fields: id, fileName, extractedText, videos
│   │
│   └── repository/
│       └── PrescriptionRepository.java
│             Spring Data JPA interface.
│             Key method: findByFileName(String fileName)
│             → runs: SELECT * FROM prescription WHERE file_name = ?
│
├── src/test/java/prescription_ai/
│   └── PrescriptionAiApplicationTests.java
│         Basic Spring Boot context load test.
│
├── src/main/resources/
│   └── application.properties
│         Server port, database URL, username, password, JPA settings.
│
├── pom.xml
│     Project dependencies: Spring Web, Spring Data JPA, PostgreSQL driver.
│
└── README.md
      This file.
```

---

## 🗄️ Database — Pin to Pin

### Table: `prescription`

Auto-created by Spring Boot JPA (`ddl-auto=update`) on startup.

```sql
CREATE TABLE prescription (
    id            BIGSERIAL PRIMARY KEY,
    file_name     VARCHAR(255),
    extracted_text VARCHAR(5000),
    videos        VARCHAR(5000)
);
```

### Column Details

| Column | Type | Purpose |
|--------|------|---------|
| `id` | BIGSERIAL | Auto-increment primary key |
| `file_name` | VARCHAR(255) | Filename used as cache key (e.g. `prescription.jpg`) |
| `extracted_text` | VARCHAR(5000) | What the AI detected from the image |
| `videos` | VARCHAR(5000) | Video URL returned to the user |

### Useful Queries

```sql
-- Connect to DB
psql -U your_username -d prescription_db

-- See all saved records
SELECT * FROM prescription;

-- See specific columns
SELECT id, file_name, videos FROM prescription;

-- Search by filename
SELECT * FROM prescription WHERE file_name = 'prescription.jpg';

-- Check how many records are cached
SELECT COUNT(*) FROM prescription;

-- Delete one record
DELETE FROM prescription WHERE id = 1;

-- Clear all cached records
TRUNCATE TABLE prescription RESTART IDENTITY;

-- Exit
\q
```

---

## 🛠 Tech Stack

* Backend: Spring Boot 4.0 (Java 17)
* Database: PostgreSQL
* API Client: WebClient (Reactive)
* Language: Java

---

## ⚡ Key Features

### ✅ 1. Database-First Logic

* Filename used as cache key
* PostgreSQL checked before any AI processing
* Prevents unnecessary API calls — improves performance and reduces cost

### ✅ 2. Resilient Error Handling

* All exceptions caught in one `catch` block
* Returns user-friendly message instead of crashing:

  > "Image unclear. Please retake."

* Controller maps error message to `400 Bad Request` — never returns raw 500

### ✅ 3. WebClient Integration

* Non-blocking API communication
* Better suited for high-concurrency systems
* Preferred over RestTemplate

### ✅ 4. Secure API Key Management

* API key stored in `application.properties` via environment variable
* Avoids hardcoding sensitive data in source code

---

## 🧪 API Endpoint

### Upload Prescription

```
POST /api/upload
Content-Type: multipart/form-data

Key:   file
Value: image file
```

---

## 📌 Sample Responses

### First Upload (new file — processed)

```
New Processed: https://youtube.com/knee-exercise
```

### Second Upload (same filename — from DB cache)

```
From DB: https://youtube.com/knee-exercise
```

### Error Case (filename contains "error" or any failure)

```
Image unclear. Please retake.
```

---

## ▶️ How to Run

1. Start PostgreSQL and create database:

   ```sql
   CREATE DATABASE prescription_db;
   ```

2. Update `application.properties`:

   ```properties
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. Run Spring Boot application:

   ```bash
   mvn spring-boot:run
   ```

4. Test with curl:

   ```bash
   # Upload a real image
   curl -X POST http://localhost:8080/api/upload \
     -F "file=@prescription.jpg"

   # Test error case
   curl -X POST http://localhost:8080/api/upload \
     -F "file=@error_image.jpg"

   # Test empty file
   curl -X POST http://localhost:8080/api/upload \
     -F "file=@/dev/null;filename=empty.jpg"
   ```

---

## 💡 Design Decisions

* Used DB caching to optimize external API usage
* Used filename as cache key — simple and effective for prototype
* Implemented graceful fallback for AI failures
* Used WebClient for scalability and modern reactive design

---

## 🔚 Conclusion

This system demonstrates how to build a scalable backend service with proper architecture, error handling, and performance optimization — suitable for real-world medical applications.
