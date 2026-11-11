package quickstart.admin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * App lets us manage the database schema and the data in it.
 */
public class App {
    /**
     * Entry point for the Admin CLI application.
     *
     * @param argv Command line arguments
     */
    public static void main(String[] argv) {
        /** 
         * Get the SQLite configuration from environment variables!!! 
         * We will need to run the program with a shell command like DB_FILE=db.db java -jar target/admin-1.0.jar
         * Is a best practice for the web backend, so this code might as well do the same
        */
        String dbFile = System.getenv("DB_FILE");
        String dbUrl = System.getenv("DATABASE_URL"); // For reading the env var
        System.out.println("Using the following environment variables:");
        System.out.println("-".repeat(45));
        System.out.println("  DB_FILE=" + dbFile);
        System.out.println("-".repeat(45));
        System.out.println("  DATABASE_URL=" + dbUrl); // For reporting the new env var

        // Validates the env vars, upadated for new dbUrl
        if (dbFile == null && dbUrl == null) {
            // insufficient information to connect
            System.err.println("Insufficient information to connect. Bye.");
            return;
        }

        // Get a fully-configured connection to the database, or exit immediately
        // Use try-with-resources so the Database (AutoCloseable) is always closed properly even if an exception occurs (This prevents resource leaks and database locks)
        try (Database db = new Database(dbUrl == null, (dbUrl == null ? dbFile : dbUrl))) { // Changed for the database consturcter
            // Start reading requests and processing them
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                switch (prompt(in)) {
                    case "?": // help
                        menu();
                        break;
                    case "q": // quit
                        return;
                    case "C": // create tables and views
                        db.createTables();
                        break;
                    case "D": // drop tables and views
                        db.dropViews();
                        db.dropTables();
                        break;
                    case "P": // add Phase 3 profile columns
                        db.addPhase3Columns();
                        db.createViews();
                        break;
                    case "1p": // query for one person row
                        var person = db.getOnePerson(getInt(in, "Enter the person ID"));
                        if (person != null) {
                            System.out.println(" " + person.id() + " | " + person.email() + " | " + person.name());
                        }
                        break;
                    case "T": // populate Phase 3 test data
                        db.populatePhase3TestData();
                        break;
                    case "I": // invalidate a user
                        db.invalidateUser(getInt(in, "Enter the user ID"));
                        break;
                    case "M": // invalidate a message
                        db.invalidateMessage(getInt(in, "Enter the message ID"));
                        break;
                    case "R": // invalidate a reply
                        db.invalidateReply(getInt(in, "Enter the reply ID"));
                        break;
                    case "*p": // query for all person rows
                        System.out.println("  tblPerson");
                        System.out.println("  -------------------------");
                        for (var row : db.getAllPerson()) {
                            System.out.println(" " + row.id() + " | " + row.email() + " | " + row.name());
                        }
                        break;
                    case "-p": // delete a person
                        db.deletePerson(getInt(in, "Enter the person ID"));
                        break;
                    case "+p": // insert a person
                        int newPid = db.insertPerson(
                                getString(in, "Enter the email"),
                                getString(in, "Enter the name"));
                        System.out.println("id of newly inserted person: " + newPid);
                        break;
                    case "~p": // update a person
                        db.updatePerson(
                                getInt(in, "Enter the person ID"),
                                getString(in, "Enter the new email"),
                                getString(in, "Enter the new name"));
                        break;
                    case "1m": // query for one message row
                        var msg = db.getOneMessage(getInt(in, "Enter the message ID"));
                        if (msg != null) {
                            System.out.println(" " + msg.id() + " | " + msg.subject() + " | " + msg.details() + " | "
                            + new java.util.Date(msg.as_of().getTime()) + " | " + msg.creatorId() + " | " + msg.email() + " | " + msg.name() + " | "+ msg.likeCount());
                        }
                        break;
                    case "*m": // query for all message rows
                        System.out.println("  tblMessage");
                        System.out.println("  -------------------------");
                        for (var row : db.getAllMessage()) {
                            System.out.println(" " + row.id() + " | " + row.subject() + " | " + row.details() + " | "
                                    + new java.util.Date(row.as_of().getTime()) + " | " + row.creatorId() + " | "
                                    + row.email() + " | " + row.name() + " | "+ row.likeCount());
                        }
                        break;
                    case "-m": // delete a message
                        db.deleteMessage(getInt(in, "Enter the message ID"));
                        break;
                    case "+m": // insert a message
                        db.insertMessage(getString(in, "Enter the subject"),
                                getString(in, "Enter the message"),
                                getInt(in, "Enter the person ID"));
                        break;
                    case "~m": // update a message
                        db.updateMessage(getInt(in, "Enter the message ID"),
                                getString(in, "Enter the subject"),
                                getString(in, "Enter the message"));
                        break;
                    case "1r": //get one reply
                        var rpl = db.getOneReply(getInt(in, "Enter the Reply ID"));
                        if (rpl != null) {
                            System.out.println(" " + rpl.id() + " | " + rpl.subject() + " | " + rpl.details() + " | "
                                    + new java.util.Date(rpl.as_of().getTime()) + " | " + rpl.creatorId() + " | "
                                    + rpl.email() + " | " + rpl.name() + " | "+ rpl.likeCount());
                        }
                        break;
                    case "*r": //get all replies 
                        System.out.println("  tblReply");
                        System.out.println("  -------------------------");
                        for (var row : db.getAllReply()) {
                            System.out.println(" " + row.id() + " | " + row.subject() + " | " + row.details() + " | "
                                    + new java.util.Date(row.as_of().getTime()) + " | " + row.creatorId() + " | "
                                    + row.email() + " | " + row.name() + " | "+ row.likeCount());
                        }
                        break;
                    case "-r": // delete a reply
                        db.deleteReply(getInt(in, "Enter the message ID"));
                        break;
                    case "+r": // insert a reply
                        db.insertReply(getString(in, "Enter the subject"),
                                getString(in, "Enter the reply"),
                                getInt(in, "Enter the person ID"),
                                getInt(in, "Enter the message ID")); 
                        break;
                    case "~r": // update a reply
                        db.updateReply(getInt(in, "Enter the reply ID"),
                                getString(in, "Enter the subject"),
                                getString(in, "Enter the reply"));
                        break;
                    case "1L": //get one like
                        var like = db.getOneLike(getInt(in, "Enter the Like ID"));
                        if (like != null) {
                            System.out.println(" " + new java.util.Date(like.liked_at().getTime()) + " | " + like.creatorId() + " | " 
                            + like.messageId() + " | "+ like.replyId()); 
                        } 
                        break;
                    case "*L":  //get all likes
                        System.out.println("Query likes for:");
                        System.out.println("1 = Message");
                        System.out.println("2 = Reply");
                        int type = getInt(in, "Enter choice");
                        List<Database.Like> likes;
                        if (type == 1) {
                            int messageId = getInt(in, "Enter message ID");
                            likes = db.getAllLike(messageId, null);
                        } 
                        else {
                            int replyId = getInt(in, "Enter reply ID");
                            likes = db.getAllLike(null, replyId);
                        }
                        System.out.println(" tblLikes");
                        System.out.println(" -------------------------");
                        for (var row : likes) {
                            System.out.println(" " + new java.util.Date(row.liked_at().getTime()) + " | " + row.creatorId() + " | "
                            + row.messageId() + " | " + row.replyId());
                        }
                        break;
                    case "-L": // delete like
                        System.out.println("Are you deleting a like from message(1) or reply(2)");
                        int cId = getInt(in, "Enter creator ID");
                        System.out.println("Like target:");
                        System.out.println("1 = Message");
                        System.out.println("2 = Reply");
                        int c = getInt(in, "Enter choice");
                        if(c == 1){
                            int mId = getInt(in, "Enter message ID");
                            db.deleteLikes(cId, mId, null);
                        }
                        else{
                            int rId = getInt(in, "Enter reply ID");
                            db.deleteLikes(cId, null, rId);
                        }
                        break;
                    case "+L": // insert like 
                        int creatorId = getInt(in, "Enter creator ID");
                        System.out.println("Like target:");
                        System.out.println("1 = Message");
                        System.out.println("2 = Reply");
                        int choice = getInt(in, "Enter choice");
                        if (choice == 1) {
                            int messageId = getInt(in, "Enter message ID");
                            db.insertLikes(creatorId, messageId, null);
                        } 
                        else {
                            int replyId = getInt(in, "Enter reply ID");
                             db.insertLikes(creatorId, null, replyId);
                            }
                        break;
                    case "1f": // query for one file
                        var file = db.getOneFile(getInt(in, "Enter the file ID"));
                        if (file != null) {
                            System.out.println(" " + file.id() + " | " + file.file_name() + " | "
                                    + file.file_url() + " | " + file.creatorId() + " | "
                                    + file.messageId() + " | " + file.replyId() + " | "
                                    + new java.util.Date(file.as_of().getTime()) + " | "
                                    + (file.last_accessed_at() == null ? "null" : new java.util.Date(file.last_accessed_at().getTime()))
                                    + " | " + file.valid());
                        }
                        break;
                    case "*f": // query for all files
                        System.out.println("  tblFile");
                        System.out.println("  -------------------------");
                        for (var row : db.getAllFiles()) {
                            System.out.println(" " + row.id() + " | " + row.file_name() + " | "
                                    + row.file_url() + " | " + row.creatorId() + " | "
                                    + row.messageId() + " | " + row.replyId() + " | "
                                    + new java.util.Date(row.as_of().getTime()) + " | "
                                    + (row.last_accessed_at() == null ? "null" : new java.util.Date(row.last_accessed_at().getTime()))
                                    + " | " + row.valid());
                        }
                        break;
                    case "+f": // insert a file
                        int creatorIdFile = getInt(in, "Enter the creator ID");
                        System.out.println("Attach file to:");
                        System.out.println("1 = Message");
                        System.out.println("2 = Reply");
                        int choiceFile = getInt(in, "Enter choice");

                        Integer messageIdFile = null;
                        Integer replyIdFile = null;

                        if (choiceFile == 1) {
                            messageIdFile = getInt(in, "Enter the message ID");
                        } else {
                            replyIdFile = getInt(in, "Enter the reply ID");
                        }

                        int newFileId = db.insertFile(
                                creatorIdFile,
                                messageIdFile,
                                replyIdFile,
                                getString(in, "Enter the file URL"),
                                getString(in, "Enter the file name"),
                                getString(in, "Enter the MIME type"));
                        System.out.println("id of newly inserted file: " + newFileId);
                        break;
                    case "F": // invalidate file
                        db.invalidateFile(getInt(in, "Enter the file ID"));
                        break;
                    case "-f": // delete file
                        db.deleteFile(getInt(in, "Enter the file ID"));
                        break;
                    case "1l": // query for one link
                        var link = db.getOneLink(getInt(in, "Enter the link ID"));
                        if (link != null) {
                            System.out.println(" " + link.id() + " | " + link.url() + " | "
                                    + link.creatorId() + " | " + link.messageId() + " | "
                                    + link.replyId() + " | "
                                    + new java.util.Date(link.as_of().getTime()) + " | "
                                    + (link.last_accessed_at() == null ? "null" : new java.util.Date(link.last_accessed_at().getTime()))
                                    + " | " + link.valid());
                        }
                        break;

                    case "*l": // query for all links
                        System.out.println("  tblLink");
                        System.out.println("  -------------------------");
                        for (var row : db.getAllLinks()) {
                            System.out.println(" " + row.id() + " | " + row.url() + " | "
                                    + row.creatorId() + " | " + row.messageId() + " | "
                                    + row.replyId() + " | "
                                    + new java.util.Date(row.as_of().getTime()) + " | "
                                    + (row.last_accessed_at() == null ? "null" : new java.util.Date(row.last_accessed_at().getTime()))
                                    + " | " + row.valid());
                        }
                        break;

                    case "+l": // insert a link
                        int linkCreatorId = getInt(in, "Enter the creator ID");
                        System.out.println("Attach link to:");
                        System.out.println("1 = Message");
                        System.out.println("2 = Reply");
                        int linkChoice = getInt(in, "Enter choice");

                        Integer linkMessageId = null;
                        Integer linkReplyId = null;

                        if (linkChoice == 1) {
                            linkMessageId = getInt(in, "Enter the message ID");
                        } else {
                            linkReplyId = getInt(in, "Enter the reply ID");
                        }

                        int newLinkId = db.insertLink(
                                linkCreatorId,
                                linkMessageId,
                                linkReplyId,
                                getString(in, "Enter the URL"));
                        System.out.println("id of newly inserted link: " + newLinkId);
                        break;

                    case "Fl": // invalidate link
                        db.invalidateLink(getInt(in, "Enter the link ID"));
                        break;

                    case "-l": // delete link
                        db.deleteLink(getInt(in, "Enter the link ID"));
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //All valid menu options of the program (updated)
    static List<String> menuOptions = Arrays.asList("C", "D", "P", "T", "I", "M", "R", "1p", "*p", "-p", "+p", "~p", "1m", "*m", "-m", "+m",
            "~m", "1r", "*r", "-r", "+r", "~r", "1L", "*L", "-L", "+L", "1f", "*f", "+f", "-f", "F", "1l", "*l", "+l", "Fl", "-l", "q", "?");

    // Print the menu for the program  (updated)
    static void menu() {
        System.out.println("Main Menu");
        System.out.println("  [C] Create tables and views");
        System.out.println("  [D] Drop tables and views");
        System.out.println("  [P] Add Phase 3 Profile Columns");
        System.out.println("  [T] Populate Phase 3 Test Data");
        System.out.println("  [I] Invalidate User");
        System.out.println("  [M] Invalidate Message");
        System.out.println("  [R] Invalidate Reply");
        System.out.println("  [1p] Query for a person");
        System.out.println("  [*p] Query for all person rows");
        System.out.println("  [-p] Delete a person");
        System.out.println("  [+p] Insert a new person");
        System.out.println("  [~p] Update a person");
        System.out.println("  [1m] Query for a specific message");
        System.out.println("  [*m] Query for all message rows");
        System.out.println("  [-m] Delete a message");
        System.out.println("  [+m] Insert a new message");
        System.out.println("  [~m] Update a message");
        System.out.println("  [1r] Query for a specific reply");
        System.out.println("  [*r] Query for all reply rows");
        System.out.println("  [-r] Delete a reply");
        System.out.println("  [+r] Insert a new reply");
        System.out.println("  [~r] Update a reply");
        System.out.println("  [1L] Query for a specific like");
        System.out.println("  [*L] Query for all likes rows");
        System.out.println("  [-L] Delete a like");
        System.out.println("  [+L] Insert a new like");
        System.out.println("  [1f] Query for a specific file");
        System.out.println("  [*f] Query for all file rows");
        System.out.println("  [+f] Insert a new file");
        System.out.println("  [F] Invalidate a file");
        System.out.println("  [-f] Delete a file");
        System.out.println("  [1l] Query for a specific link");
        System.out.println("  [*l] Query for all link rows");
        System.out.println("  [+l] Insert a new link");
        System.out.println("  [Fl] Invalidate a link");
        System.out.println("  [-l] Delete a link");
        System.out.println("  [q] Quit Program");
        System.out.println("  [?] Help (this message)");
    }

    /**
     * Ask the user to enter a menu option; repeat until we get a valid option
     * @param in A BufferedReader, for reading from the keyboard
     * @return The chosen menu option
     */
    static String prompt(BufferedReader in) {
        // Create a set with the valid actions, so it's easy to get the user's
        // request
        var options = new HashSet<String>(menuOptions);

        // Repeat until a valid option is selected
        while (true) {
            System.out.print("[" + String.join(", ", options) + "] :> ");
            try {
                String action = in.readLine();
                if (options.contains(action)) {
                    return action;
                } else if (action == null) {
                    return "q";
                }
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            System.out.println("Invalid Command");
        }
    }

    /**
     * Ask the user to enter a String message
     * @param in A BufferedReader, for reading from the keyboard
     * @param message A message to display when asking for input
     * @return The string that the user provided. May be "".
     */
    static String getString(BufferedReader in, String message) {
        try {
            System.out.print(message + " :> ");
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Ask the user to enter an integer
     * @param in A BufferedReader, for reading from the keyboard
     * @param message A message to display when asking for input
     * @return The integer that the user provided. On error, it will be -1
     */
    static int getInt(BufferedReader in, String message) {
        try {
            System.out.print(message + " :> ");
            return Integer.parseInt(in.readLine());
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}


//push again