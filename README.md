# GAppAuth

GAppAuth is an SDK for applications installed on Android devices like phones, tablets use Google's OAuth 2.0 endpoints to authorize access to Google APIs.

## Usage

1. Add codes below in project build.gradle file.
```java
allprojects {
	repositories {
		...
		maven { url 'https://www.jitpack.io' }
	}
}
```
5. Add codes below in app build.gradle file.
```java
dependencies {
    implementation 'com.github.billtom20:GAppAuth:main-SNAPSHOT'
    ...
}
```