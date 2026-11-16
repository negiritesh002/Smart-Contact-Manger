This is the Smart Contact manger app,
It mainly focuses on the backend part and I used some thymeleaf and bootstrap for the frontend.

## Run locally on Windows

Prerequisites: a JDK (Java 17 works with the override below), a running MySQL
server on port 3306, and internet access for the first Maven dependency download.
Maven is included through `mvnw.cmd`; a separate Maven installation is unnecessary.

1. In MySQL Workbench or the MySQL command line, create the database:

   ```sql
   CREATE DATABASE IF NOT EXISTS smartcontact;
   ```

2. Open PowerShell in the project folder and configure your database credentials
   for that terminal session:

   ```powershell
   cd C:\dev\smart\Smart-Contact-Manger
   $env:SPRING_DATASOURCE_USERNAME = 'root'
   $dbCredential = Get-Credential -UserName $env:SPRING_DATASOURCE_USERNAME -Message 'Enter your MySQL credentials'
   $env:SPRING_DATASOURCE_USERNAME = $dbCredential.UserName
   $env:SPRING_DATASOURCE_PASSWORD = $dbCredential.GetNetworkCredential().Password
   ```

   These variables override `src/main/resources/application.properties` without
   saving your password in the repository. If needed, set a different connection
   URL using `$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/smartcontact'`.

3. Start the application:

   ```powershell
   .\mvnw.cmd '-Djava.version=17' spring-boot:run
   ```

   The POM currently targets Java 25, so keep the Java 17 override when using
   Java 17. With JDK 25, omit `'-Djava.version=17'`.

4. Wait for the application startup message, then open <http://localhost:8080>.
   Register at <http://localhost:8080/signup> and sign in at
   <http://localhost:8080/signin>. Hibernate creates/updates the tables in the
   configured database on startup. Press **Ctrl+C** in PowerShell to stop it.

To build a runnable JAR (the database must be available for the context test):

```powershell
.\mvnw.cmd '-Djava.version=17' clean package
java -jar .\target\Smartcontactmanager-0.0.1-SNAPSHOT.jar
```

Password recovery sends email and requires valid SMTP settings. You can override
the configured mail account with `SPRING_MAIL_USERNAME` and `SPRING_MAIL_PASSWORD`.
