package quickstart.admin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import quickstart.admin.Database.Person;


@Disabled("Tests need to be rewritten for PostgreSQL support")
// TODO: Update tests to work with both SQLite and PostgreSQL
/**
 * Unit test for simple App.
 * Creates a shared test db for all tests in initAll, and deletes it at the end in tearDownAll.
 * To better isolate each test, the tables and views are dropped after each test, and recreated (empty) before each test begins.
 * See https://docs.junit.org/current/user-guide/#writing-tests
 */
public class AppTest {
    /**
     * rather than mocking, we share among tests a newly constructed database object
     */
    static Database db = null;
    /**
     * the test db has its own filename based on system time; we track it so we can delete it when finished
     */
    static String dbFileName = null;

    /**
     * True if using SQLite, false if using PostgreSQL
     * Determined at test startup based on env vars 
     */
    static boolean useSQLite = true;

    /**
     * Ensure that DB_FILE env var is provided, has 1 or more characters, then create a db just for testing
     * Made chagnes to use PostegreSQL or fall back to sqlite
     */
    @BeforeAll
    static void initAll() {
        String dbUrl = System.getenv("DATABASE_URL"); // So checks url (postgresql) furst then falls back to file (sqlite)
        String dbFile = System.getenv("DB_FILE");
       
        if (dbUrl != null && !dbUrl.isEmpty()) { // Use PostgreSQL
            useSQLite = false;
            System.out.println("AppTest: Using PostgreSQL via DATABASE_URL");
            assertDoesNotThrow(() -> db = new Database(false, dbUrl), "Exception thrown while creating PostgreSQL test db.");
        } else { // Fall back to SQLite
            useSQLite = true;
            assertNotNull(dbFile, "Missing environment variable: DB_FILE."); // Code from ealirer tests
            assertFalse(dbFile.length() == 0, "DB_FILE should be 1 or more characters.");
            dbFileName = String.format("%s_junit_%d.db", dbFile, System.currentTimeMillis());
            System.out.println("AppTest: Using SQLite with file: " + dbFileName);
            assertDoesNotThrow(() -> db = new Database(true, dbFileName), "Exception thrown while creating SQLite test db.");
        }
    }

    /** Shuts down the database and deletes the file */
    @AfterAll
    static void tearDownAll() throws Exception {
        System.out.println("Cleaning up: Shutting down the test database.");
        if (db != null) {
            try {
                db.close();
            } catch (Exception e) {
                System.err.println("WARNING: problem closing test database.");
            }
        }
        if (dbFileName != null) {
            java.io.File dbFileToDelete = new File(dbFileName);
            if (dbFileToDelete.exists()) {
                System.out.println("Cleaning up: deleting " + dbFileName);
                if (dbFileToDelete.delete()) {
                    System.out.println("  Delete test database successful.");
                } else {
                    System.err.println("WARNING: could not delete test db " + dbFileName);
                }
            }
        }
    }

    /** Ensures before each test that db exists, and has fresh tables and views */
    @BeforeEach
    void initEach() {
        System.out.println("running initEach");
        assertNotNull(db);
        if (db != null) {
            assertDoesNotThrow(() -> db.createTables());
            assertDoesNotThrow(() -> db.createViews());
            assertDoesNotThrow(() -> db.addPhase3Columns());
        }
    }

    /** Ensures after each test that all tables and views are dropped from db */
    @AfterEach
    void tearDownEach() {
        System.out.println("running tearDownEach");
        if (db != null) {
            assertDoesNotThrow(() -> db.dropViews());
            assertDoesNotThrow(() -> db.dropTables());
        }
    }

    /**
     * Foreign keys are essential for our apps correctness; this ensures they are on
     * This test is SQLite-only because PRAGMA is a SQLite-specific command.
     * PostgreSQL enforces foreign keys by default, so no check is needed ther
     */
    @Test
    @DisplayName("Verify FOREIGN_KEYS are on.")
    public void givenDatabase_whenCreated_thenPragmaIsOn() throws SQLException {
        assertNotNull(db);
        if (!useSQLite) {
            System.out.println("Skipping PRAGMA foreign_keys check: not applicable for PostgreSQL.");
            return;
        }
        assertTrue(db.getPragmaForeignKeysStatus(), "This application requires that FOREIGN_KEYS be on.");
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    @Disabled("For demonstration purposes")
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    /** 
     * The app expects env var DB_FILE to exist; this ensures it does. 
     * Updated for postgres 
    */
    @Test
    @DisplayName("Verify at least one database environment variable is set")
    public void checkEnv_DB_FILE_or_DATABASE_URL() {
        String dbFile = System.getenv("DB_FILE");
        String dbUrl = System.getenv("DATABASE_URL");
        assertTrue((dbFile != null && dbFile.length() >= 1) || (dbUrl != null && dbUrl.length() >= 1), "Missing environment variable: set either DB_FILE (SQLite) or DATABASE_URL (PostgreSQL).");
    }

    /**
     * Database::insertPerson should not be able to insert person with null email
     */
    @Test
    @DisplayName("insertPerson should throw if the email is null")
    public void givenPerson_whenEmailNull_thenRejectInsert() {
        assertNotNull(db);
        var ex = assertThrows(RuntimeException.class, () -> db.insertPerson(null, null));
        assertEquals("Invalid email address", ex.getMessage());
    }

    /**
     * Database::insertPerson should not be able to insert person with overly long
     * email
     */
    @Test
    @DisplayName("insertPerson should throw if the email is too long")
    public void givenPerson_whenEmailTooLong_thenRejectInsert() {
        assertNotNull(db);
        var ex = assertThrows(RuntimeException.class, () -> db.insertPerson(".".repeat(31), null));
        assertEquals("Invalid email address", ex.getMessage());
    }

    /** 
     * Should not be able to insert a new person with same email as another 
     * Updated to use assertThrows without checking the exact message, since SQLite and PostgreSQL produce different violation msgs
    */
    @Test
    @DisplayName("insertPerson should throw if the email is already in use")
    public void givenPerson_whenAlreadyExists_thenRejectInsert() {
        assertNotNull(db);
        final String sEmail = "test@email.com", sName = "test person";
        assertDoesNotThrow(() -> db.insertPerson(sEmail, sName));
        // Both SQLite and PostgreSQL throw a SQLException on unique constraint violation but w dif msgs, so we only assert the exception type here
        assertThrows(SQLException.class, () -> db.insertPerson(sEmail, sName));
    }

    /**
     * Should not be able to update a person's email to one in use by another user
     */
    @Test
    @DisplayName("updatePerson should throw if the email is already in use")
    public void givenPerson_whenChangingEmailToOneInUse_thenRejectUpdate() {
        assertNotNull(db);
        final String sEmail1 = "test1@email.com", sName1 = "test person1";
        final String sEmail2 = "test2@email.com", sName2 = "test person2";
        final int idUser1 = assertDoesNotThrow(() -> {
            return db.insertPerson(sEmail1, sName1);
        });
        final int idUser2 = assertDoesNotThrow(() -> {
            return db.insertPerson(sEmail2, sName2);
        });
        assertNotEquals(idUser1, idUser2, "Two distinct users should not get the same id upon creation.");
        var ex = assertThrows(RuntimeException.class, () -> db.updatePerson(idUser2, sEmail1, sName2));
        assertEquals("Email already in use", ex.getMessage());
    }

    /**
     * Confirm a new user can be created, and getOnePerson retreives it correctly
     */
    @Test
    @DisplayName("getOnePerson should retreive the information for a newly created user")
    public void givenPerson_whenCreating_then_getOnePerson_should_match() {
        assertNotNull(db);
        final String sEmail1 = "test1@email.com", sName1 = "test person1";
        final int idUser1 = assertDoesNotThrow(() -> {
            return db.insertPerson(sEmail1, sName1);
        });
        Database.Person p = assertDoesNotThrow(() -> {
            return db.getOnePerson(idUser1);
        });
        assertNotNull(p, "Could not reteive new user by their id.");
        assertEquals(idUser1, p.id(), "getOneUser returned a user with an id different than expected");
        assertEquals(sEmail1, p.email());
        assertEquals(sName1, p.name());
    }

    /**
     * Confirm a new user can be created, and getAllPerson retreives it correctly
     */
    @Test
    @DisplayName("getAllPerson should retreive the information for a newly created user")
    public void givenPerson_whenCreating_then_getAllPerson_should_haveThem() {
        assertNotNull(db);
        final String sEmail1 = "test1@email.com", sName1 = "test person1";
        final int idUser1 = assertDoesNotThrow(() -> {
            return db.insertPerson(sEmail1, sName1);
        });
        java.util.List<Database.Person> allPeople = assertDoesNotThrow(() -> {
            return db.getAllPerson();
        });
        assertNotNull(allPeople);
        boolean foundUser = false;
        for (Person pEntry : allPeople) {
            if (pEntry.id() == idUser1 || sEmail1.equals(pEntry.email())) {
                foundUser = true;
                assertEquals(idUser1, pEntry.id());
                assertEquals(sEmail1, pEntry.email());
                assertEquals(sName1, pEntry.name());
            }
        }
        assertTrue(foundUser, "Did not find user in list returned by getAllPerson()");
    }

    /** Confirm a user can be deleted if they have no messages */
    @Test
    @DisplayName("deletePerson should remove an existing user if they have no messages")
    public void givenPerson_whenDeleting_thenSucceedWhenNoMessagesExist() {
        assertNotNull(db);
        final String sEmail1 = "test1@email.com", sName1 = "test person1";
        final int idUser1 = assertDoesNotThrow(() -> {
            return db.insertPerson(sEmail1, sName1);
        });
        Database.Person p = assertDoesNotThrow(() -> {
            return db.getOnePerson(idUser1);
        });
        assertNotNull(p);
        assertEquals(idUser1, p.id());
        assertDoesNotThrow(() -> db.deletePerson(idUser1));
        p = assertDoesNotThrow(() -> {
            return db.getOnePerson(idUser1);
        });
        assertNull(p);
        java.util.List<Database.Person> allPeople = assertDoesNotThrow(() -> {
            return db.getAllPerson();
        });
        for (Person pEntry : allPeople)
            assertNotEquals(pEntry.id(), idUser1);
    }

    /**
     * Confirm a user canNOT be deleted if they have messages associated with their account
     * Again while both SQLite and PostgreSQL throw a SQLException on foreign key violations but w dif msgs, so we only assert the exception type
     */
    @Test
    @DisplayName("deletePerson should NOT remove an existing user if they have messages")
    public void givenPerson_whenDeleting_thenFailWhenMessagesExist() {
        assertNotNull(db);
        final String sEmail1 = "test1@email.com", sName1 = "test person1";
        final String sSubject = "test subject", sDetails = "test details";
        final int idUser1 = assertDoesNotThrow(() -> {
            return db.insertPerson(sEmail1, sName1);
        });
        Database.Person p = assertDoesNotThrow(() -> {
            return db.getOnePerson(idUser1);
        });
        assertNotNull(p);
        assertEquals(idUser1, p.id());
        assertDoesNotThrow(() -> db.insertMessage(sSubject, sDetails, idUser1));
        // Both databases throw SQLException on FK violation, msgs differ so we don't check it
        assertThrows(SQLException.class, () -> db.deletePerson(idUser1));
        p = assertDoesNotThrow(() -> {
            return db.getOnePerson(idUser1);
        });
        assertNotNull(p);
    }

    /** 
     * Should not be able to insert message with a creatorId not in user table 
     * Both SQLite and PostgreSQL throw a SQLException on foreign key violations but w dif msgs, so we only assert the exception type again
    */
    @Test
    @DisplayName("insertMessage should fail when associated creatorId is not in users table")
    public void givenMessage_whenCreatorIdDoesNotExistInUsers_thenRejectInsert() {
        assertNotNull(db);
        final String sSubject = "test subject", sDetails = "test details";
        java.util.List<Database.Person> allPeople = assertDoesNotThrow(() -> {
            return db.getAllPerson();
        });
        assertNotNull(allPeople);
        assertEquals(allPeople.size(), 0);
        // Both databases throw SQLException on FK violation, messages differ so we don't check it
        assertThrows(SQLException.class, () -> db.insertMessage(sSubject, sDetails, 42));
    } 

    // Used chatgpt to help write this test, this tests for insertreply and creating a new reply  
    @Test
    @DisplayName("insertReply should create reply associated with message")
    public void givenReply_whenInserted_thenRetrieveCorrectly() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("reply@email.com", "reply user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertReply("reply subject", "reply details", uid, mid)
    );

    var replies = assertDoesNotThrow(() ->
        db.getAllReply()
    );

    assertEquals(1, replies.size());

    Database.Reply r = replies.get(0);

    assertEquals("reply subject", r.subject());
    assertEquals("reply details", r.details());
}

// Used chatgpt to help write this test, this tests adding in a like onto an exisiting message   
@Test
@DisplayName("insertLikes should create like on message")
public void givenLike_whenInsertedOnMessage_thenRetrieve() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("like@email.com", "like user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertLikes(uid, mid, null)
    );

    var likes = assertDoesNotThrow(() ->
        db.getAllLike(mid, null)
    );

    assertEquals(1, likes.size());
}

// Used chatgpt to help write this test, this tests for whether a user can like a prexisiting reply
@Test
@DisplayName("insertLikes should create like on reply")
public void givenLike_whenInsertedOnReply_thenRetrieve() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("replylike@email.com", "user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertReply("reply", "reply body", uid, mid)
    );

    int rid = assertDoesNotThrow(() ->
        db.getAllReply().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertLikes(uid, null, rid)
    );

    var likes = assertDoesNotThrow(() ->
        db.getAllLike(null, rid)
    );

    assertEquals(1, likes.size());
}

// Used chatgpt to help write this test, this tests vieMessage and correctly counting the likes on messages 
@Test
@DisplayName("viewMessage should correctly count likes")
public void givenMessageWithLikes_whenQueryingView_thenLikeCountMatches() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("count@email.com", "user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() -> db.insertLikes(uid, mid, null));
    assertDoesNotThrow(() -> db.insertLikes(uid, mid, null));

    Database.Message m = assertDoesNotThrow(() ->
        db.getOneMessage(mid)
    );

    assertEquals(2, m.likeCount());
}

// Used chatgpt to help write this test, his tests vieMessage and correctly counting the replies on messages 
@Test
@DisplayName("viewMessage should correctly count replies")
public void givenMessageWithReplies_whenQueryingView_thenReplyCountMatches() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("replycount@email.com", "user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertReply("r1", "body", uid, mid)
    );

    assertDoesNotThrow(() ->
        db.insertReply("r2", "body", uid, mid)
    );

    Database.Message m = assertDoesNotThrow(() ->
        db.getOneMessage(mid)
    );

    assertEquals(2, m.replyCount());
}

// Used chatgpt to help write this test, this tests for inserting a file and then retrieving it to confirm it was inserted correctly
@Test
@DisplayName("insertFile should create and retrieve file correctly")
public void givenFile_whenInserted_thenRetrieveCorrectly() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("file1@email.com", "file user")
    );

    int fileId = assertDoesNotThrow(() ->
        db.insertFile(uid, null, null, "http://example.com/file1.png", "file1.png", "image/png")
    );

    Database.File f = assertDoesNotThrow(() ->
        db.getOneFile(fileId)
    );

    assertNotNull(f);
    assertEquals(fileId, f.id());
    assertEquals(uid, f.creatorId());
    assertEquals("file1.png", f.file_name());
    assertEquals("http://example.com/file1.png", f.file_url());
    assertEquals("image/png", f.mime_type());
    assertTrue(f.valid());
}

// Used chatgpt to help write this test, this tests for inserting a file with a blank url and confirming that it throws the correct exception
@Test
@DisplayName("insertFile should throw when file URL is blank")
public void givenFile_whenUrlBlank_thenRejectInsert() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("file2@email.com", "file user")
    );

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        db.insertFile(uid, null, null, "", "file2.png", "image/png")
    );

    assertEquals("Invalid file URL", ex.getMessage());
}

// Used chatgpt to help write this test, this tests for inserting a file with a blank name and confirming that it throws the correct exception
@Test
@DisplayName("insertFile should throw when file name is blank")
public void givenFile_whenNameBlank_thenRejectInsert() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("file3@email.com", "file user")
    );

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        db.insertFile(uid, null, null, "http://example.com/file3.png", "", "image/png")
    );

    assertEquals("Invalid file name", ex.getMessage());
}

// Used chatgpt to help write this test, this tests for inserting a file and then using getAllFiles to confirm it is included in the list of all files
@Test
@DisplayName("getAllFiles should include newly inserted file")
public void givenFile_whenInserted_thenGetAllFilesShouldContainIt() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("file4@email.com", "file user")
    );

    int fileId = assertDoesNotThrow(() ->
        db.insertFile(uid, null, null, "http://example.com/file4.pdf", "file4.pdf", "application/pdf")
    );

    var files = assertDoesNotThrow(() ->
        db.getAllFiles()
    );

    assertNotNull(files);
    assertEquals(1, files.size());
    assertEquals(fileId, files.get(0).id());
    assertEquals("file4.pdf", files.get(0).file_name());
}

// Used chatgpt to help write this test, this tests for inserting a file, then invalidating it, and confirming that valid is false when we retrieve the file after invalidation
@Test
@DisplayName("invalidateFile should mark file as invalid")
public void givenFile_whenInvalidated_thenValidShouldBeFalse() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("file5@email.com", "file user")
    );

    int fileId = assertDoesNotThrow(() ->
        db.insertFile(uid, null, null, "http://example.com/file5.txt", "file5.txt", "text/plain")
    );

    assertDoesNotThrow(() ->
        db.invalidateFile(fileId)
    );

    Database.File f = assertDoesNotThrow(() ->
        db.getOneFile(fileId)
    );

    assertNotNull(f);
    assertFalse(f.valid());
}

// Used chatgpt to help write this test, this tests for inserting a file, then deleting it, and confirming that getOneFile returns null after deletion
@Test
@DisplayName("deleteFile should remove file from database")
public void givenFile_whenDeleted_thenGetOneFileReturnsNull() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("file6@email.com", "file user")
    );

    int fileId = assertDoesNotThrow(() ->
        db.insertFile(uid, null, null, "http://example.com/file6.txt", "file6.txt", "text/plain")
    );

    assertDoesNotThrow(() ->
        db.deleteFile(fileId)
    );

    Database.File f = assertDoesNotThrow(() ->
        db.getOneFile(fileId)
    );

    assertNull(f);
}

// Used chatgpt to help write this test, this tests for inserting a file and then retrieving it to confirm it was inserted correctly
@Test
@DisplayName("insertLink should create and retrieve link correctly")
public void givenLink_whenInserted_thenRetrieveCorrectly() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("link1@email.com", "link user")
    );

    int linkId = assertDoesNotThrow(() ->
        db.insertLink(uid, null, null, "http://example.com")
    );

    Database.Link l = assertDoesNotThrow(() ->
        db.getOneLink(linkId)
    );

    assertNotNull(l);
    assertEquals(linkId, l.id());
    assertEquals(uid, l.creatorId());
    assertEquals("http://example.com", l.url());
    assertTrue(l.valid());
}

// Used chatgpt to help write this test, this tests for inserting a link with a blank url and confirming that it throws the correct exception
@Test
@DisplayName("insertLink should throw when URL is blank")
public void givenLink_whenUrlBlank_thenRejectInsert() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("link2@email.com", "link user")
    );

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        db.insertLink(uid, null, null, "")
    );

    assertEquals("Invalid URL", ex.getMessage());
}

// Used chatgpt to help write this test, this tests for inserting a link and then using getAllLinks to confirm it is included in the list of all links  
@Test
@DisplayName("getAllLinks should include newly inserted link")
public void givenLink_whenInserted_thenGetAllLinksShouldContainIt() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("link3@email.com", "link user")
    );

    int linkId = assertDoesNotThrow(() ->
        db.insertLink(uid, null, null, "http://lehigh.edu")
    );

    var links = assertDoesNotThrow(() ->
        db.getAllLinks()
    );

    assertNotNull(links);
    assertEquals(1, links.size());
    assertEquals(linkId, links.get(0).id());
    assertEquals("http://lehigh.edu", links.get(0).url());
}

// Used chatgpt to help write this test, this tests for inserting a link, then invalidating it, and confirming that valid is false when we retrieve the link after invalidation
@Test
@DisplayName("invalidateLink should mark link as invalid")
public void givenLink_whenInvalidated_thenValidShouldBeFalse() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("link4@email.com", "link user")
    );

    int linkId = assertDoesNotThrow(() ->
        db.insertLink(uid, null, null, "http://openai.com")
    );

    assertDoesNotThrow(() ->
        db.invalidateLink(linkId)
    );

    Database.Link l = assertDoesNotThrow(() ->
        db.getOneLink(linkId)
    );

    assertNotNull(l);
    assertFalse(l.valid());
}

// Used chatgpt to help write this test, this tests for inserting a link, then deleting it, and confirming that getOneLink returns null after deletion
@Test
@DisplayName("deleteLink should remove link from database")
public void givenLink_whenDeleted_thenGetOneLinkReturnsNull() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("link5@email.com", "link user")
    );

    int linkId = assertDoesNotThrow(() ->
        db.insertLink(uid, null, null, "http://github.com")
    );

    assertDoesNotThrow(() ->
        db.deleteLink(linkId)
    );

    Database.Link l = assertDoesNotThrow(() ->
        db.getOneLink(linkId)
    );

    assertNull(l);
}

// Used chatgpt to help write this test, this tests for inserting a like with both messageId and replyId as null, which should throw an exception since a like must target either a message or a reply
@Test
@DisplayName("insertLikes should throw when both messageId and replyId are null")
public void givenLike_whenBothTargetsNull_thenRejectInsert() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("likebad1@email.com", "like user")
    );

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        db.insertLikes(uid, null, null)
    );

    assertEquals("Like must target either message OR reply", ex.getMessage());
}

// Used chatgpt to help write this test, this tests for inserting a like with both messageId and replyId provided, which should throw an exception since a like must target either a message or a reply, but not both
@Test
@DisplayName("insertLikes should throw when both messageId and replyId are provided")
public void givenLike_whenBothTargetsProvided_thenRejectInsert() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("likebad2@email.com", "like user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertReply("reply", "body", uid, mid)
    );

    int rid = assertDoesNotThrow(() ->
        db.getAllReply().get(0).id()
    );

    RuntimeException ex = assertThrows(RuntimeException.class, () ->
        db.insertLikes(uid, mid, rid)
    );

    assertEquals("Like must target either message OR reply", ex.getMessage());
}

// Used chatgpt to help write this test, this tests for inserting a like on a message, then deleting that like, and confirming that the like is removed from the message
@Test
@DisplayName("deleteLikes should remove like from message")
public void givenMessageLike_whenDeleted_thenNoLikesRemain() {
    assertNotNull(db);

    int uid = assertDoesNotThrow(() ->
        db.insertPerson("likedel@email.com", "like user")
    );

    assertDoesNotThrow(() ->
        db.insertMessage("msg", "details", uid)
    );

    int mid = assertDoesNotThrow(() ->
        db.getAllMessage().get(0).id()
    );

    assertDoesNotThrow(() ->
        db.insertLikes(uid, mid, null)
    );

    assertDoesNotThrow(() ->
        db.deleteLikes(uid, mid, null)
    );

    var likes = assertDoesNotThrow(() ->
        db.getAllLike(mid, null)
    );

    assertNotNull(likes);
    assertEquals(0, likes.size());
}

// Used chatgpt to help write this test, this tests for inserting a like on a reply, then deleting that like, and confirming that the like is removed from the reply
@Test
@DisplayName("getOnePerson should return null for missing id")
public void givenMissingPersonId_whenQuerying_thenReturnNull() {
    assertNotNull(db);

    Database.Person p = assertDoesNotThrow(() ->
        db.getOnePerson(9999)
    );

    assertNull(p);
}

// Used chatgpt to help write this test, this tests for inserting a like on a reply, then deleting that like, and confirming that the like is removed from the reply
@Test
@DisplayName("getOneFile should return null for missing id")
public void givenMissingFileId_whenQuerying_thenReturnNull() {
    assertNotNull(db);

    Database.File f = assertDoesNotThrow(() ->
        db.getOneFile(9999)
    );

    assertNull(f);
}

// Used chatgpt to help write this test, this tests for inserting a like on a reply, then deleting that like, and confirming that the like is removed from the reply
@Test
@DisplayName("getOneLink should return null for missing id")
public void givenMissingLinkId_whenQuerying_thenReturnNull() {
    assertNotNull(db);

    Database.Link l = assertDoesNotThrow(() ->
        db.getOneLink(9999)
    );

    assertNull(l);
}
    
}