# 💊 Prescription AI — Orizen Rx

> An AI-powered prescription analyzer that reads prescription images using **Google Gemini 2.5 Flash Vision**, extracts medication names with descriptions, and returns educational YouTube video links — with smart PostgreSQL caching for instant repeat results.

---

## 📌 Description

**Prescription AI** is a Spring Boot backend + beautiful 3D frontend that helps patients understand their prescriptions.

When a user uploads a prescription image:
- **Google Gemini 2.5 Flash Vision AI** reads the image and extracts each medication name along with a description of what it treats
- The system generates **YouTube educational video links** for each medication
- Results are **cached in PostgreSQL** using SHA-256 content hashing — so the same image never calls AI twice
- A **History page** shows all past analyses in a beautiful timeline

---

## ✨ Features

- 📷 **Upload Prescription Image** — JPG, PNG, WEBP · Drag-and-drop supported
- 🤖 **Real Gemini 2.5 Flash Vision AI** — reads medications + what each one is used for
- 💊 **Medication Detail Cards** — name + one-line description (e.g. *"Pain relief and fever reduction"*)
- 🎬 **YouTube Video Links** — one educational search link per detected medication
- ⚡ **SHA-256 Content-Based Caching** — same image → instant response from PostgreSQL (~1ms)
- 📋 **Prescription History Page** — view all past analyses with stats and timestamps
- 🔒 **Secure API Key Management** — key stored in environment variable, never in source code
- 🌐 **Clean REST API** — structured JSON responses with proper HTTP status codes
- 🎨 **Premium 3D Frontend** — Three.js animated background, glassmorphism, fully responsive

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 4.0 |
| **Language** | Java 17 |
| **AI Vision** | Google Gemini 2.5 Flash (`v1beta`) |
| **Database** | PostgreSQL 16 |
| **ORM** | Spring Data JPA + Hibernate |
| **JSON Parsing** | Jackson Databind |
| **HTTP Client** | Java `java.net.http.HttpClient` |
| **Build Tool** | Maven |
| **Frontend** | HTML + CSS + JavaScript + Three.js |

---

## 📁 Folder Structure

```
prescription-ai/
│
├── src/main/java/prescription_ai/
│   ├── PrescriptionAiApplication.java     ← App entry point
│   ├── controller/
│   │   └── PrescriptionController.java   ← REST endpoints (/upload, /history, /health)
│   ├── service/
│   │   └── PrescriptionService.java      ← Core logic: hash → cache → Gemini → save
│   ├── entity/
│   │   └── Prescription.java             ← DB table (id, file_name, extracted_text, videos, created_at)
│   ├── repository/
│   │   └── PrescriptionRepository.java   ← DB queries
│   └── dto/
│       ├── MedicationInfo.java           ← {name, use} per medication
│       ├── ServiceResult.java            ← Internal service return type
│       ├── PrescriptionResponse.java     ← JSON shape for /upload
│       └── HistoryItem.java              ← JSON shape for /history
│
├── src/main/resources/
│   ├── application.properties            ← Config (reads GEMINI_API_KEY from env)
│   ├── application.properties.template   ← Safe template to commit to git
│   └── static/
│       ├── index.html                    ← Main analyzer UI (3D background)
│       └── history.html                  ← Prescription history page
│
├── pom.xml                               ← Maven dependencies
└── README.md
```

---

## ⚙️ Installation

### Prerequisites

- ✅ Java 17
- ✅ Maven
- ✅ PostgreSQL

---

### Step 1 — Clone the Repository

```bash
git clone https://github.com/your-username/prescription-ai.git
cd prescription-ai
```

---

### Step 2 — Create the Database

```bash
createdb -U your_username prescription_db
```

---

### Step 3 — Set Your Gemini API Key (Environment Variable)

> ⚠️ The API key is **never hardcoded** in source code. Always use an environment variable.

```bash
export GEMINI_API_KEY=your_gemini_api_key_here
```

> Get a **free Gemini API key** at: https://aistudio.google.com/app/apikey

To make it permanent, add it to your shell profile:
```bash
echo 'export GEMINI_API_KEY=your_key_here' >> ~/.zshrc
source ~/.zshrc
```

---

### Step 4 — Configure Database in `application.properties`

Copy the template:
```bash
cp src/main/resources/application.properties.template \
   src/main/resources/application.properties
```

Then edit the file and set your PostgreSQL credentials:
```properties
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password
```

---

### Step 5 — Run the App

```bash
export GEMINI_API_KEY=your_key_here
./mvnw spring-boot:run
```

You should see:
```
Started PrescriptionAiApplication in 2.6 seconds
Tomcat started on port 8080
```

---

## 🚀 Usage

### Option 1 — Web UI

Open your browser:
```
http://localhost:8080          ← Main analyzer
http://localhost:8080/history.html  ← Prescription history
```

1. Drag-and-drop or choose a prescription image
2. Click **"Analyze Prescription"**
3. Gemini AI reads the image (~5–8 sec first time)
4. See detected medications, their descriptions, and YouTube video links
5. Upload same image again → instant result from cache

---

### Option 2 — API (curl)

```bash
# Analyze a prescription
curl -X POST http://localhost:8080/api/v1/prescription/upload \
  -F "file=@prescription.jpg"

# View history
curl http://localhost:8080/api/v1/prescription/history

# Health check
curl http://localhost:8080/api/v1/prescription/health
```

---

## 📡 API Responses

### `POST /api/v1/prescription/upload`

**Success — AI analyzed (first upload):**
```json
{
  "success": true,
  "cached": false,
  "message": "Analyzed by Gemini AI",
  "medications": [
    { "name": "Paracetamol", "use": "Pain relief and fever reduction" },
    { "name": "Amoxicillin", "use": "Antibiotic for bacterial infections" },
    { "name": "Omeprazole",  "use": "Reduces stomach acid to treat heartburn and GERD" }
  ],
  "videos": [
    "https://www.youtube.com/results?search_query=Paracetamol+medication+uses+side+effects+guide",
    "https://www.youtube.com/results?search_query=Amoxicillin+medication+uses+side+effects+guide",
    "https://www.youtube.com/results?search_query=Omeprazole+medication+uses+side+effects+guide"
  ]
}
```

**Success — cache hit (same image uploaded again):**
```json
{
  "success": true,
  "cached": true,
  "message": "Served from cache",
  "medications": [ ... ],
  "videos": [ ... ]
}
```

**Error:**
```json
{
  "success": false,
  "message": "No medications found. Please upload a clearer prescription image.",
  "medications": [],
  "videos": []
}
```

---

### `GET /api/v1/prescription/history`

```json
[
  {
    "id": 1,
    "medications": [
      { "name": "Paracetamol", "use": "Pain relief and fever reduction" },
      { "name": "Amoxicillin", "use": "Antibiotic for bacterial infections" }
    ],
    "videoCount": 2,
    "analyzedAt": "27 Apr 2026, 10:33 am"
  }
]
```

---

## 🗄️ Database

Table `prescription` is **auto-created** by Hibernate on startup.

```sql
CREATE TABLE prescription (
    id             BIGSERIAL PRIMARY KEY,
    file_name      VARCHAR(255),    -- SHA-256 hash of image bytes (cache key)
    extracted_text VARCHAR(5000),   -- JSON array: [{name, use}, ...]
    videos         VARCHAR(5000),   -- comma-separated YouTube search URLs
    created_at     TIMESTAMP        -- auto-set on insert
);

-- Index for fast cache lookups (auto-created by Hibernate)
CREATE INDEX idx_prescription_file_name ON prescription (file_name);
```

**Useful commands:**
```bash
# Connect
psql -U your_username -d prescription_db

# View all analyses
SELECT id, extracted_text, created_at FROM prescription;

# Clear cache (forces re-analysis next upload)
TRUNCATE TABLE prescription RESTART IDENTITY;
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/v1/prescription/health` | Server health check → `"OK"` |
| `POST` | `/api/v1/prescription/upload` | Analyze prescription image |
| `GET`  | `/api/v1/prescription/history` | List all past analyses |
| `GET`  | `/` | Main analyzer UI |
| `GET`  | `/history.html` | Prescription history page |

---

## 🔒 Security

| Practice | Implementation |
|----------|---------------|
| API Key storage | Environment variable `GEMINI_API_KEY` — never in source code |
| `.gitignore` | `application.properties` excluded — commit `application.properties.template` instead |
| Error handling | All exceptions caught — raw stack traces never exposed to client |
| CORS | `@CrossOrigin(origins = "*")` for local dev — restrict in production |

---

## ⚡ Key Design Decisions

### 1. SHA-256 Content-Based Caching
Cache key = SHA-256 hash of file **bytes**, not filename. Same prescription → instant cache hit regardless of filename. Saves cost + response time.

### 2. Database-First Architecture
PostgreSQL checked **before** any AI call. Cache hit: **~1ms** vs AI call: **~5–8s**.

### 3. Structured Gemini Prompt
Gemini is asked to return a JSON array `[{name, use}]` — not just medication names. This gives meaningful context to the user.

### 4. ServiceResult Pattern
Service returns a typed `ServiceResult` object — no more string parsing in the controller.

### 5. Backward-Compatible Cache Parsing
Old comma-separated `extracted_text` data is still handled gracefully via try-catch fallback.

---

## 👤 Author

**Shaik Chandbasha**
- 🎓 Java Backend Developer
- 💼 Built with Spring Boot, PostgreSQL & Google Gemini 2.5 Flash AI
- 📧 [chandbasha434@gmail.com](mailto:chandbasha434@gmail.com)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
