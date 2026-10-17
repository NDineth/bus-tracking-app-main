# 🚌 IoT-Enabled Smart Bus Route Monitoring & Revenue Management System

A comprehensive digital transport management system designed to improve the **efficiency, transparency, and reliability** of public bus services in Sri Lanka. This solution integrates **IoT hardware** with a **cloud-based backend** to enable real-time monitoring, automated scheduling, and intelligent revenue management.

---

## 🚀 Overview

This system leverages IoT devices and modern web/mobile technologies to:

- Track bus locations in real-time  
- Optimize scheduling using policy-driven logic  
- Monitor driver behavior and safety  
- Implement fair, distance-based revenue models  
- Provide actionable insights for transport authorities and bus owners  

---

## 🏗️ System Architecture

The platform follows a **modular architecture**:

- **On-board Unit (OBU):**
  - ESP32 microcontroller collects real-time data from sensors
- **Cloud Backend:**
  - Processes, analyzes, and stores incoming data
- **Frontend Applications:**
  - Web portal for administrators
  - Mobile app for drivers and bus owners  

---

## 🧰 Tech Stack

### 🔌 Hardware
- ESP32 Microcontroller  
- NEO-6M GPS Module  
- MPU6050 IMU Sensor  

### 💻 Backend
- Java  
- Spring Boot  
- Spring Security (JWT Authentication)  
- Spring Data JPA  

### 🗄️ Database
- PostgreSQL  

### 🌐 Frontend
- React (Web Portal)  
- React Native (Mobile App)  

### ☁️ Cloud & IoT
- Azure IoT Hub  
- MQTT / HTTPS Protocols  

---

## 👨‍💻 System Components

### 1️⃣ Distance-Based Revenue Management  
**Student:** W.S.H.R. Gunasekara (IT22228208)

#### Functional Requirements
- Real-time vehicle location and speed tracking  
- Distance-based revenue calculation model  
- Off-peak commission incentive system  
- Validation against manual distance records  

#### Non-Functional Requirements
- Secure encrypted data transmission  
- Privacy through anonymized identifiers  

---

### 2️⃣ Policy-Driven Bus Scheduling Engine  
**Student:** Sachintha H.D.D. (IT22264152)

#### Functional Requirements
- Time-slot classification (Peak / Normal / Night)  
- Dynamic schedule adjustments  
- Configurable policy enforcement  
- Automatic reassignment during disruptions  

#### Non-Functional Requirements
- Response time < 2 seconds  
- ≥ 95% classification accuracy  

---

### 3️⃣ Event-Driven Bus Monitoring Engine  
**Student:** Miflal Ahamed M.T. (IT22121042)

#### Functional Requirements
- GPS-based trip progress tracking  
- Route deviation detection  
- Geofencing at key locations  
- Event-based reporting  

#### Non-Functional Requirements
- Bandwidth-efficient event-driven communication  
- High accuracy in junction detection  

---

### 4️⃣ Driver Monitoring & Reporting Mobile App  
**Student:** N.D. Aththanayaka (IT22925268)

#### Functional Requirements
- Detection of unsafe driving behavior  
- Real-time driver alerts  
- Trip and operational reporting  
- Earnings and schedule visibility  

#### Non-Functional Requirements
- Low-latency alert system  
- User-friendly interface for drivers and owners  

---

## 📅 Project Roadmap (2026)

### 🟢 Semester 1
- Literature review  
- Data collection  
- Core system development (IoT + Backend + Scheduling Engine)  

### 🔵 Semester 2
- Full system integration  
- Testing & validation  
- Deployment & final evaluation  

---

## 🔒 Key Features

- 📍 Real-time GPS tracking  
- ⚙️ Automated scheduling engine  
- 💰 Smart revenue calculation  
- 🚨 Driver safety monitoring  
- 📊 Data-driven decision support  

---

## 📌 Future Improvements

- AI-based traffic prediction  
- Passenger demand forecasting  
- Integration with ticketing systems  
- Advanced analytics dashboards  

---

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request.

---

## 📄 License

This project is developed for academic purposes. License details can be added as needed.
