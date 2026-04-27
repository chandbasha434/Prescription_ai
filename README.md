# 💊 Prescription AI — Orizen Rx

> An AI-powered prescription analyzer that reads prescription images, extracts medication names using **Google Gemini 2.5 Flash Vision**, and returns educational YouTube video links — with smart database caching for instant repeat results.

---

## 📌 Description

**Prescription AI** is a Spring Boot backend system designed to help patients understand their prescriptions better.

When a user uploads a prescription image:
- **Google Gemini AI** reads the image and extracts the medication names
- The system generates **YouTube educational video links** for each medication
- Results are **cached in PostgreSQL** — so the same image never calls AI twice

This solves a real-world problem: patients often receive prescriptions they don't understand. This system bridges that gap by giving them easy-to-access educational resources about their medications.

---

## ✨ Features

- 📷 **Upload Prescription Image** — Supports JPG, PNG, WEBP up to 10MB
- 🤖 **Real AI Analysis** — Google Gemini 2.5 Flash Vision reads and extracts medication names
- 🎬 **YouTube Video Recommendations** — One educational video link per detected medication
- ⚡ **Smart Caching** — SHA-256 content hash checked against PostgreSQL before any AI call
- 🔁 **Instant Repeat Results** — Same image returns in ~1ms from cache (vs ~5–8s AI call)
- 🛡️ **Graceful Error Handling** — User-friendly messages, never raw 500 errors
- 🌐 **REST API** — Clean JSON responses with proper HTTP status codes
- 💻 **Built-in Frontend** — Beautiful dark-mode web UI served at `localhost:8080`

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 4.0 |
| **Language** | Java 17 |
| **AI Vision** | Google Gemini 2.5 Flash |
| **Database** | PostgreSQL 16 |
| **ORM** | Spring Data JPA + Hibernate |
| **JSON Parsing** | Jackson Databind |
| **HTTP Client** | Java `java.net.http.HttpClient` |
| **Build Tool** | Maven |
| **Frontend** | HTML + CSS + JavaScript |

---

## 📁 Folder Structure

```
prescription-ai/
│
├── src/main/java/prescription_ai/
│   ├── PrescriptionAiApplication.java    ← App entry point
│   ├── controller/
│   │   └── PrescriptionController.java  ← Handles HTTP requests
│   ├── service/
│   │   └── PrescriptionService.java     ← Core business logic
│   ├── entity/
│   │   └── Prescription.java            ← Database table model
│   ├── repository/
│   │   └── PrescriptionRepository.java  ← Database queries
│   └── dto/
│       └── PrescriptionResponse.java    ← JSON response format
│
├── src/main/resources/
│   ├── application.properties           ← App configuration
│   └── static/
│       └── index.html                   ← Frontend UI
│
├── pom.xml                              ← Maven dependencies
└── README.md
```

---

## ⚙️ Installation

### Prerequisites

Make sure you have the following installed:
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

Or using psql:
```sql
CREATE DATABASE prescription_db;
```

---

### Step 3 — Configure the App

Open `src/main/resources/application.properties` and set:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/prescription_db
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password

gemini.api.key=YOUR_GEMINI_API_KEY
```

> 🔑 Get a **free Gemini API key** at: https://aistudio.google.com/app/apikey

---

### Step 4 — Run the App

```bash
./mvnw spring-boot:run
```

You should see:
```
Started PrescriptionAiApplication in 2.3 seconds
Tomcat started on port 8080
```

---

## 🚀 Usage

### Option 1 — Use the Web UI

Open your browser and go to:
```
http://localhost:8080
```

1. Click **"Choose Image"** and select a prescription photo
2. Click **"Analyze Prescription"**
3. Wait ~5–8 seconds for Gemini AI to read the image
4. See the detected medications and YouTube video links

---

### Option 2 — Use the API Directly

**Upload a prescription image:**
```bash
curl -X POST http://localhost:8080/api/v1/prescription/upload \
  -F "file=@prescription.jpg"
```

**Check server health:**
```bash
curl http://localhost:8080/api/v1/prescription/health
```

---

### API Response

**Success (first upload — AI analyzed):**
```json
{
  "success": true,
  "cached": false,
  "message": "Analyzed by Gemini AI",
  "medications": ["Paracetamol", "Amoxicillin", "Omeprazole"],
  "videos": [
    "https://www.youtube.com/results?search_query=Paracetamol+medication+uses+side+effects+guide",
    "https://www.youtube.com/results?search_query=Amoxicillin+medication+uses+side+effects+guide",
    "https://www.youtube.com/results?search_query=Omeprazole+medication+uses+side+effects+guide"
  ]
}
```

**Success (same image again — from cache):**
```json
{
  "success": true,
  "cached": true,
  "message": "Served from cache",
  "medications": ["Paracetamol", "Amoxicillin", "Omeprazole"],
  "videos": [ "..." ]
}
```

**Error (unclear image):**
```json
{
  "success": false,
  "message": "Image unclear. Please retake.",
  "medications": [],
  "videos": []
}
```

---

## 🗄️ Database

The `prescription` table is **automatically created** when the app starts.

```sql
CREATE TABLE prescription (
    id             BIGSERIAL PRIMARY KEY,
    file_name      VARCHAR(255),   -- SHA-256 hash of the image (cache key)
    extracted_text VARCHAR(5000),  -- detected medication names
    videos         VARCHAR(5000)   -- YouTube video URLs
);
```

**Check cached results:**
```bash
psql -U your_username -d prescription_db -c "SELECT id, extracted_text FROM prescription;"
```

**Clear the cache** (force re-analysis on next upload):
```bash
psql -U your_username -d prescription_db -c "TRUNCATE TABLE prescription RESTART IDENTITY;"
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/prescription/health` | Server health check |
| `POST` | `/api/v1/prescription/upload` | Analyze prescription image |
| `GET` | `/` | Frontend web UI |

---

## 👤 Author

**Shaik Chandbasha**
- 🎓 Java Backend Developer
- 💼 Built with Spring Boot, PostgreSQL & Google Gemini AI
- 📧 [chandbasha434@gmail.com](mailto:chandbasha434@gmail.com)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
