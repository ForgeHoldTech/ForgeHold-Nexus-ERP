# ForgeHold Nexus ERP

A desktop inventory, billing and sales management system built with Java and Swing.

## Overview

ForgeHold Nexus ERP is a lightweight, offline desktop ERP application designed for small businesses to manage products, stock, customers, billing and sales reporting from a single application.

## Features

- **Dashboard** — At-a-glance summary of business activity
- **Products** — Add, update, delete and search product records
- **Stock** — Track stock movements (in/out), view stock history, and monitor low-stock items
- **Customers** — Manage customer records with search support
- **Billing** — Build a cart, checkout sales, view saved bills, and generate or print invoice PDFs
- **Reports** — Filter sales by date range and export sales reports
- **Login System** — Single-admin login with the ability to change username and password
- **Invoice Generation** — Invoices are generated as PDF files using a custom, dependency-free PDF writer

## Tech Stack

- Java 21
- Java Swing (GUI)
- SQLite (local database)
- JDBC (database connectivity)
- Apache Maven (build tool)
- Maven Shade Plugin (fat JAR packaging)
- jpackage (Windows installer)

## Project Structure

ForgeHold Nexus ERP/
├── src/Main/Java/
│ ├── Main.java
│ ├── DBConnection.java
│ └── ConnectionTest.java
├── installer/
├── pom.xml
└── README.md


## Getting Started

### Prerequisites

- Java Development Kit (JDK) 21 or later
- Apache Maven

### Build and Run

mvn clean package
java -jar target/ForgeHold-Nexus-ERP.jar


A pre-built Windows installer is also available in the `installer` folder.

## Author

**Usman Ahmad**
Forgehold Technologies

- GitHub: [github.com/ForgeHoldTech](https://github.com/ForgeHoldTech)
- Portfolio: [forgeholdtech.github.io](https://forgeholdtech.github.io)
- Email: uahmad1666@gmail.com
