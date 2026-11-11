package quickstart.admin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;

/**
 * Database has all the logic for connecting to and interacting with a database
 * Note: This class is hard-coded for SQLite.
 */
// 4. Database implements AutoCloseable so that it will be easy to avoid resource leaks
public class Database implements AutoCloseable{
    // Load the sqlite-JDBC driver when the class is initialize 
    // 1. The static initialization block that loads the SQLite driver before the Database object is constructed
    static{
        try{
            Class.forName("org.sqlite.JDBC");
            Class.forName("org.postgresql.Driver");
        }
        catch (java.lang.ClassNotFoundException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** A connection to a SQLite db, or null */
    private Connection conn;

    /** True if using SQLite, false if PostgreSQL */
    private boolean sqlite = true;

    // Replaced entire constructer:
    /**
     * Use dbStr to create a connection to a database, and store it in the
     * constructed Database object
     *
     * @param useSQLite true if using a local sqlite db, false if using postgresql
     * @param dbStr     the connection string for the database (path for sqlite,
     *                  DATABASE_URL for postgresql)
     * @throws SQLException if a connection cannot be created
     */
    public Database(boolean useSQLite, String dbStr) throws SQLException {
        if (useSQLite) {
            this.sqlite = true;
            // Connect to the database or fail
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbStr);
            System.out.printf("sqlite connected to file: %s%n", dbStr);
            // SQLite is odd: we need to opt-in for referential integrity
            try (var stmt = conn.prepareStatement("PRAGMA foreign_keys = ON;")) {
                stmt.execute();
                getPragmaForeignKeysStatus();
            }
        } else { // assume postgresql
            this.sqlite = false;

            String jdbcUrl = null, username = null, password = null;
            try {
                java.net.URI dbUri = new java.net.URI(dbStr);
                jdbcUrl = String.format("jdbc:postgresql://%s:%d%s",
                        dbUri.getHost(), dbUri.getPort(), dbUri.getPath());
                String[] uname_pword = dbUri.getUserInfo().split(":");
                username = uname_pword[0];
                password = uname_pword[1];
            } catch (java.net.URISyntaxException e) {
                throw new RuntimeException("Critical problem parsing dbStr; cannot create postgresql connection.", e);
            }

            // Connect to the database or fail
            conn = DriverManager.getConnection(jdbcUrl, username, password);
        }
        if (conn == null) {
            throw new RuntimeException("Error: conn==null. (DriverManager.getConnection() returned a null object?)");
        }
    }

    /**
     * If connection isn't null, verifies that referential integrity is enabled
     * @return true if enabled, false otherwise
     */
    // 6. getPragmaForeignKeysStatus function is only called once, but it's still its own function, because we're going to want it to be a standalone function for the unit tests we'll write later
    synchronized boolean getPragmaForeignKeysStatus() throws SQLException {
        if (conn != null) {
            // 5. To avoid resource leaks, interactions with PreparedStatement objects use try-with-resources (and thus AutoCloseable)
            try (var stmt = conn.prepareStatement("PRAGMA foreign_keys;");
                    var rs = stmt.executeQuery();) {
                if (rs.next()) {
                    int status = rs.getInt(1);
                    if (status == 1)
                        System.out.println("sqlite referential integrity enabled");
                    else
                        System.out.println("WARNING: sqlite referential integrity is DISABLED");
                    return status == 1;
                } else {
                    System.err.println("ERROR: did not get a result set for PRAGMA foreign_keys when expected 0 or 1.");
                }
            }
        }
        return false;
    }

    /**
     * Forgot this close at first and got many errors when running mvn package
     * Close the current connection to the database, if one exists
     * NB: The connection will always be null after this call, even if an error occurred during the closing operation
     */
    @Override
    public void close() throws Exception {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                conn = null;
            }
        }
    }

    /**
     * Create the database tables
     *
     * @throws SQLException if any table cannot be created
     */
    synchronized void createTables() throws SQLException {
        // postgres has different syntax for both id and email
        var createTblPerson = sqlite ? """
                CREATE TABLE tblPerson (
                    id INTEGER PRIMARY KEY,
                    email VARCHAR(30) NOT NULL UNIQUE COLLATE NOCASE,
                    name VARCHAR(50)
                );""" : """
                CREATE TABLE tblPerson (
                    id SERIAL PRIMARY KEY,
                    email VARCHAR(30) NOT NULL UNIQUE CHECK (email = lower(email)),
                    name VARCHAR(50)
                );""";
        try (var ps = conn.prepareStatement(createTblPerson)) {
            ps.execute();
        }
        // Postgres has different syntax for id; Updated so varchar is 512 as required
        var createTblMessage = sqlite ? """
                CREATE TABLE tblMessage (
                    id INTEGER PRIMARY KEY,
                    subject VARCHAR(50) NOT NULL,
                    details VARCHAR(512) NOT NULL,
                    as_of DATE NOT NULL,
                    creatorId INTEGER,
                    FOREIGN KEY (creatorId) REFERENCES tblPerson(id)
                );""" : """
                CREATE TABLE tblMessage (
                    id SERIAL PRIMARY KEY,
                    subject VARCHAR(50) NOT NULL,
                    details VARCHAR(512) NOT NULL,
                    as_of DATE NOT NULL,
                    creatorId INTEGER,
                    FOREIGN KEY (creatorId) REFERENCES tblPerson(id)
                );""";
        try (var ps = conn.prepareStatement(createTblMessage)) {
            ps.execute();
        }
        var createTblReply = sqlite ? """
                CREATE TABLE tblReply (
                    id INTEGER PRIMARY KEY,
                    subject VARCHAR(50) NOT NULL,
                    details VARCHAR(500) NOT NULL,
                    as_of DATE NOT NULL,
                    creatorId INTEGER,
                    messageId INTEGER,
                    FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                    FOREIGN KEY (messageId) REFERENCES tblMessage(id)
                );""" : """
                CREATE TABLE tblReply (
                    id SERIAL PRIMARY KEY,
                    subject VARCHAR(50) NOT NULL,
                    details VARCHAR(500) NOT NULL,
                    as_of DATE NOT NULL,
                    creatorId INTEGER,
                    messageId INTEGER,
                    FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                    FOREIGN KEY (messageId) REFERENCES tblMessage(id)
                );""";
        try (var ps = conn.prepareStatement(createTblReply)){
            ps.execute();
        }
        var createTblLikes = sqlite ? """
            CREATE TABLE tblLikes (
                id INTEGER PRIMARY KEY,
                creatorId INTEGER,
                messageId INTEGER,
                replyId INTEGER,
                liked_at DATE NOT NULL,
                FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                FOREIGN KEY (messageId) REFERENCES tblMessage(id),
                FOREIGN KEY (replyId) REFERENCES tblReply(id)
            );""" : """
            CREATE TABLE tblLikes (
                id SERIAL PRIMARY KEY,
                creatorId INTEGER,
                messageId INTEGER,
                replyId INTEGER,
                liked_at DATE NOT NULL,
                FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                FOREIGN KEY (messageId) REFERENCES tblMessage(id),
                FOREIGN KEY (replyId) REFERENCES tblReply(id)
            );""";
            try(var ps = conn.prepareStatement(createTblLikes)){
                ps.execute();
            }
            System.out.println("Tables created successfully");

        var createTblFile = sqlite ? """
            CREATE TABLE tblFile (
                id INTEGER PRIMARY KEY,
                creatorId INTEGER,
                messageId INTEGER,
                replyId INTEGER,
                file_url TEXT NOT NULL,
                file_name VARCHAR(100),
                mime_type VARCHAR(50),
                as_of DATE NOT NULL,
                last_accessed_at DATE,
                valid BOOLEAN DEFAULT TRUE,
                FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                FOREIGN KEY (messageId) REFERENCES tblMessage(id),
                FOREIGN KEY (replyId) REFERENCES tblReply(id)
            );""" : """
            CREATE TABLE tblFile (
                id SERIAL PRIMARY KEY,
                creatorId INTEGER,
                messageId INTEGER,
                replyId INTEGER,
                file_url TEXT NOT NULL,
                file_name VARCHAR(100),
                mime_type VARCHAR(50),
                as_of DATE NOT NULL,
                last_accessed_at DATE,
                valid BOOLEAN DEFAULT TRUE,
                FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                FOREIGN KEY (messageId) REFERENCES tblMessage(id),
                FOREIGN KEY (replyId) REFERENCES tblReply(id)
            );""";
        try (var ps = conn.prepareStatement(createTblFile)) {
            ps.execute();
        }

        var createTblLink = sqlite ? """
            CREATE TABLE tblLink (
                id INTEGER PRIMARY KEY,
                creatorId INTEGER,
                messageId INTEGER,
                replyId INTEGER,
                url TEXT NOT NULL,
                as_of DATE NOT NULL,
                last_accessed_at DATE,
                valid BOOLEAN DEFAULT TRUE,
                FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                FOREIGN KEY (messageId) REFERENCES tblMessage(id),
                FOREIGN KEY (replyId) REFERENCES tblReply(id)
            );""" : """
            CREATE TABLE tblLink (
                id SERIAL PRIMARY KEY,
                creatorId INTEGER,
                messageId INTEGER,
                replyId INTEGER,
                url TEXT NOT NULL,
                as_of DATE NOT NULL,
                last_accessed_at DATE,
                valid BOOLEAN DEFAULT TRUE,
                FOREIGN KEY (creatorId) REFERENCES tblPerson(id),
                FOREIGN KEY (messageId) REFERENCES tblMessage(id),
                FOREIGN KEY (replyId) REFERENCES tblReply(id)
            );""";
        try (var ps = conn.prepareStatement(createTblLink)) {
            ps.execute();
        }
    }

    /**
     * Create the database views
     * @throws SQLException if any view cannot be created
     */
    synchronized void createViews() throws SQLException {
    var createViewMessage = """
        CREATE VIEW viewMessage AS
        SELECT
        tblMessage.id AS id,
        tblMessage.subject AS subject,
        tblMessage.details AS details,
        tblMessage.as_of AS as_of,
        tblMessage.creatorId AS creatorId,
        tblPerson.email AS email,
        tblPerson.name AS name,
        tblMessage.valid AS valid,

        (SELECT COUNT(*)
        FROM tblLikes
        WHERE tblLikes.messageId = tblMessage.id) AS likeCount,

        (SELECT COUNT(*)
        FROM tblReply
        WHERE tblReply.messageId = tblMessage.id) AS replyCount

        FROM tblMessage
        INNER JOIN tblPerson
        ON tblMessage.creatorId = tblPerson.id;
        """;
    try (var ps = conn.prepareStatement(createViewMessage)) {
        ps.execute();
    }
    var createViewReply = """
        CREATE VIEW viewReply AS
        SELECT
            tblReply.id AS id,
            tblReply.subject AS subject,
            tblReply.details AS details,
            tblReply.as_of AS as_of,
            tblReply.creatorId AS creatorId,
            tblReply.messageId AS messageId,
            tblPerson.email AS email,
            tblPerson.name AS name,
            (SELECT COUNT(*)
            FROM tblLikes
            WHERE tblLikes.replyId = tblReply.id) AS likeCount

            FROM tblReply
            INNER JOIN tblPerson
            ON tblReply.creatorId = tblPerson.id;
        """;
    try (var ps = conn.prepareStatement(createViewReply)) {
        ps.execute();
    }
    System.out.println("Views created successfully");
}
    /**
     * Adds additional columns introduced in Phase 3 to existing tables.
     * Includes fields like valid, gender_identity, sexual_orientation, and note.
     *
     * @throws SQLException if the columns cannot be added
     */
    synchronized void addPhase3Columns() throws SQLException {
        try (var stmt = conn.createStatement()) {

            // tblPerson
            try { stmt.execute("ALTER TABLE tblPerson ADD COLUMN valid BOOLEAN DEFAULT TRUE"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE tblPerson ADD COLUMN gender_identity VARCHAR(50)"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE tblPerson ADD COLUMN sexual_orientation VARCHAR(50)"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE tblPerson ADD COLUMN note VARCHAR(150)"); } catch (SQLException e) {}
            System.out.println("tblPerson columns added!");

            // tblMessage
            try { stmt.execute("ALTER TABLE tblMessage ADD COLUMN valid BOOLEAN DEFAULT TRUE"); } catch (SQLException e) {}
            System.out.println("tblMessage columns added!");

            // tblReply
            try { stmt.execute("ALTER TABLE tblReply ADD COLUMN valid BOOLEAN DEFAULT TRUE"); } catch (SQLException e) {}
            System.out.println("tblReply columns added!");
        }
    }

    /**
     * Remove all tables from the database
     * @throws SQLException if any table cannot be dropped
     */
    synchronized void dropTables() throws SQLException {
        var dropTblFile = "DROP TABLE IF EXISTS tblFile;";
        try (var ps = conn.prepareStatement(dropTblFile)) {
            ps.execute();
        }
        var dropTblLink = "DROP TABLE IF EXISTS tblLink;";
        try (var ps = conn.prepareStatement(dropTblLink)) {
            ps.execute();
        }
        var dropTblLikes = "Drop Table IF EXISTS tblLikes;";
        try (var ps = conn.prepareStatement(dropTblLikes)) {
            ps.execute();
        }
        var dropTblReply = "Drop TABLE IF EXISTS tblReply;";
        try (var ps = conn.prepareStatement(dropTblReply)) {
            ps.execute();
        } 
        var dropTblMessage = "DROP TABLE IF EXISTS tblMessage;";
        try (var ps = conn.prepareStatement(dropTblMessage)) {
            ps.execute();
        }
        var dropTblPerson = "DROP TABLE IF EXISTS tblPerson;";
        try (var ps = conn.prepareStatement(dropTblPerson)) {
            ps.execute();
        }
        System.out.println("Tables dropped successfully");
    }

    /**
     * Remove all views from the database
     * @throws SQLException if any view cannot be dropped
     */
    synchronized void dropViews() throws SQLException {
        var dropViewMessage = "DROP VIEW IF EXISTS viewMessage;";
        try (var ps = conn.prepareStatement(dropViewMessage)) {
            ps.execute();
        }
        var dropViewReply = "DROP VIEW IF EXISTS viewReply;";
        try (var ps = conn.prepareStatement(dropViewReply)){
            ps.execute();
        } 
        System.out.println("Views dropped successfully");
    }
    
    // Operations for tblPerson (11.9)
    /**
     * Perform lightweight validation of the provided email address.
     * @param email The address to validate
     * @throws RuntimeException if the address does not meet length requirements
     */
    private static synchronized void validateEmail(String email) throws RuntimeException {
        if (email == null || email.length() < 3 || email.length() > 30) {
            throw new RuntimeException("Invalid email address");
        }
    }

    /**
     * Perform lightweight validation of the provided display name
     * @param name The name to validate
     * @throws RuntimeException if the address does not meet length requirements
     */
    private static synchronized void validateName(String name) throws RuntimeException {
        if (name == null || name.length() < 1 || name.length() > 50) {
            throw new RuntimeException("Invalid name");
        }
    }

    /**
     * Create a new Person in the database. Note that uniqueness of email
     * addresses is enforced by the database itself, which lets this code remain
     * clean and simple.
     * @param email The new person's email address
     * @param name The new person's name
     * @throws SQLException If the person cannot be created
     * @throws RuntimeException If the provided data is invalid
     * @return the id of the new row upon success, Integer.MIN_VALUE on failure
     */
    synchronized int insertPerson(String email, String name) throws SQLException, RuntimeException {
        // First validate the email and name w functions from above (bc "trust nobody")
        Database.validateEmail(email);
        Database.validateName(name);

        // NB: The PreparedStatement uses a second parameter to request the row Id of the created row
        try (var stmt = conn.prepareStatement("INSERT INTO tblPerson (email, name) VALUES (?, ?) RETURNING id;");) {
            stmt.setString(1, email);
            stmt.setString(2, name);
            if( stmt.execute() ){
                try (ResultSet rs = stmt.getResultSet()) {
                    if( rs.next() ){ // retrieves the id of the new row
                        return rs.getInt(1);
                    }
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    /**
     * Delete a person from the database
     * @param id The Id of the person to delete
     * @throws SQLException If the person cannot be deleted
     */
    synchronized void deletePerson(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("DELETE FROM tblPerson WHERE id = ?");) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Update a person's data in the database
     * @apiNote If this function throws a SQLException, the connection autocommit setting can be in an undefined or invalid state
     *          Callers will not be able to recover in that case, so they should treat SQLExceptions as fatal
     *          Basically warns the caller rather than fixing the bug
     * @param id    The Id of the person to update
     * @param email The email (might be the same as before)
     * @param name  The name (might be the same as before)
     * @throws SQLException     If the person cannot be updated
     * @throws RuntimeException If the provided data is invalid
     */
    synchronized void updatePerson(int id, String email, String name) throws SQLException, RuntimeException {
        Database.validateEmail(email);
        Database.validateName(name);

        /*
        Even though this is "demonstration" code, and not actually something you'd want to have in a real system, it's important for it to be
        correct. The issue is that technically, this code can change a user's email address. When inserting, the code ensured that emails were unique, 
        using a transaction. That effort is useless unless this code also enforces uniqueness. Hence a transaction is needed here, too.
        */
        conn.setAutoCommit(false);

        try (var stmt = conn.prepareStatement("SELECT * FROM tblPerson WHERE email = ? and id <> ?");) {
            stmt.setString(1, email);
            stmt.setInt(2, id);
            try (var rs = stmt.executeQuery();) {
                if (rs.next()) {
                    conn.commit();
                    conn.setAutoCommit(true); // return to non-transaction mode
                    throw new RuntimeException("Email already in use");
                }
            }
        }
        try (var stmt = conn.prepareStatement("UPDATE tblPerson SET email = ?, name = ? WHERE id = ?;");) {
            stmt.setString(1, email);
            stmt.setString(2, name);
            stmt.setInt(3, id);
            stmt.executeUpdate();
            conn.commit();
            conn.setAutoCommit(true); // return to non-transaction mode
        }
    }

    /** 
     * Person is a Java object that matches the contents of tblPerson 
     * An immutable data object representing one row in tblPerson
     * Using a record automatically provides constructor, getters, equals, and hashCode
    */
    public static record Person(int id, String email, String name) {}

    /**
     * Get all data for a single person
     * @param id The Id of the person to get
     * @return a Person object representing the data that was retrieved from the database, or null if no person was found
     * @throws SQLException on any error
     */
    synchronized Person getOnePerson(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM tblPerson WHERE id = ?;");) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery();) {
                if (rs.next()) {
                    return new Person(rs.getInt("id"), rs.getString("email"), rs.getString("name"));
                }
            }
        }
        return null; // Return null if no matching person exists
    }

    /**
     * Get all data for all people in the database
     * @return A List with zero or more Person objects
     * @throws SQLException on any error
     */
    synchronized List<Person> getAllPerson() throws SQLException {
        try (var ps = conn.prepareStatement("SELECT * FROM tblPerson;");
                var rs = ps.executeQuery();) {
            var results = new ArrayList<Person>(); // Build result list from query results
            while (rs.next()) {
                results.add(new Person(rs.getInt("id"), rs.getString("email"), rs.getString("name")));
            }
            return results;
        }
    }

    // Operations for tblMessage now (11.10)
    /**
     * Perform lightweight validation of the provided message subject
     * @param subject The subject text to validate
     * @throws RuntimeException if the subject does not meet length requirements
     */
    private static synchronized void validateSubject(String subject) throws RuntimeException {
        if (subject == null || subject.length() < 1 || subject.length() > 50) {
            throw new RuntimeException("Invalid subject");
        }
    }

    /**
     * Perform lightweight validation of the provided message details
     * @param details The details text to validate
     * @throws RuntimeException if the details do not meet length requirements
     */
    private static synchronized void validateDetails(String details) throws RuntimeException {
        if (details == null || details.length() < 1 || details.length() > 500) {
            throw new RuntimeException("Invalid details");
        }
    }

    /**
     * Create a new message in the database
     * @param subject The subject
     * @param details The details
     * @param creatorId The Id of the user creating the message
     * @throws SQLException If the person cannot be created
     * @throws RuntimeException If the provided data is invalid
     */
    synchronized void insertMessage(String subject, String details, int creatorId)
            throws SQLException, RuntimeException {
        Database.validateSubject(subject);
        Database.validateDetails(details);
        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblMessage (subject, details, as_of, creatorId) VALUES (?, ?, ?, ?);");) {
            stmt.setString(1, subject);
            stmt.setString(2, details);
            stmt.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(4, creatorId);
            stmt.executeUpdate();
        }
    }

    /**
     * Update a message in the database
     * @param id The Id of the message to update
     * @param subject The subject (might be the same as before)
     * @param details The details (might be the same as before)
     * @throws SQLException If the message cannot be updated
     * @throws RuntimeException If the provided data is invalid
     */
    synchronized void updateMessage(int id, String subject, String details) throws SQLException, RuntimeException {
        Database.validateSubject(subject);
        Database.validateDetails(details);
        try (var stmt = conn
                .prepareStatement("UPDATE tblMessage SET subject = ?, details = ?, as_of = ? WHERE id = ?;");) {
            stmt.setString(1, subject);
            stmt.setString(2, details);
            stmt.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(4, id);
            stmt.executeUpdate();
        }
    }

    /**
     * Delete a message from the database
     * @param id The Id of the message to delete
     * @throws SQLException If the message cannot be deleted
     */
    synchronized void deleteMessage(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("DELETE FROM tblMessage WHERE id = ?");) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    
    // Operations for tblFile (11.11)
    /**
     * Insert a new file record into the database
     * @param creatorId The Id of the user creating the file record
     * @param messageId The Id of the message to which the file belongs (nullable)
     * @param replyId The Id of the reply to which the file belongs (nullable)
     * @param file_url The URL where the file is stored (must be non-null and non-blank)
     * @param file_name The original name of the file (must be non-null and non-blank)
     * @param mime_type The MIME type of the file (nullable)
     * @return The ID of the inserted file record, or Integer.MIN_VALUE on failure
     * @throws SQLException If the file record cannot be inserted
     * @throws RuntimeException If the provided data is invalid
     */
    synchronized int insertFile(int creatorId, Integer messageId, Integer replyId,
        String file_url, String file_name, String mime_type) throws SQLException {

        if (file_url == null || file_url.isBlank()) {
            throw new RuntimeException("Invalid file URL");
        }
        if (file_name == null || file_name.isBlank()) {
            throw new RuntimeException("Invalid file name");
        }

        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblFile (creatorId, messageId, replyId, file_url, file_name, mime_type, as_of, last_accessed_at, valid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;")) {

            stmt.setInt(1, creatorId);

            if (messageId == null) stmt.setNull(2, java.sql.Types.INTEGER);
            else stmt.setInt(2, messageId);

            if (replyId == null) stmt.setNull(3, java.sql.Types.INTEGER);
            else stmt.setInt(3, replyId);

            stmt.setString(4, file_url);
            stmt.setString(5, file_name);
            stmt.setString(6, mime_type);

            java.sql.Date now = new java.sql.Date(new java.util.Date().getTime());
            stmt.setDate(7, now);
            stmt.setDate(8, now);
            stmt.setBoolean(9, true);

            if (stmt.execute()) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return Integer.MIN_VALUE;
    }
    /**
     * Get a file record from the database by its ID    
     * @param id The ID of the file to retrieve
     * @return The file record, or null if not found
     * @throws SQLException If the file cannot be retrieved
     */
    synchronized File getOneFile(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM tblFile WHERE id = ?;")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new File(
                        rs.getInt("id"),
                        rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"),
                        (Integer) rs.getObject("replyId"),
                        rs.getString("file_name"),
                        rs.getString("file_url"),
                        rs.getString("mime_type"),
                        rs.getDate("as_of"),
                        rs.getDate("last_accessed_at"),
                        rs.getBoolean("valid")
                    );
                }
            }
        }
        return null;
    }
    /**
     * Get all file records from the database
     * @return A list of all file records
     * @throws SQLException If the files cannot be retrieved
     */
    synchronized List<File> getAllFiles() throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM tblFile;");
            var rs = stmt.executeQuery()) {

            var results = new ArrayList<File>();
            while (rs.next()) {
                results.add(new File(
                    rs.getInt("id"),
                    rs.getInt("creatorId"),
                    (Integer) rs.getObject("messageId"),
                    (Integer) rs.getObject("replyId"),
                    rs.getString("file_name"),
                    rs.getString("file_url"),
                    rs.getString("mime_type"),
                    rs.getDate("as_of"),
                    rs.getDate("last_accessed_at"),
                    rs.getBoolean("valid")
                ));
            }
            return results;
        }
    }
    /**
     * Invalidate a file record in the database by setting its valid field to false
     * @param id The ID of the file to invalidate
     * @throws SQLException If the file cannot be invalidated
     */
    synchronized void invalidateFile(int id) throws SQLException {
        try (var stmt = conn.prepareStatement(
                "UPDATE tblFile SET valid = FALSE WHERE id = ?")) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("File " + id + " invalidated successfully");
            } else {
                System.out.println("File " + id + " not found");
            }
        }
    }
    /**
     * Delete a file record from the database
     * @param id The ID of the file to delete
     * @throws SQLException If the file cannot be deleted
     */
    synchronized void deleteFile(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("DELETE FROM tblFile WHERE id = ?");) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    /**
     * Insert a new link record into the database
     * @param creatorId The Id of the user creating the link record
     * @param messageId The Id of the message to which the link belongs
     * @param replyId The Id of the reply to which the link belongs
     * @param url The URL of the link
     * @return The ID of the inserted link record
     * @throws SQLException If the link record cannot be inserted
     */
    synchronized int insertLink(int creatorId, Integer messageId, Integer replyId, String url) throws SQLException {
        if (url == null || url.isBlank()) {
            throw new RuntimeException("Invalid URL");
        }

        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblLink (creatorId, messageId, replyId, url, as_of, last_accessed_at, valid) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id;")) {

            stmt.setInt(1, creatorId);

            if (messageId == null) {
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(2, messageId);
            }

            if (replyId == null) {
                stmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(3, replyId);
            }

            stmt.setString(4, url);

            java.sql.Date now = new java.sql.Date(new java.util.Date().getTime());
            stmt.setDate(5, now);
            stmt.setDate(6, now);
            stmt.setBoolean(7, true);

            if (stmt.execute()) {
                try (ResultSet rs = stmt.getResultSet()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return Integer.MIN_VALUE;
    }
    /**
     * Get a link record from the database by its ID    
     * @param id The ID of the link to retrieve
     * @return The link record, or null if not found
     * @throws SQLException If the link cannot be retrieved
     */
    synchronized Link getOneLink(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM tblLink WHERE id = ?;")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Link(rs.getInt("id"), rs.getInt("creatorId"), rs.getInt("messageId"), rs.getInt("replyId"), rs.getString("url"), rs.getDate("as_of"), rs.getDate("last_accessed_at"), rs.getBoolean("valid"));
                }
            }
        }
        return null;
    }
    /**
     * Get all link records from the database
     * @return A list of all link records
     * @throws SQLException If the links cannot be retrieved
     */
    synchronized List<Link> getAllLinks() throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM tblLink;");
            var rs = stmt.executeQuery()) {

            var results = new ArrayList<Link>();
            while (rs.next()) {
                results.add(new Link(rs.getInt("id"), rs.getInt("creatorId"), rs.getInt("messageId"), rs.getInt("replyId"), rs.getString("url"), rs.getDate("as_of"), rs.getDate("last_accessed_at"), rs.getBoolean("valid")));
            }
            return results;
        }
    }
    /**
     * Invalidate a link record in the database by setting its valid field to false
     * @param id The ID of the link to invalidate
     * @throws SQLException If the link cannot be invalidated
     */
    synchronized void invalidateLink(int id) throws SQLException {
        try (var stmt = conn.prepareStatement(
                "UPDATE tblLink SET valid = FALSE WHERE id = ?")) {
            stmt.setInt(1, id);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Link " + id + " invalidated successfully");
            } else {
                System.out.println("Link " + id + " not found");
            }
        }
    }
    /**
     * Delete a link record from the database
     * @param id The ID of the link to delete
     * @throws SQLException If the link cannot be deleted
     */
    synchronized void deleteLink(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("DELETE FROM tblLink WHERE id = ?");) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /** Message is a Java object that matches the contents of viewMessage */ 
    public static record Message(int id, String subject, String details, Date as_of, int creatorId, String email, String name, int replyCount, int likeCount) {}

    /**
     * Get all data for a single message
     * @param id The Id of the message to get
     * @return a Message object representing the data that was retrieved from the database, or null if no message was found
     * @throws SQLException on any error
     */ 
    synchronized Message getOneMessage(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM viewMessage WHERE id = ?;");) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery();) {
                if (rs.next()) {
                    return new Message(rs.getInt("id"), rs.getString("subject"), rs.getString("details"),
                            rs.getDate("as_of"), rs.getInt("creatorId"), rs.getString("email"), rs.getString("name"), rs.getInt("replyCount"), rs.getInt("likeCount"));
                }
            }
        }
        return null;
    }

    /**
     * Get all data for all messages in the database
     * @return a List with zero or more message objects
     * @throws SQLException on any error
     */ 
    synchronized List<Message> getAllMessage() throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM viewMessage;");
                var rs = stmt.executeQuery();) {
            var results = new ArrayList<Message>();
            while (rs.next()) {
                results.add(new Message(rs.getInt("id"), rs.getString("subject"), rs.getString("details"),
                        rs.getDate("as_of"), rs.getInt("creatorId"), rs.getString("email"), rs.getString("name"), rs.getInt("replyCount"), rs.getInt("likeCount")));
            }
            return results;
        }
    }

    /**
     * Insert a new reply record into the database
     * @param subject The subject of the reply
     * @param details The details of the reply
     * @param creatorId The ID of the user creating the reply
     * @param messageId The ID of the message to which the reply belongs
     * @throws SQLException If the reply record cannot be inserted
     * @throws RuntimeException If the provided data is invalid
     */
    synchronized void insertReply(String subject, String details, int creatorId, int messageId) throws SQLException, RuntimeException {
        Database.validateSubject(subject);
        Database.validateDetails(details);
        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblReply (subject, details, as_of, creatorId, messageId) VALUES (?, ?, ?, ?, ?);");) {
            stmt.setString(1, subject);
            stmt.setString(2, details);
            stmt.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(4, creatorId);
            stmt.setInt(5, messageId);
            stmt.executeUpdate();
        }
    }
    /**
     * Update an existing reply record in the database
     * @param id The ID of the reply to update
     * @param subject The updated subject of the reply
     * @param details The updated details of the reply
     * @throws SQLException If the reply record cannot be updated
     * @throws RuntimeException If the reply record cannot be updated
     */
    synchronized void updateReply(int id, String subject, String details) throws SQLException, RuntimeException {
        Database.validateSubject(subject);
        Database.validateDetails(details);
        try (var stmt = conn
                .prepareStatement("UPDATE tblReply SET subject = ?, details = ?, as_of = ? WHERE id = ?;");) {
            stmt.setString(1, subject);
            stmt.setString(2, details);
            stmt.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(4, id);
            stmt.executeUpdate();
        }
    }
    /**
     * Delete a reply record from the database
     * @param id The ID of the reply to delete
     * @throws SQLException If the reply record cannot be deleted
     */
    synchronized void deleteReply(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("DELETE FROM tblReply WHERE id = ?");) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
    /** Reply is a Java object that matches the contents of viewReply */
    public static record Reply(int id, String subject, String details, Date as_of, int creatorId, String email, String name, int likeCount) {}

    /**
     * Get all data for a single reply
     * @param id The Id of the reply to get
     * @return a Reply object representing the data that was retrieved from the database, or null if no reply was found
     * @throws SQLException on any error
     */
    synchronized Reply getOneReply(int id) throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM viewReply WHERE id = ?;");) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery();) {
                if (rs.next()) {
                    return new Reply(rs.getInt("id"), rs.getString("subject"), rs.getString("details"),
                            rs.getDate("as_of"), rs.getInt("creatorId"), rs.getString("email"), rs.getString("name"), rs.getInt("likeCount"));
                }
            }
        }
        return null;
    }

    /**
     * Get all data for all replies in the database
     * @return a List with zero or more reply objects
     * @throws SQLException on any error
     */
    synchronized List<Reply> getAllReply() throws SQLException {
        try (var stmt = conn.prepareStatement("SELECT * FROM viewReply;");
                var rs = stmt.executeQuery();) {
            var results = new ArrayList<Reply>();
            while (rs.next()) {
                results.add(new Reply(rs.getInt("id"), rs.getString("subject"), rs.getString("details"),
                        rs.getDate("as_of"), rs.getInt("creatorId"), rs.getString("email"), rs.getString("name"), rs.getInt("likeCount")));
            }
            return results;
        }
    }
    /**
     * Insert a new like record into the database
     * @param creatorId The ID of the user creating the like record
     * @param messageId The ID of the message being liked (null if liking a reply)
     * @param replyId The ID of the reply being liked (null if liking a message)
     * @throws SQLException If the like record cannot be inserted
     * @throws RuntimeException if invalid parameters are provided
     */
    synchronized void insertLikes(int creatorId, Integer messageId, Integer replyId) throws SQLException, RuntimeException {
        if ((messageId == null && replyId == null) || (messageId != null && replyId != null)) {
            throw new RuntimeException("Like must target either message OR reply");
        }
        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblLikes (creatorId, messageId, replyId, liked_at) VALUES (?, ?, ?, ?); ON CONFLICT DO NOTHING");) {
                stmt.setInt(1, creatorId);
                if (messageId == null){
                    stmt.setNull(2, java.sql.Types.INTEGER);
                }
                else{
                    stmt.setInt(2, messageId);
                }
                if (replyId == null){
                    stmt.setNull(3, java.sql.Types.INTEGER);
                }
                else{
                    stmt.setInt(3, replyId);
                }
                stmt.setDate(4, new java.sql.Date(new java.util.Date().getTime()));
                stmt.executeUpdate();
        }
    }
    /**
     * Delete a like record from the database
     * @param creatorId The ID of the user who created the like record
     * @param messageId The ID of the message that was liked (null if a reply was liked)
     * @param replyId The ID of the reply that was liked (null if a message was liked)
     * @throws SQLException If the like record cannot be deleted
     * @throws RuntimeException if invalid parameters are provided
     */
    synchronized void deleteLikes(int creatorId, Integer messageId, Integer replyId) throws SQLException {
        String sql = "DELETE FROM tblLikes WHERE creatorId = ? AND ";
        if (messageId != null) {
            sql += "messageId = ?";
        } 
        else if (replyId != null) {
            sql += "replyId = ?";
        } 
        else {
            throw new IllegalArgumentException("Either messageId or replyId must be provided");
        }
        try (var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, creatorId);
            if (messageId != null) {
                stmt.setInt(2, messageId);
            } 
            else {
                stmt.setInt(2, replyId);
            }
        stmt.executeUpdate();
    }
}
    /** Like is a Java object that matches the contents of tblLikes */
    public static record Like(Date liked_at, int creatorId, Integer messageId, Integer replyId) {}

    /**
     * Get a like record from the database by its ID    
     * @param id The ID of the like to retrieve
     * @return The like record, or null if not found
     * @throws SQLException If the like cannot be retrieved
     */
    synchronized Like getOneLike(int id) throws SQLException {
        try (var stmt = conn.prepareStatement( "SELECT liked_at, creatorId, messageId, replyId FROM tblLikes WHERE id = ?;")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Like(
                        rs.getDate("liked_at"),
                        rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"),
                        (Integer) rs.getObject("replyId")
                );
            }
        }
    }
    return null;
   }
   /**
     * Get all like records for a given message or reply
     * @param messageId The ID of the message to get likes for (null if getting likes for a reply)
     * @param replyId The ID of the reply to get likes for (null if getting likes for a message)
     * @return A list of like records matching the criteria
     * @throws SQLException If the like records cannot be retrieved
     * @throws RuntimeException If both messageId and replyId are provided or both are null
     */
    synchronized List<Like> getAllLike(Integer messageId, Integer replyId) throws SQLException {
        if ((messageId == null && replyId == null) || (messageId != null && replyId != null)) {
            throw new RuntimeException("Like must target either message OR reply");
        }
        var results = new ArrayList<Like>();
        if (replyId != null) {
            try (var stmt = conn.prepareStatement( "SELECT liked_at, creatorId, messageId, replyId FROM tblLikes WHERE replyId = ?;")) {
                stmt.setInt(1, replyId);
                try (var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(new Like(
                            rs.getDate("liked_at"),
                            rs.getInt("creatorId"),
                            (Integer) rs.getObject("messageId"),
                            (Integer) rs.getObject("replyId")
                    ));
                }
            }
        }
    } 
    else if (messageId != null) {
        try (var stmt = conn.prepareStatement(
                "SELECT liked_at, creatorId, messageId, replyId FROM tblLikes WHERE messageId = ?;")) {
            stmt.setInt(1, messageId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Like(
                            rs.getDate("liked_at"),
                            rs.getInt("creatorId"),
                            (Integer) rs.getObject("messageId"),
                            (Integer) rs.getObject("replyId")
                    ));
                }
            }
        }
    }
    return results;
  }
    /**
     * Populates the database with sample Phase 3 test data.
     *
     * @throws SQLException if the data cannot be inserted
     */
    synchronized void populatePhase3TestData() throws SQLException {
        try (var stmt = conn.prepareStatement(
                "UPDATE tblPerson SET gender_identity = ?, sexual_orientation = ?, note = ? WHERE id = ?")) {
            //Test user 1
            stmt.setString(1, "Male");
            stmt.setString(2, "Heterosexual");
            stmt.setString(3, "Test user 1 profile note");
            stmt.setInt(4, 1);
            stmt.executeUpdate();

            //Test user 2
            stmt.setString(1, "Female");
            stmt.setString(2, "Homosexual");
            stmt.setString(3, "Test user 2 profile note");
            stmt.setInt(4, 2);
            stmt.executeUpdate();

            //Test user 3
            stmt.setString(1, "Nonbinary");
            stmt.setString(2, "Gay");
            stmt.setString(3, "Test user 3 profile note");
            stmt.setInt(4, 3);
            stmt.executeUpdate();
        }
        System.out.println("Phase 3 test data populated with 3 test users");
    }
    /**
     * Marks a user as invalid.
     *
     * @param userId The ID of the user
     * @throws SQLException if the operation fails
     */
    synchronized void invalidateUser(int userId) throws SQLException {
        try (var stmt = conn.prepareStatement("UPDATE tblPerson SET valid = FALSE WHERE id = ?")) {
            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("User " + userId + " invalidated successfully");
            } else {
                System.out.println("User " + userId + " not found");
            }
        }
    }
    /**
     * Marks a message as invalid.
     *
     * @param messageId The ID of the message
     * @throws SQLException if the operation fails
     */
    synchronized void invalidateMessage(int messageId) throws SQLException {
        try (var stmt = conn.prepareStatement("UPDATE tblMessage SET valid = FALSE WHERE id = ?")) {
            stmt.setInt(1, messageId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Message " + messageId + " invalidated successfully");
            } else {
                System.out.println("Message " + messageId + " not found");
            }
        }
    }
    /**
     * Marks a reply as invalid.
     *
     * @param replyId The ID of the reply
     * @throws SQLException if the operation fails
     */
    synchronized void invalidateReply(int replyId) throws SQLException {
        try (var stmt = conn.prepareStatement("UPDATE tblReply SET valid = FALSE WHERE id = ?")) {
            stmt.setInt(1, replyId);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Reply " + replyId + " invalidated successfully");
            } else {
                System.out.println("Reply " + replyId + " not found");
            }
        }
    }
   /** Represents a file in the system.
     * Files are never actually deleted from the database, just marked as invalid. This is a common practice in real systems, because it allows you to keep around data that would be difficult to restore if deleted, while still preventing it from being visible in the application.
     * @param id The ID of the file
     * @param creatorId The ID of the user who created the file record
     * @param messageId The ID of the message to which the file belongs (nullable)
     * @param replyId The ID of the reply to which the file belongs (nullable)
     * @param file_name The original name of the file
     * @param file_url The URL where the file is stored
     * @param mime_type The MIME type of the file
     * @param as_of The date the file record was created
     * @param last_accessed_at The date the file was last accessed
     * @param valid Whether the file record is valid (not deleted)
     * @throws SQLException If the file record cannot be retrieved
    */
    public static record File(int id, int creatorId, Integer messageId, Integer replyId, String file_name, String file_url, String mime_type, Date as_of, Date last_accessed_at, boolean valid) {}

    /** Represents a link in the system.
     * Links are never actually deleted from the database, just marked as invalid. This is a common practice in real systems, because it allows you to keep around data that would be difficult to restore if deleted, while still preventing it from being visible in the application.
     * @param id The ID of the link
     * @param creatorId The ID of the user who created the link record
     * @param messageId The ID of the message to which the link belongs (nullable)
     * @param replyId The ID of the reply to which the link belongs (nullable)
     * @param url The URL of the link
     * @param as_of The date the link record was created
     * @param last_accessed_at The date the link was last accessed
     * @param valid Whether the link record is valid (not deleted)
     * @throws SQLException If the link record cannot be retrieved
    */
    public static record Link(int id, int creatorId, Integer messageId, Integer replyId, String url, Date as_of, Date last_accessed_at, boolean valid) {}
} // End of Database Class

//push again


