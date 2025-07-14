

# 🎒 Campus Lost & Found — Backend (Java + Vert.x)

This is the backend for a Lost & Found web application developed using Java, Vert.x, MongoDB, Redis, and JWT authentication. It allows students to post lost/found items, communicate via email, and manage claims, while admins can moderate content and manage categories.



## 📂 Project Overview

- Java 17 + Vert.x (asynchronous, event-driven framework)
- MongoDB (for storing users, items, categories)
- Redis (for token blacklisting during logout)
- JWT (stateless auth with role-based access)
- SMTP (email verification, contact, and password reset)
- File Upload (stores images in a local uploads/ folder)



## 🛠️ Project Structure


src/
│
├── config/
│   ├── handlers/         # Main route logic (Auth, Item, Admin)
│   ├── middleware/       # Authentication middleware using JWT & Redis
│   ├── models/           # MongoDB documents for Item, User, Category
│   ├── utils/            # Utility classes: Mail, JWT, Redis, Password
│   └── DatabaseConfig.java
│
├── uploads/              # Stores uploaded images (you must create this folder)
├── .env                  # Environment variables (do not commit)
└── Main.java             # Vert.x launcher and route loader



## ⚙️ Setup Instructions

### 🔧 Prerequisites

- Java 17+
- Maven
- MongoDB
- Redis
- Gmail SMTP account (with App Password)

### 🔑 Environment Variables

Create a .env file in the root folder:


# MongoDB Configuration

MONGO\_URI=mongodb://localhost:27017
MONGO\_DB=lostandfound

# Email SMTP

MAIL\_USERNAME=[your\_email@gmail.com](mailto:your_email@gmail.com)
MAIL\_PASSWORD=your\_app\_password
MAIL\_SENDER\_NAME=Findly



Do NOT commit this file to version control.

### 📁 Create Uploads Directory

This is required to store uploaded images locally:

bash
mkdir uploads


### 📦 Build & Run

bash
mvn clean package
java -jar target/lostfound-backend-1.0-fat.jar


Runs on: [http://localhost:8888](http://localhost:8888)

---

## 📮 API Overview

Use Postman or your frontend to interact with the following routes:

### 🔐 Auth Endpoints

| Method | Route                     | Description               |
| ------ | ------------------------- | ------------------------- |
| POST   | /api/auth/register        | Register new user         |
| GET    | /api/auth/verify/\:token  | Email verification        |
| POST   | /api/auth/login           | Login, returns JWT token  |
| POST   | /api/auth/logout          | Logout (blacklists token) |
| POST   | /api/auth/forgot-password | Sends reset email         |
| POST   | /api/auth/reset-password  | Resets password           |

### 👤 User Profile

| Method | Route   | Description           |
| ------ | ------- | --------------------- |
| GET    | /api/me | Get current user info |
| PATCH  | /api/me | Update name only      |

---

### 📦 Item Endpoints

| Method | Route                   | Description                                |
| ------ | ----------------------- | ------------------------------------------ |
| POST   | /api/items              | Create lost/found post (with image)        |
| GET    | /api/items              | Get list of items                          |
| GET    | /api/items/\:id         | Get item by ID                             |
| PATCH  | /api/items/\:id/claim   | Poster marks item as claimed               |
| POST   | /api/items/\:id/contact | Contact poster (must contact before claim) |
| GET    | /api/search?q=keyword   | Global search                              |
| GET    | /api/items/mine         | Get user’s own items                       |
| DELETE | /api/items/\:id         | Delete user’s own item                     |

⚠️ Claiming Logic:

* Only poster can mark an item as claimed
* Claimed is allowed only if another user first contacted the poster

---

### 🛠️ Admin Endpoints

| Method | Route                               | Description                  |
| ------ | ----------------------------------- | ---------------------------- |
| GET    | /api/admin/items                    | View all items               |
| DELETE | /api/admin/items/\:id/inappropriate | Admin deletes inappropriate  |
| GET    | /api/admin/stats                    | Get stats (items, users)     |
| POST   | /api/admin/categories               | Add a category               |
| DELETE | /api/admin/categories/\:id          | Delete a category            |
| GET    | /api/categories                     | Public: fetch all categories |

Note: Admin is auto-assigned to email: [findly.kjc@gmail.com](mailto:findly.kjc@gmail.com)

---

## 🧪 Testing APIs with Postman

1. Register with college email (must end with @kristujayanti.com)
2. Check terminal/email for the verification link
3. Login with email/password → receive JWT
4. For protected routes, set header:

```
Authorization: Bearer <token>
```

5. Use form-data for file/image upload in POST /api/items

---

## 🔌 Frontend Integration

* Frontend: Angular + TailwindCSS
* All APIs served from: [http://localhost:8888](http://localhost:8888)
* Use Angular services to call backend endpoints
* Use HttpInterceptor to attach JWT tokens to requests
* Store token in localStorage for session management
* Handle 401/403 responses gracefully in frontend

---

## 🚀 Deployment Notes

* Backend can be deployed independently
* Use services like Railway, Render, or any VPS
* Use PM2/systemd to run the backend in production
* Set .env variables in your hosting environment
* Point Angular frontend to the deployed backend API

---

## 📦 Developer & Handoff Notes

* Modular code: handlers, middleware, models, utils
* Uses MailClient (SMTP) for email communication
* Tokens expire in 2 hours; Redis handles logout blacklist
* Only verified users can log in
* Images stored in local /uploads folder
* Easy to scale and extend (e.g. add refresh tokens)





