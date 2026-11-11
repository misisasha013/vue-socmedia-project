package quickstart.backend;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class Database implements AutoCloseable {
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (java.lang.ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private Connection conn;

    private final String dbStr;

    public Database(String dbStr) throws SQLException {
        this.dbStr = dbStr;
        connect();
    }

    private void connect() throws SQLException {
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
        conn = DriverManager.getConnection(jdbcUrl, username, password);
        if (conn == null) {
            throw new RuntimeException("Error: DriverManager.getConnection() returned null");
        }
    }

    private synchronized void ensureConnection() throws SQLException {
        try {
            if (conn == null || !conn.isValid(2)) {
                System.out.println("DB connection lost, reconnecting...");
                connect();
            }
        } catch (Exception e) {
            System.out.println("Reconnect failed: " + e.getMessage());
            connect();
        }
    }

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

   
    public static record PersonShort(int id, String name) {
    }

  
    public synchronized List<PersonShort> getAllPerson() throws SQLException {
        ensureConnection();
        try (var ps = conn.prepareStatement("SELECT id, name FROM tblPerson ORDER BY name;");
                var rs = ps.executeQuery();) {
            var results = new ArrayList<PersonShort>();
            while (rs.next()) {
                results.add(new PersonShort(rs.getInt("id"), rs.getString("name")));
            }
            return results;
        }
    }

    // Added valid field to not allow invlaided users at login
    public static record Person(int id, String email, String name, boolean valid) {
    }

    // now also checks valid so authCallback can block invalidated users
    public synchronized Person getPersonByEmail(String email) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblPerson WHERE email = ?;")) {
            stmt.setString(1, email);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Person(rs.getInt("id"), rs.getString("email"), rs.getString("name"), rs.getBoolean("valid"));
                }
                return null;
            }
        }
    }

    public synchronized Person getOnePerson(int id) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblPerson WHERE id = ?;")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Person(rs.getInt("id"), rs.getString("email"), rs.getString("name"), rs.getBoolean("valid"));
                }
                return null;
            }
        }
    }

    // Full profile record, used for GET /users/:id
    // Gender identity and sexual orienation are only returnes to the profile owner, handled in routes
    public static record UserProfile(int id, String email, String name, String gender_identity, String sexual_orientation, String note){
    }

    // gets the full user profile by user id
    public synchronized UserProfile getUserProfile(int id) throws SQLException{
        ensureConnection();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblPerson WHERE id = ?;")){
            stmt.setInt(1, id);
            try(var rs = stmt.executeQuery()){
                if (rs.next()) {
                    return new UserProfile(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getString("gender_identity"),
                        rs.getString("sexual_orientation"),
                        rs.getString("note")
                    );
                }
                return null;
            }
        }
        
    }
    
    // Changed old records that requesed and update person name to change profile info in general
    public static record UpdateProfileRequest(String name, String gender_identity, String sexual_orientation, String note) {
        void validate() {
            if (name == null || name.length() < 1 || name.length() > 50){
                throw new RuntimeException("Invalid name");
            }
            // gender_identity, sexual_orientation and note are optional so we allow null
            if (note != null && note.length() > 150){
                throw new RuntimeException("Note too long");
            }
        }
    }
    
    public synchronized void updateUserProfile(int id, UpdateProfileRequest req) throws SQLException, RuntimeException {
        ensureConnection();
        req.validate();
        try (var stmt = conn.prepareStatement("UPDATE tblPerson SET name = ?, gender_identity = ?, sexual_orientation = ?, note = ? WHERE id = ?;")) {
            stmt.setString(1, req.name);
            stmt.setString(2, req.gender_identity());
            stmt.setString(3, req.sexual_orientation());
            stmt.setString(4, req.note());
            stmt.setInt(5, id);
            stmt.executeUpdate();
        }
    }

    public static record NewMessageRequest(String subject, String details) {
        void validate() {
            if (subject == null || subject.length() < 1 || subject.length() > 50)
                throw new RuntimeException("Invalid subject");
            if (details == null || details.length() < 1 || details.length() > 500)
                throw new RuntimeException("Invalid details");
        }
    }

    public synchronized long insertMessage(NewMessageRequest req, int creatorId) throws SQLException, RuntimeException {
        ensureConnection();
        req.validate();
        try (
                var stmt = conn.prepareStatement("""
                        INSERT INTO tblMessage
                            (subject, details, as_of, creatorId)
                        VALUES (?, ?, ?, ?);
                        """,
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, req.subject);
            stmt.setString(2, req.details);
            stmt.setDate(3, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(4, creatorId);
            stmt.executeUpdate();
            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public static record MessageShort(int id, String subject, String details, Date as_of, int creatorId, String name, int likeCount, boolean userLiked, int replyCount) {
    }

 public synchronized List<MessageShort> getAllMessage(int currentUserId) throws SQLException {
    ensureConnection();
    var results = new ArrayList<MessageShort>();
    try (var ps = conn.prepareStatement("SELECT * FROM viewMessage WHERE valid = true ORDER BY as_of DESC;");
            var rs = ps.executeQuery()) {
        while (rs.next()) {
            int msgId = rs.getInt("id");
            int likeCount = getLikeCount(msgId);
            boolean userLiked = hasUserLikedMessage(msgId, currentUserId);
            int replyCount = getReplyCount(msgId);
            results.add(new MessageShort(
                rs.getInt("id"),
                rs.getString("subject"),
                rs.getString("details"),
                rs.getDate("as_of"),
                rs.getInt("creatorId"),
                rs.getString("name"),
                likeCount,
                userLiked,
                replyCount
            ));
        }
        return results;
    }
}



    public static record Message(int id, String subject, String details, Date as_of, int creatorId, String email,
            String name) {
    }

    public synchronized Message getOneMessage(int id) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("SELECT * FROM viewMessage WHERE id = ?;")) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Message(rs.getInt("id"), rs.getString("subject"), rs.getString("details"),
                            rs.getDate("as_of"), rs.getInt("creatorId"), rs.getString("email"), rs.getString("name"));
                }
            }
            return null;
        }
    }

    public static record UpdateMessageRequest(String details) {
        
        void validate() {
            if (details == null || details.length() < 1 || details.length() > 500)
                throw new RuntimeException("Invalid details");
        }
    }

   
    public synchronized void updateMessage(int id, UpdateMessageRequest req, int creatorId)
            throws SQLException, RuntimeException {
        ensureConnection();
        req.validate();
        try (var stmt = conn.prepareStatement("""
                UPDATE tblMessage
                SET
                  details = ?,
                  as_of = ?
                WHERE
                  id = ? AND
                  creatorId = ?;
                """);) {
            stmt.setString(1, req.details);
            stmt.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(3, id);
            stmt.setInt(4, creatorId);
            stmt.executeUpdate();
        }
    }

    public synchronized void deleteMessage(int id, int creatorId) throws SQLException {
        ensureConnection();
        //Delete likes on replies of this message
        try (var s = conn.prepareStatement(
                "DELETE FROM tblLikes WHERE replyId IN (SELECT id FROM tblReply WHERE messageId = ?)")) {
            s.setInt(1, id); s.executeUpdate();
        }
        //Delete likes on the message itself 
        try (var s = conn.prepareStatement("DELETE FROM tblLikes WHERE messageId = ?")) {
            s.setInt(1, id); s.executeUpdate();
        }
        //Delete files for replies 
        try (var s = conn.prepareStatement(
                "DELETE FROM tblFile WHERE replyId IN (SELECT id FROM tblReply WHERE messageId = ?)")) {
            s.setInt(1, id); s.executeUpdate();
        }
        //Delete links for replies
        try (var s = conn.prepareStatement(
                "DELETE FROM tblLink WHERE replyId IN (SELECT id FROM tblReply WHERE messageId = ?)")) {
            s.setInt(1, id); s.executeUpdate();
        }
        //Delete the replies
        try (var s = conn.prepareStatement("DELETE FROM tblReply WHERE messageId = ?")) {
            s.setInt(1, id); s.executeUpdate();
        }
        //Delete message files/links
        try (var s = conn.prepareStatement("DELETE FROM tblFile WHERE messageId = ?")) {
            s.setInt(1, id); s.executeUpdate();
        }
        try (var s = conn.prepareStatement("DELETE FROM tblLink WHERE messageId = ?")) {
            s.setInt(1, id); s.executeUpdate();
        }
        //Delete the message itself
        try (var stmt = conn.prepareStatement("DELETE FROM tblMessage WHERE id = ? AND creatorId = ?")) {
            stmt.setInt(1, id);
            stmt.setInt(2, creatorId);
            stmt.executeUpdate();
        }
    }

    public static record NewReplyRequest(String content) {
    public void validate() throws RuntimeException {
        if (content == null || content.length() < 1 || content.length() > 500) {
            throw new RuntimeException("Invalid content");
        }
    }
}
    
    public synchronized long insertReply(NewReplyRequest req, int creatorId, int messageId)
        throws SQLException, RuntimeException {
            ensureConnection();
        req.validate();
        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblReply (subject, details, as_of, creatorId, messageId) VALUES ('', ?, ?, ?, ?) RETURNING id")) {
            stmt.setString(1, req.content());
            stmt.setDate(2, new java.sql.Date(new java.util.Date().getTime()));
            stmt.setInt(3, creatorId);
            stmt.setInt(4, messageId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return -1;
}


    public static record Reply(int id, String details, Date as_of, int creatorId, String email, String name, int messageId, int likeCount, boolean userLiked) {
    }

    public synchronized List<Reply> getRepliesForMessage(int messageId, int currentUserId) throws SQLException {
        ensureConnection();
        var results = new ArrayList<Reply>();
        try (var ps = conn.prepareStatement(
                "SELECT r.id, r.details, r.as_of, r.creatorId, r.messageId, p.email, p.name " +
                "FROM tblReply r INNER JOIN tblPerson p ON r.creatorId = p.id " +
                "WHERE r.messageId = ? ORDER BY r.as_of ASC")) {
            ps.setInt(1, messageId);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    int replyId = rs.getInt("id");
                    int likeCount = getReplyLikeCount(replyId);
                    boolean userLiked = hasUserLikedReply(replyId, currentUserId);
                    results.add(new Reply(
                        replyId,
                        rs.getString("details"),
                        rs.getDate("as_of"),
                        rs.getInt("creatorId"),
                        rs.getString("email"),
                        rs.getString("name"),
                        rs.getInt("messageId"),
                        likeCount,
                        userLiked
                    ));
                }
                return results;
            }
        }
    }

    //like a message
    int likeMessage(int messageId, int userId) throws SQLException {
        ensureConnection();
        try (var ps = conn.prepareStatement(
            "INSERT INTO tblLikes (messageId, creatorId, replyId, liked_at) VALUES (?, ?, NULL, ?) " +
            "ON CONFLICT DO NOTHING")) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            ps.setDate(3, new java.sql.Date(System.currentTimeMillis()));
            ps.executeUpdate();
    }
    return getLikeCount(messageId);
    }
    
    //unlike a message
    int unlikeMessage(int messageId, int userId) throws SQLException {
        ensureConnection();
    try (var ps = conn.prepareStatement(
        "DELETE FROM tblLikes WHERE messageId = ? AND creatorId = ? AND replyId IS NULL")) {
        ps.setInt(1, messageId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }
    return getLikeCount(messageId);
    }
    
    //get like count for message
    private int getLikeCount(int messageId) throws SQLException {
        ensureConnection();
    try (var ps = conn.prepareStatement(
        "SELECT COUNT(*) FROM tblLikes WHERE messageId = ? AND replyId IS NULL")) {
        ps.setInt(1, messageId);
        try (var rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
    }
    return 0;
}
//helper method to get reply count
    private int getReplyCount(int messageId) throws SQLException {
        ensureConnection();
        try (var ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM tblReply WHERE messageId = ?")) {
            ps.setInt(1, messageId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    //like a reply
    int likeReply(int replyId, int userId) throws SQLException {
        ensureConnection();
    try (var ps = conn.prepareStatement(
        "INSERT INTO tblLikes (replyId, creatorId, messageId, liked_at) VALUES (?, ?, NULL, ?) " +
        "ON CONFLICT DO NOTHING")) {
        ps.setInt(1, replyId);
        ps.setInt(2, userId);
        ps.setDate(3, new java.sql.Date(System.currentTimeMillis()));
        ps.executeUpdate();
    }
    return getReplyLikeCount(replyId);
}
    
    //unlike a reply
    int unlikeReply(int replyId, int userId) throws SQLException {
        ensureConnection();
    try (var ps = conn.prepareStatement(
        "DELETE FROM tblLikes WHERE replyId = ? AND creatorId = ? AND messageId IS NULL")) {
        ps.setInt(1, replyId);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }
    return getReplyLikeCount(replyId);
}
    
    //get like count for reply
    private int getReplyLikeCount(int replyId) throws SQLException {
        try (var ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM tblLikes WHERE replyId = ? AND messageId IS NULL")) {
            ps.setInt(1, replyId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    //helper methods for testing errors
    private boolean hasUserLikedReply(int replyId, int userId) throws SQLException {
        if (userId <= 0) return false;
        try (var ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM tblLikes WHERE replyId = ? AND creatorId = ? AND messageId IS NULL")) {
            ps.setInt(1, replyId);
            ps.setInt(2, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean hasUserLikedMessage(int messageId, int userId) throws SQLException {
        if (userId <= 0) return false;
        try (var ps = conn.prepareStatement(
            "SELECT COUNT(*) FROM tblLikes WHERE messageId = ? AND creatorId = ? AND replyId IS NULL")) {
            ps.setInt(1, messageId);
            ps.setInt(2, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    // check if a specific user has liked a reply, same idea as userLikedMessage
    public boolean userLikedReply(int replyId, int userId) {
        try {
            var ps = conn.prepareStatement("SELECT COUNT(*) FROM tblLikes WHERE replyId = ? AND creatorId = ? AND messageId IS NULL");
            ps.setInt(1, replyId);
            ps.setInt(2, userId);
            var rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static record FileRecord(int id, int creatorId, Integer messageId, Integer replyId, String fileName, String fileUrl, String mimeType, Date asOf, Date lastAccessedAt, boolean valid) {}

    public synchronized long insertFile(int creatorId, Integer messageId, Integer replyId,  String fileName, String fileUrl, String mimeType) 
        throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblFile (creatorId, messageId, replyId, file_name, file_url, mime_type, as_of, last_accessed_at, valid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE) RETURNING id")) {
            stmt.setInt(1, creatorId);
            if (messageId != null) stmt.setInt(2, messageId); else stmt.setNull(2, java.sql.Types.INTEGER);
            if (replyId != null) stmt.setInt(3, replyId); else stmt.setNull(3, java.sql.Types.INTEGER);
            stmt.setString(4, fileName);
            stmt.setString(5, fileUrl);
            stmt.setString(6, mimeType);
            stmt.setDate(7, new java.sql.Date(System.currentTimeMillis()));
            stmt.setDate(8, new java.sql.Date(System.currentTimeMillis()));
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        return -1;
    }

    public synchronized FileRecord getFile(int fileId) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblFile WHERE id = ? AND valid = TRUE")) {
            stmt.setInt(1, fileId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new FileRecord(rs.getInt("id"), rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"), (Integer) rs.getObject("replyId"),
                        rs.getString("file_name"), rs.getString("file_url"), rs.getString("mime_type"),
                        rs.getDate("as_of"), rs.getDate("last_accessed_at"), rs.getBoolean("valid"));
                }
            }
        }
        return null;
    }

    public synchronized List<FileRecord> getFilesByMessage(int messageId) throws SQLException {
        ensureConnection();
        var results = new ArrayList<FileRecord>();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblFile WHERE messageId = ? AND valid = TRUE")) {
            stmt.setInt(1, messageId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new FileRecord(rs.getInt("id"), rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"), (Integer) rs.getObject("replyId"),
                        rs.getString("file_name"), rs.getString("file_url"), rs.getString("mime_type"),
                        rs.getDate("as_of"), rs.getDate("last_accessed_at"), rs.getBoolean("valid")));
                }
            }
        }
        return results;
    }

    public synchronized List<FileRecord> getFilesByReply(int replyId) throws SQLException {
        ensureConnection();
        var results = new ArrayList<FileRecord>();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblFile WHERE replyId = ? AND valid = TRUE")) {
            stmt.setInt(1, replyId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new FileRecord(rs.getInt("id"), rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"), (Integer) rs.getObject("replyId"),
                        rs.getString("file_name"), rs.getString("file_url"), rs.getString("mime_type"),
                        rs.getDate("as_of"), rs.getDate("last_accessed_at"), rs.getBoolean("valid")));
                }
            }
        }
        return results;
    }

    public synchronized void deleteFile(int fileId) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("DELETE FROM tblFile WHERE id = ?")) {
            stmt.setInt(1, fileId);
            stmt.executeUpdate();
        }
    }

    public synchronized void updateFileLastAccessed(int fileId) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("UPDATE tblFile SET last_accessed_at = ? WHERE id = ?")) {
            stmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            stmt.setInt(2, fileId);
            stmt.executeUpdate();
        }
    }

    //Link record and CRUD stuff

    public static record Link(int id, int creatorId, Integer messageId, Integer replyId,
                              String url, Date asOf, Date lastAccessedAt, boolean valid) {}

    public synchronized long insertLink(int creatorId, Integer messageId, Integer replyId, String url) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement(
                "INSERT INTO tblLink (creatorId, messageId, replyId, url, as_of, last_accessed_at, valid) " +
                "VALUES (?, ?, ?, ?, ?, ?, TRUE) RETURNING id")) {
            stmt.setInt(1, creatorId);
            if (messageId != null) stmt.setInt(2, messageId); else stmt.setNull(2, java.sql.Types.INTEGER);
            if (replyId != null) stmt.setInt(3, replyId); else stmt.setNull(3, java.sql.Types.INTEGER);
            stmt.setString(4, url);
            stmt.setDate(5, new java.sql.Date(System.currentTimeMillis()));
            stmt.setDate(6, new java.sql.Date(System.currentTimeMillis()));
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("id");
            }
        }
        return -1;
    }

    public synchronized Link getLink(int linkId) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblLink WHERE id = ? AND valid = TRUE")) {
            stmt.setInt(1, linkId);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Link(rs.getInt("id"), rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"), (Integer) rs.getObject("replyId"),
                        rs.getString("url"), rs.getDate("as_of"), rs.getDate("last_accessed_at"),
                        rs.getBoolean("valid"));
                }
            }
        }
        return null;
    }

    public synchronized List<Link> getLinksByMessage(int messageId) throws SQLException {
        ensureConnection();
        var results = new ArrayList<Link>();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblLink WHERE messageId = ? AND valid = TRUE")) {
            stmt.setInt(1, messageId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Link(rs.getInt("id"), rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"), (Integer) rs.getObject("replyId"),
                        rs.getString("url"), rs.getDate("as_of"), rs.getDate("last_accessed_at"),
                        rs.getBoolean("valid")));
                }
            }
        }
        return results;
    }

    public synchronized List<Link> getLinksByReply(int replyId) throws SQLException {
        ensureConnection();
        var results = new ArrayList<Link>();
        try (var stmt = conn.prepareStatement("SELECT * FROM tblLink WHERE replyId = ? AND valid = TRUE")) {
            stmt.setInt(1, replyId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Link(rs.getInt("id"), rs.getInt("creatorId"),
                        (Integer) rs.getObject("messageId"), (Integer) rs.getObject("replyId"),
                        rs.getString("url"), rs.getDate("as_of"), rs.getDate("last_accessed_at"),
                        rs.getBoolean("valid")));
                }
            }
        }
        return results;
    }

    public synchronized void deleteLink(int linkId) throws SQLException {
        ensureConnection();
        try (var stmt = conn.prepareStatement("DELETE FROM tblLink WHERE id = ?")) {
            stmt.setInt(1, linkId);
            stmt.executeUpdate();
        }
    }

    public synchronized void runMigrations() {
        try (var s = conn.prepareStatement("ALTER TABLE tblFile RENAME COLUMN is_valid TO valid")) {
            s.execute();
            System.out.println("Renamed tblFile.is_valid -> valid");
        } catch (Exception e) {
            System.out.println("tblFile rename skipped: " + e.getMessage());
        }
        try (var s = conn.prepareStatement("ALTER TABLE tblLink RENAME COLUMN is_valid TO valid")) {
            s.execute();
            System.out.println("Renamed tblLink.is_valid -> valid");
        } catch (Exception e) {
            System.out.println("tblLink rename skipped: " + e.getMessage());
        }
    }

}

