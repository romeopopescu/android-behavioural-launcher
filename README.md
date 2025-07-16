# Android Launcher for Behavioural Profile

This project is my bachelor's thesis, an intelligent Android launcher designed to provide a "post-unlock" security layer, and also user behaviour based app recommendations. It acts as a second line of defense by learning your unique app usage habits and detecting anomalous behavior in real-time, while providing an efficient finding of apps.

## Core Concept

The fundamental idea is to create a profile of a user's normal smartphone interaction. The application monitors which apps are used, when they are used, for how long, and how frequently.

-   **Normal Usage:** When your activity matches your learned profile, the system remains in a normal state.
-   **Anomalous Usage:** If a pattern emerges that significantly deviates from your normal routine (e.g., a rarely used app being launched repeatedly at an odd hour), the system flags it as an anomaly and raises the security level.

## Features

-   **Custom Home Screen:** A clean home screen with a live system status indicator, floating cards for app suggestions, risky apps based on permissions, and easy access to other parts of the launcher.
-   **App Drawer & Search:** A standard, scrollable app drawer listing all installed applications with a functional search bar to quickly find any app.
-   **Usage Statistics:** A detailed statistics page showing key metrics like the most used app, average daily usage, total app launches, and the most active day of the week.
-   **Real-time Anomaly Detection:** The core security feature that constantly evaluates recent app usage against the trained user profile.
-   **Past 24h Application Usage** Page that shows each app and it's time used in the past 24 hours.

## Architecture & Tech Stack

The project is split into two main components: the Android client and a Python backend for the machine learning model.

### Android Client

-   **Language:** Kotlin
-   **UI:** Jetpack Compose
-   **Architecture:** MVI (Model-View-Intent)
-   **Key Libraries:**
    -   Hilt for Dependency Injection
    -   Room for the local database
    -   Retrofit for networking
    -   Coroutines & Flow for asynchronous tasks

### Backend Server

-   **Language:** Python
-   **Framework:** FastAPI
-   **Machine Learning:** TensorFlow (Keras) to build the Autoencoder model.

## How It Works: The Data Lifecycle

The system operates in a continuous cycle, ensuring the user's profile is both established and monitored.

```mermaid
graph TD
    A[Start: First App Launch] -->|Collects 7 days of history| B(UsageStatsCollector);
    B -->|Sends data to /train| C[Python API: Train Autoencoder];
    C -->|Calculates & Stores| D(Personalized Threshold);
    
    subgraph "Continuou![homepage](https://github.com/user-attachments/assets/40520153-a892-41aa-82d1-8ac414af6b90)
s Monitoring"
        E[User Uses Phone] --> F[RealtimeUsageSampler];
        F -->|Sends recent data to /detect| G[Python API: Detect Anomaly];
        G -->|Compares score to| D;
        G --> H{Result};
    end

    H -->|Updates StateFlow| I[UI on Home Screen];
```

## Screenshots

<table>
  <tr>
    <td align="center"><b>Home Screen</b></td>
    <td align="center"><b>App Drawer</b></td>
    <td align="center"><b>App Search</b></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/1332a134-b816-4a46-9f1a-a13402c44172" alt="Home Screen" width="300"/></td>
    <td><img src="https://github.com/user-attachments/assets/83ff3fec-c7a7-4be4-b2d8-7fcd86a9e0aa" alt="App Drawer" width="300"/></td>
    <td><img src="https://github.com/user-attachments/assets/b13158d1-7fc6-41e9-912a-ba6c7cb7b01e" alt="App Search" width="300"/></td>
  </tr>
  <tr>
    <td align="center"><b>Statistics Page</b></td>
    <td align="center"><b>24-Hour Usage</b></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/580efe39-24ac-4390-bfe8-aca63df4da57" alt="Statistics Page" width="300"/></td>
    <td><img src="https://github.com/user-attachments/assets/25928b1e-16b1-4a85-b26d-e5acd6152e49" alt="24-Hour Usage" width="300"/></td>
  </tr>
</table>
