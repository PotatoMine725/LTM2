# TCP Chat Server with Image Transfer (Java)

## Project Overview

This project is a Java-based TCP Chat Application running entirely on localhost (127.0.0.1). It supports:

- Text messaging between Client and Server  
- Image transfer from Client to Server  
- Multi-threaded Server (multiple clients at the same time)  
- Graphical User Interface (GUI) for both Server and Client  

This system is designed for learning and local testing only, not for deployment.

---

## System Description

The system consists of two main applications:

### Server Application (with GUI)

The Server includes a management interface (GUI) to monitor and control the system.

#### Server Responsibilities:

- Accept multiple client connections concurrently
- Display connection status
- Show received messages in real-time
- Display logs (events, errors)
- Save received images
- Allow basic control (start/stop server)

---

### Client Application (with GUI)

The Client provides a user-friendly interface for interacting with the server.

#### Client Responsibilities:

- Connect to server via localhost
- Send text messages
- Select and send image files
- Display server responses (if any)
- Disconnect safely

---

## GUI Design Overview

### Server GUI Features

- Server Status Panel  
  - Port number  
  - Running / Stopped state  

- Log Panel  
  - Connection events  
  - Errors  
  - File transfer status  

- Message Viewer  
  - Displays incoming messages from client  

- Control Buttons  
  - Start Server  
  - Stop Server  

---

### Client GUI Features

- Connection Panel  
  - Input: Host (127.0.0.1)  
  - Input: Port  
  - Connect / Disconnect button  

- Chat Panel  
  - Text input field  
  - Send button  
  - Chat display area  

- File Transfer Panel  
  - Choose image button  
  - Send image button  
  - Display selected file name  

---

## System Workflow

### 1. Start Server (via GUI)

- User clicks Start Server  
- Server binds to port (e.g., 8080)  
- GUI updates status → "Running"  

#### Build
```powershell
& "D:\Tools\apache-maven-3.9.9\bin\mvn.cmd" clean package
```

#### Run Server (fat jar)
```powershell
java -jar target\ltm2-chat-1.0.0-all.jar
```

#### Run Server (portable)
```powershell
.\dist\LTM2-Server\LTM2-Server.exe
```

---

### 2. Client Connects

- User enters:
  - Host: 127.0.0.1  
  - Port: 8080  
- Click Connect  

#### Run Client (fat jar)
```powershell
java -cp target\ltm2-chat-1.0.0-all.jar client.ClientLauncher
```

#### Run Client (portable)
```powershell
.\dist\LTM2-Client\LTM2-Client.exe
```

---

### 3. Communication

- Client sends:
  - TEXT → appears in Server GUI  
  - IMAGE → saved and logged  

- Server GUI updates in real-time:
  - Message content  
  - File received notification  

---

### 4. Disconnect

- Client clicks Disconnect  
- Server logs disconnection  
- Connection closes safely  

---

## Multi-threaded Behavior

- Server processes multiple client sessions concurrently  
- GUI remains responsive while each client is handled on its own thread  
- Multi-client support is enabled  

---

## Communication Protocol

### Message Types

| Type       | Description |
|------------|------------|
| TEXT       | Chat message |
| IMAGE      | Image transfer |
| DISCONNECT | Close connection |

---

### IMAGE Format

```

IMAGE <filename> <filesize> <binary data>

```

---

## Security Considerations

Even in localhost, basic protections are required:

- Validate file name (prevent path traversal)  
- Limit file size  
- Do not trust client input  
- Handle malformed requests safely  
- Close resources properly  

---

## Code Documentation Rules

All critical sections must be commented, especially:

- Socket initialization  
- Connection handling  
- Message parsing  
- File transfer logic  
- Error handling  

---

## Suggested Technologies for GUI

- Java Swing (simple, lightweight)  
- JavaFX (modern UI)  

---

## Project Structure

```
/project-root
├── Docs/
│   ├── Plans/
│   ├── Reports/
│   └── Reviews/
├── src/
│   ├── client/
│   │   ├── ClientApp.java
│   │   ├── ChatClient.java
│   │   └── ClientFrame.java
│   ├── server/
│   │   ├── ServerApp.java
│   │   ├── ChatServer.java
│   │   └── ServerFrame.java
│   └── shared/
│       └── Protocol.java
├── received_images/
└── README.md
```

---

## Limitations

- Local-only use  
- No encryption  
- No authentication  
- Not designed for internet deployment  

---

## Learning Outcomes

After completing this project, you will understand:

- TCP socket programming in Java  
- GUI integration with networking  
- File transfer using streams  
- Basic security practices  
- Designing simple communication protocols  

---

## Summary

This project simulates a chat and file transfer system with GUI, running entirely on localhost.

It combines:

- Backend (Server logic)  
- Frontend (GUI for both sides)  
- Networking (TCP communication)  

This provides a solid foundation for more advanced systems such as multi-client servers and real-time applications.
```
