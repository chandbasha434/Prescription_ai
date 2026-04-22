# Prescription AI Module

## 🚀 Overview

This project implements a backend system where users upload a prescription image and receive exercise video recommendations. The system is designed with a focus on **performance, scalability, and resilience**.

---

## 🧠 Architecture Flow

1. User uploads prescription image via API
2. Backend checks PostgreSQL database for existing results (DB-first approach)
3. If data exists → return cached response
4. If not:

    * Call AI service using WebClient
    * Process response
    * Store result in database
    * Return output

---

## 🛠 Tech Stack

* Backend: Spring Boot
* Database: PostgreSQL
* API Client: WebClient (Reactive)
* Language: Java

---

## ⚡ Key Features

### ✅ 1. Database-First Logic

* Prevents unnecessary AI API calls
* Improves performance and reduces cost

### ✅ 2. Resilient Error Handling

* Handles unclear images gracefully
* Returns user-friendly message:

  > "Image unclear. Please retake."

### ✅ 3. WebClient Integration

* Non-blocking API communication
* Better suited for high-concurrency systems
* Preferred over RestTemplate

### ✅ 4. Secure API Key Management

* API key stored in application.properties
* Avoids hardcoding sensitive data

---

## 🧪 API Endpoint

### Upload Prescription

POST `/api/upload`

**Request:**

* form-data
* key: `file`
* value: image file

---

## 📌 Sample Responses

### First Upload

```
New Processed: https://youtube.com/knee-exercise
```

### Second Upload (Same File)

```
From DB: https://youtube.com/knee-exercise
```

### Error Case

```
Image unclear. Please retake.
```

---

## ▶️ How to Run

1. Start PostgreSQL
2. Create database:

   ```sql
   CREATE DATABASE prescription_db;
   ```
3. Update application.properties:

   ```
   spring.datasource.username=your_username
   ```
4. Run Spring Boot application
5. Test API using Postman or curl

---

## 💡 Design Decisions

* Used DB caching to optimize external API usage
* Implemented graceful fallback for AI failures
* Used WebClient for scalability and modern reactive design

---

## 🔚 Conclusion

This system demonstrates how to build a scalable backend service with proper architecture, error handling, and performance optimization suitable for real-world applications.
