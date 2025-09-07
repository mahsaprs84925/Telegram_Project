# Telegram Clone

A desktop messaging application inspired by Telegram, built with Java, JavaFX, and PostgreSQL.

## Table of Contents

- [Description](#description)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [API Documentation](#api-documentation)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Description

This is a functional messaging platform that allows users to communicate through private chats, group chats, and channels. The application demonstrates object-oriented programming principles, JavaFX GUI design, and PostgreSQL database integration.

### Key Technologies

- **Java 11+**: Core programming language
- **JavaFX**: User interface framework
- **PostgreSQL**: Database management system
- **Gradle**: Build automation and dependency management
- **BCrypt**: Password hashing and security
- **SLF4J + Logback**: Logging framework

## Features

### Core Features ✅

- **User Account Management**
  - User registration with secure password requirements
  - User authentication and login
  - Profile management (name, bio, profile picture)
  - User status tracking (Online/Offline/Typing)

- **Private Chats**
  - One-on-one messaging between users
  - Real-time message delivery
  - Message history persistence
  - Image sharing capabilities
  - User search functionality

- **Group Chats**
  - Create and manage group conversations
  - Add/remove members
  - Admin privileges for group creators
  - Group profile information
  - Member list management

- **Channels**
  - Broadcast messaging to multiple subscribers
  - Channel creation and management
  - Subscription/unsubscription functionality
  - Admin-only posting capabilities
  - Public and private channel support

- **Modern UI/UX**
  - Telegram-inspired interface design
  - Responsive layout with proper styling
  - Intuitive navigation and user interactions
  - Message bubbles with sender identification
  - Real-time chat updates

### Bonus Features 🌟

- **File Sharing**: Send and receive images and other file types
- **Message Search**: Search through message history
- **Advanced UI**: Custom CSS theming and modern design
- **Security**: Password strength validation and secure hashing

## Prerequisites

Before running this application, ensure you have the following installed:

- **Java Development Kit (JDK) 11 or higher**
- **PostgreSQL 12 or higher**
- **Gradle 8.0 or higher** (optional - included wrapper)

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/telegram-clone.git
cd telegram-clone
```

### 2. Database Setup

1. Install and start PostgreSQL
2. Create a database named `telegram_clone`:

```sql
CREATE DATABASE telegram_clone;
```

3. Update database configuration in `DatabaseManager.java`:

```java
private static final String DB_URL = "jdbc:postgresql://localhost:5432/telegram_clone";
private static final String DB_USER = "your_username";
private static final String DB_PASSWORD = "your_password";
```

### 3. Build the Application

```bash
gradle build
```

### 4. Run the Application

```bash
# Using Gradle
gradle run
```

## Usage

### Getting Started

1. **Launch the Application**: Run the application using the commands above
2. **Register**: Click "Register" to create a new account
   - Choose a unique username (3-30 characters, alphanumeric + underscore)
   - Create a strong password (8+ characters with upper, lower, and digit)
   - Enter your display name and optional bio
3. **Login**: Use your credentials to sign in

### Core Operations

#### Starting Conversations

- **Private Chat**: Click "New Chat" and enter a username or user ID
- **Group Chat**: Click "New Group", enter group name and description
- **Channel**: Click "New Channel", configure name, description, and privacy settings

#### Messaging

- Select any chat from the sidebar
- Type your message in the input field at the bottom
- Press Enter or click the send button
- Use the attachment button (📎) to share images and files

#### User Management

- Click your profile button to access profile settings
- Search for users using the search bar
- Manage group/channel members through the context menu

### Navigation

- **Sidebar**: Lists all your chats, groups, and channels
- **Main Area**: Displays selected conversation
- **Search**: Find users, groups, and channels
- **Profile Menu**: Access settings and logout

## Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────┐
│           Presentation Layer        │
│     (Controllers + FXML Views)      │
├─────────────────────────────────────┤
│            Service Layer            │
│   (UserService, MessageService,    │
│            ChatService)             │
├─────────────────────────────────────┤
│         Data Access Layer           │
│    (UserDAO, MessageDAO, etc.)     │
├─────────────────────────────────────┤
│            Model Layer              │
│   (User, Message, Chat entities)   │
├─────────────────────────────────────┤
│           Database Layer            │
│         (PostgreSQL DB)            │
└─────────────────────────────────────┘
```

### Design Patterns Used

- **Singleton Pattern**: For service classes and database manager
- **DAO Pattern**: For data access abstraction
- **MVC Pattern**: Separation of concerns in UI components
- **Factory Pattern**: For creating database connections
- **Observer Pattern**: For real-time UI updates

### Key Classes

- `TelegramApp`: Main application entry point
- `DatabaseManager`: Singleton for database connection management
- `UserService`: Business logic for user operations
- `MessageService`: Handles message sending and retrieval
- `ChatService`: Manages private chats, groups, and channels
- `MainController`: Primary UI controller for the main interface

## Database Schema

### Tables Overview

```sql
-- Users table
users (
    user_id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    profile_name VARCHAR(100) NOT NULL,
    profile_picture_path VARCHAR(500),
    status VARCHAR(20) DEFAULT 'OFFLINE',
    bio TEXT DEFAULT '',
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)

-- Messages table
messages (
    message_id VARCHAR(255) PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) DEFAULT 'TEXT',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'SENT',
    reply_to_message_id VARCHAR(255),
    media_path VARCHAR(500)
)

-- And more tables for private_chats, group_chats, channels, etc.
```

### Entity Relationships

- **Users** have many **Private Chats**
- **Users** can be members of many **Group Chats**
- **Users** can subscribe to many **Channels**
- **Messages** belong to one sender and one receiver (user/group/channel)

## API Documentation

### Core Services

#### UserService

```java
// User registration
boolean registerUser(String username, String password, String profileName)

// User authentication
boolean loginUser(String username, String password)

// Search users
List<User> searchUsers(String searchTerm)
```

#### MessageService

```java
// Send message
boolean sendMessage(Message message)

// Get chat messages
List<Message> getPrivateChatMessages(String user1Id, String user2Id, int limit)
```

#### ChatService

```java
// Create private chat
PrivateChat createOrGetPrivateChat(String user1Id, String user2Id)

// Create group
GroupChat createGroup(String groupName, String description, String creatorId)

// Create channel
Channel createChannel(String channelName, String description, String ownerId, boolean isPrivate)
```

## Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Follow Java naming conventions
- Write comprehensive JavaDoc comments
- Add unit tests for new features
- Ensure database migrations are included
- Update documentation for API changes

## Changelog

### Version 1.0.0 (Current)

#### Added
- Complete user authentication system
- Private chat functionality
- Group chat creation and management
- Channel broadcasting capabilities
- Image sharing in chats
- Modern JavaFX UI with custom styling
- PostgreSQL database integration
- Comprehensive logging system

#### Security
- BCrypt password hashing
- Input validation and sanitization
- SQL injection prevention with prepared statements

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Program made by

- Nazanin Fatemi
- Mahsa Pooresmaeil

## Contact

**Project Team**: Advanced Programming Class - Summer 2025

**Course Instructor**: Dr. Saeed Reza Kheradpisheh

**University**: Shahid Beheshti University



### Acknowledgments

- Telegram for UI/UX inspiration
- JavaFX community for excellent documentation
- PostgreSQL team for robust database capabilities
- Course instructor and TAs for guidance and support



