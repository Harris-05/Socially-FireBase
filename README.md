# 📸 Socially — Instagram-Style Networking App

**Socially** is a feature-rich mobile networking platform designed to connect users through immersive photo sharing, ephemeral stories, and real-time communication. Built entirely with **Android (Kotlin)**, it leverages **Firebase** for robust backend services and the **Agora API** for high-quality audio and video calling.

## ✨ Features

### 🔐 Authentication & Security
* **Secure Login/Signup:** Powered by Firebase Authentication (Email/Password).
* **Session Management:** Persistent user sessions to keep users logged in seamlessly.
* **Profile Initialization:** Setup display name, username, and profile picture upon registration.

### 📱 Social Feed & Posts
* **Media Sharing:** Upload photos from the gallery or camera directly to the main feed.
* **Rich Interactions:** Like, comment, and engage with posts from connections.
* **Smart Feed:** An infinite-scrolling RecyclerView that dynamically loads posts chronologically from followed accounts.
* **Optimized Media:** Images are processed, compressed, and cached locally using Glide before uploading to Firebase Cloud Storage.

### ⏳ Ephemeral Stories
* **24-Hour Lifespan:** Share moments that automatically disappear after a day.
* **Intuitive UI:** Tap left/right to navigate between story segments, complete with a top progress bar just like Instagram.
* **View Tracking:** See who has viewed your active stories.

### 📞 Audio & Video Calling (Agora API)
* **Real-Time VoIP:** Seamless 1-on-1 audio and video calls integrated directly into the chat interface.
* **Low Latency:** Powered by the Agora SDK for enterprise-grade WebRTC communication.
* **Call Controls:** Toggle camera, mute microphone, and handle incoming/outgoing call states gracefully.

### 💬 Real-Time Messaging
* **Instant Chat:** 1-on-1 direct messaging using Firebase's real-time listeners.
* **Media Messages:** Send photos and updates instantly within the chat thread.
* **Chat Previews:** A dedicated inbox screen showing all active conversations and the latest message snippets.

### 👥 Profile & Network Management
* **Follow System:** Send, accept, or decline follow requests to build a curated network.
* **Profile Grids:** A dedicated profile screen displaying user info, follower/following counts, and a grid view of all historical posts.
* **Dynamic Privacy:** Logic to handle public vs. private account interactions.

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Mobile** | Android (Kotlin) |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 36 |
| **UI** | XML Layouts, Material Components, RecyclerView |
| **Image Loading** | Glide |
| **Auth** | Firebase Authentication |
| **Database & Storage** | Firebase Firestore (NoSQL) & Firebase Cloud Storage |
| **A/V Calling** | Agora SDK (WebRTC) |
| **Concurrency** | Kotlin Coroutines |

## 📂 Project Structure

```text
Socially/
├── app/
│   ├── build.gradle.kts          # Dependencies & build config
│   ├── google-services.json      # Firebase configuration
│   └── src/main/
│       ├── AndroidManifest.xml   # Activities, services, camera/mic permissions
│       ├── java/com/Harris/socially/
│       │   ├── 🔐 Auth
│       │   │   ├── LoginActivity.kt
│       │   │   └── RegisterActivity.kt
│       │   │
│       │   ├── 📱 Feed & Posts
│       │   │   ├── FeedFragment.kt
│       │   │   ├── CreatePostActivity.kt
│       │   │   └── PostAdapter.kt
│       │   │
│       │   ├── ⏳ Stories
│       │   │   ├── StoryViewerActivity.kt
│       │   │   └── StoryAdapter.kt
│       │   │
│       │   ├── 📞 Calling (Agora)
│       │   │   ├── VideoCallActivity.kt
│       │   │   ├── AudioCallActivity.kt
│       │   │   └── AgoraManager.kt
│       │   │
│       │   ├── 💬 Chat
│       │   │   ├── InboxFragment.kt
│       │   │   ├── ChatActivity.kt
│       │   │   └── MessageAdapter.kt
│       │   │
│       │   └── 👥 Profile & Network
│       │       ├── ProfileFragment.kt
│       │       ├── FollowRequestsActivity.kt
│       │       └── GridAdapter.kt
│       │
│       └── res/
│           ├── layout/           # XML layout files
│           ├── drawable/         # Icons, backgrounds, custom shapes
│           └── values/           # Colors, strings, themes
│
└── build.gradle.kts              # Root build file
```

## 🚀 Getting Started

### Prerequisites
* Android Studio Ladybug or later
* JDK 11+
* A Firebase Account
* An Agora Developer Account

### Setup Instructions
1.  **Clone this repository:**
    ```bash
    git clone https://github.com/your-username/Socially.git
    ```
2.  **Open the project** in Android Studio.
3.  **Firebase Setup:**
    * Create a new project in the Firebase Console.
    * Enable Authentication, Firestore, and Cloud Storage.
    * Download your `google-services.json` file and place it in the `app/` directory.
4.  **Agora Setup:**
    * Create an Agora project to get your **App ID**.
    * Add your App ID to the project's `strings.xml` or secure environment variables.
5.  **Build and Run** on an Android device or emulator (API 24+). *Note: Video/Audio calling requires a physical device to test camera and microphone inputs properly.*

## 🏗️ Architecture Highlights

* **Real-Time Data Sync:** Utilizes Firestore Snapshot Listeners to ensure likes, comments, and messages update instantly across all active devices without requiring manual refreshes.
* **Media Pipeline:** Heavy emphasis on background processing using Kotlin Coroutines to compress images before Firebase upload, saving user bandwidth and reducing storage costs.
* **Decoupled SDK Integration:** The Agora SDK logic is isolated into a dedicated manager class, preventing monolithic activities and making the calling feature highly maintainable.

## 🧠 Key Learnings

* **NoSQL Data Modeling:** Structuring a highly relational concept (like followers, feeds, and nested comments) inside a NoSQL environment (Firestore) for maximum read efficiency.
* **WebRTC & State Management:** Handling the complex lifecycle of the Agora SDK, including managing camera/microphone states, handling background interruptions, and cleaning up resources to prevent memory leaks.
* **Custom UI Engineering:** Building the complex, tap-to-navigate Story UI with synchronized progress bars mimicking industry standards.

## 📄 License

This project is for educational and portfolio purposes.
