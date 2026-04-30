<div align="center">

# 🏥 MediLink Care

### AI-Powered Medical Platform — JavaFX Desktop Application

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-3.x-red?style=for-the-badge&logo=apache-maven)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--3.5-green?style=for-the-badge&logo=openai)
![Google](https://img.shields.io/badge/Google-OAuth2-yellow?style=for-the-badge&logo=google)

*Connecting patients with doctors — your health, our priority*

</div>

---

## 📸 Screenshots

### 🔐 Authentication & Landing
| Landing Page | Login Page | Register Page |
|---|---|---|
| ![Landing](screenshots/landing.png) | ![Login](screenshots/login.png) | ![Register](screenshots/register.png) |
| Dark particle background | Google Sign In + CAPTCHA | AI password validation |

### 👑 Admin — Mohamed Zribi
| Dashboard | User Management | AI Chat |
|---|---|---|
| ![Dashboard](screenshots/dashboard.png) | ![Users](screenshots/userlist.png) | ![AI Chat](screenshots/ai_chat.png) |
| Charts + stats + AI welcome | Card view + AI search + export | OpenAI chat widget |

### 📅 Rendez-vous — Seif Ben Salem
| Appointments List | AI Planning | Email Notification |
|---|---|---|
| ![RDV List](screenshots/rdv_list.png) | ![AI Planning](screenshots/rdv_ai.png) | ![Email](screenshots/rdv_email.png) |
| Manage appointments | AI timing for doctors | Mailing confirmation |

### 💊 Médicaments & Ordonnances — Dhiaa Taamouli
| Medications | Ordonnance | QR Code + Voice |
|---|---|---|
| ![Medications](screenshots/medications.png) | ![Ordonnance](screenshots/ordonnance.png) | ![QR](screenshots/qr_voice.png) |
| Drug management | AI-assisted prescription writing | QR reader + voice detection |
| Export PDF + SMS Twilio | | |

### 💝 Gestion Dons — Rahma Habbechi
| Donations List | Stripe Payment | AI Decision |
|---|---|---|
| ![Dons](screenshots/dons.png) | ![Stripe](screenshots/stripe.png) | ![AI Dons](screenshots/ai_dons.png) |
| Donation management | Stripe payment integration | AI helps admin accept/refuse |

### 🎪 Gestion Events — Slim Ammar
| Events List | Maps Integration | QR Code |
|---|---|---|
| ![Events](screenshots/events.png) | ![Maps](screenshots/events_map.png) | ![Event QR](screenshots/events_qr.png) |
| Event management | Google Maps location | QR code per event |
| AI description generation | | |

---

## ✨ Features by Module

### 👑 User Management — Mohamed Zribi
- **Email & Password login** with role-based routing (Admin / Doctor / Patient)
- **Google OAuth2 Sign In** — auto-registers new users as ROLE_USER
- **Remember Me** — auto-login on next app launch
- **Forgot Password** — 6-digit reset code flow
- **Custom CAPTCHA** — drag slider + smart questions (bot-proof)
- **User CRUD** — card view with Add / Edit / Delete popups
- **Real-time filter** by name, email, role, status
- **Export users** to `.txt` or `.csv`
- **OpenAI** — password validation, welcome message, AI search, profile summary, health tips, chat widget
- **Dark / Light theme** toggle on all pages
- **Particle animations**, typing effect, count-up, hover glow, slide-in

---

### 📅 Rendez-vous Management — Seif Ben Salem
- Full appointment CRUD for patients and doctors
- **AI Planning** — OpenAI suggests optimal timing slots for doctors
- **Mailing** — automatic email notifications for appointment confirmation
- Doctor availability management
- Appointment status tracking (pending / confirmed / cancelled)

---

### 💊 Médicaments & Ordonnances — Dhiaa Taamouli
- Full medication and prescription management
- **AI Prescription Assistant** — helps doctors write ordonnances
- **Export PDF** — generates professional prescription PDF
- **SMS Twilio** — sends prescription details to patient by SMS
- **QR Code Reader** — scan medication QR codes
- **Voice Detection** — voice input for prescription writing
- Medication stock and dosage tracking

---

### 💝 Gestion des Dons — Rahma Habbechi
- Full donation management for the medical platform
- **Stripe Payment** — secure online donation processing
- **AI Decision Assistant** — AI analyzes donation description and helps admin accept or refuse based on medical utility
- Donation history and tracking
- Donor management

---

### 🎪 Gestion des Événements — Slim Ammar
- Full event management (medical conferences, health fairs, etc.)
- **Google Maps API** — location integration for events
- **QR Code** — unique QR code per event for check-in
- **AI Description Generator** — auto-generates event description from the title
- Event registration and attendance tracking

---

## 🏗️ Architecture

```
MediLink Care
├── Model        User, RendezVous, Medicament, Ordonnance, Don, Event
├── Service      UserService, MyConnection (Singleton DB)
├── Controller   LoginController, DashboardController, HomeController...
├── Helper       ThemeManager, RememberMeHelper, EffectsHelper...
├── View         FXML files + CSS stylesheets
└── Resources    config.properties (gitignored), logo.png, dark.css
```

### Navigation Flow
```
landing.fxml
    └── main.fxml (Login)
            ├── ROLE_ADMIN ──────→ dashboard.fxml
            │                          ├── userlist.fxml
            │                          ├── rendezvous.fxml
            │                          ├── medicaments.fxml
            │                          ├── dons.fxml
            │                          └── events.fxml
            ├── ROLE_MEDECIN ────→ dashboard.fxml
            │                          ├── rendezvous.fxml
            │                          └── ordonnances.fxml
            └── ROLE_USER ───────→ home.fxml
                                       └── profile.fxml
```

---

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| JavaFX | 21.0.2 | Desktop UI framework |
| Maven | 3.x | Dependency management |
| MySQL | 8.0 | Database |
| XAMPP | Latest | Local MySQL server |
| OpenAI API | GPT-3.5-turbo | AI features (all modules) |
| Google OAuth2 | v2 | Sign In with Google |
| Google Maps API | Latest | Event locations |
| Stripe API | Latest | Donation payments |
| Twilio API | Latest | SMS notifications |
| Gson | 2.10.1 | JSON parsing |
| iText / Apache PDFBox | Latest | PDF export |

---

## 🚀 Getting Started

### Prerequisites
- Java 21 JDK
- IntelliJ IDEA
- XAMPP (for MySQL)
- Maven

### Installation

**1. Clone the repository**
```bash
git clone https://github.com/rahma0108/pidev_java26.git
cd pidev_java26
git checkout userfx
```

**2. Set up the database**
- Start XAMPP and enable MySQL
- Create database `medilink`
- Import the SQL schema from `/database/medilink.sql`

**3. Create config.properties**

Create `src/main/resources/config.properties`:
```properties
# OpenAI
openai.api.key=YOUR_OPENAI_KEY

# Google OAuth2
google.client.id=YOUR_GOOGLE_CLIENT_ID
google.client.secret=YOUR_GOOGLE_CLIENT_SECRET

# Stripe
stripe.api.key=YOUR_STRIPE_KEY

# Twilio
twilio.account.sid=YOUR_TWILIO_SID
twilio.auth.token=YOUR_TWILIO_TOKEN
twilio.phone.number=YOUR_TWILIO_NUMBER

# Google Maps
google.maps.api.key=YOUR_MAPS_KEY
```

> ⚠️ This file is gitignored and must NEVER be committed to GitHub.

**4. Load Maven dependencies**

Open `pom.xml` in IntelliJ and click **Load Maven Changes**.

**5. Run the app**
```bash
mvn javafx:run
```
Or press **Shift + F10** in IntelliJ.

---

## 🔑 Key Design Patterns

### Singleton — MyConnection.java
```java
public class MyConnection {
    private static MyConnection instance;

    public static MyConnection getInstance() {
        if (instance == null) instance = new MyConnection();
        return instance;
    }
}
```
Ensures only one database connection exists at a time.

### MVC Pattern
- **Model** — `User.java`, `RendezVous.java`, `Don.java` etc.
- **View** — FXML files (UI layout)
- **Controller** — `LoginController.java` etc. (business logic)

---

## 🤖 AI Features Summary

| Module | AI Feature | API |
|---|---|---|
| Users | Password validation on register | OpenAI |
| Users | Personalized admin welcome message | OpenAI |
| Users | Natural language user search | OpenAI |
| Users | Professional user profile summary | OpenAI |
| Users | Daily health tips for patients | OpenAI |
| Users | AI Chat widget on dashboard | OpenAI |
| Rendez-vous | Optimal timing planner for doctors | OpenAI |
| Ordonnances | AI prescription writing assistant | OpenAI |
| Dons | AI accept/refuse donation decision | OpenAI |
| Events | Auto-generate event description from title | OpenAI |

---

## 🔒 Security Features

| Feature | Implementation |
|---|---|
| SQL Injection prevention | PreparedStatement for all queries |
| API Key protection | config.properties in .gitignore |
| Bot prevention | Custom drag-slider + smart CAPTCHA |
| Authentication | Google OAuth2 + email/password |
| Role-based access | ROLE_ADMIN, ROLE_MEDECIN, ROLE_USER |
| Payment security | Stripe secure checkout |

---

## 👨‍💻 Team

| Member | Module | APIs & Features |
|---|---|---|
| **Mohamed Zribi** | 👑 User Management & Auth | OpenAI, Google OAuth2, Custom CAPTCHA |
| **Seif Ben Salem** | 📅 Rendez-vous | OpenAI AI planning, Mailing |
| **Dhiaa Taamouli** | 💊 Médicaments & Ordonnances | OpenAI, PDF export, Twilio SMS, QR code, Voice detection |
| **Rahma Habbechi** | 💝 Gestion des Dons | Stripe payment, OpenAI AI decision |
| **Slim Ammar** | 🎪 Gestion des Événements | Google Maps, QR code, OpenAI description |

---

## 📝 Academic Context

This project was built for **PiDev 3A** at **Esprit School of Engineering**.
- **Branch:** `userfx`
- **Sprint:** Java Desktop (JavaFX)
- **Team size:** 5 members

---

<div align="center">

Made with ❤️ by the MediLink Team — Esprit School of Engineering

**🏥 MediLink Care — Your Health, Our Priority**

</div>
