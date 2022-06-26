# GAppAuth

This document explains how applications installed on Android devices like phones, tablets use GAppAuth(based on OAuth 2.0) endpoints to authorize access to Google APIs.

GAppAuth allows users to share specific data with an application while keeping their usernames, passwords, and other information private. For example, an application can use GAppAuth to obtain permission from users to store files in their Google Drives.

Installed apps are distributed to individual devices, and it is assumed that these apps cannot keep secrets. They can access Google APIs while the user is present at the app or when the app is running in the background.

This authorization flow is similar to the one used for web server applications. The main difference is that installed apps must open the system browser and supply a local redirect URI to handle responses from Google's authorization server.

