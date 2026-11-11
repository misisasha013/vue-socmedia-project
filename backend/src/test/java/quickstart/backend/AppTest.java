package quickstart.backend;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test
    void validProfile_shouldNotThrow() {
        Database.UpdateProfileRequest req =
            new Database.UpdateProfileRequest("Alice", "Nonbinary", "Bisexual", "hello");
        assertDoesNotThrow(req::validate);
    }

    @Test
    void nullName_shouldThrow() {
        Database.UpdateProfileRequest req =
            new Database.UpdateProfileRequest(null, "Female", "Heterosexual", "hello");
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void emptyName_shouldThrow() {
        Database.UpdateProfileRequest req =
            new Database.UpdateProfileRequest("", "Male", "Homosexual", "note");
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void noteTooLong_shouldThrow() {
        String longNote = "a".repeat(151);
        Database.UpdateProfileRequest req =
            new Database.UpdateProfileRequest("Alice", "Other", "Questioning", longNote);
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void nullGIAndSO_shouldNotThrow() {
        Database.UpdateProfileRequest req =
            new Database.UpdateProfileRequest("Alice", null, null, null);
        assertDoesNotThrow(req::validate);
    }

    @Test
    void validNewMessage_shouldNotThrow() {
        Database.NewMessageRequest req =
            new Database.NewMessageRequest("Subject", "Details here");
        assertDoesNotThrow(req::validate);
    }

    @Test
    void newMessage_nullSubject_shouldThrow() {
        Database.NewMessageRequest req =
            new Database.NewMessageRequest(null, "Details");
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void newMessage_subjectTooLong_shouldThrow() {
        String longSubject = "a".repeat(51);
        Database.NewMessageRequest req =
            new Database.NewMessageRequest(longSubject, "Details");
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void newMessage_detailsTooLong_shouldThrow() {
        String longDetails = "a".repeat(501);
        Database.NewMessageRequest req =
            new Database.NewMessageRequest("Subject", longDetails);
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void validDetails_shouldNotThrow() {
        Database.UpdateMessageRequest req =
            new Database.UpdateMessageRequest("Valid message");
        assertDoesNotThrow(req::validate);
    }

    @Test
    void nullDetails_shouldThrow() {
        Database.UpdateMessageRequest req =
            new Database.UpdateMessageRequest(null);
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void emptyDetails_shouldThrow() {
        Database.UpdateMessageRequest req =
            new Database.UpdateMessageRequest("");
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void validReply_shouldNotThrow() {
        Database.NewReplyRequest req =
            new Database.NewReplyRequest("This is a reply");
        assertDoesNotThrow(req::validate);
    }

    @Test
    void reply_nullContent_shouldThrow() {
        Database.NewReplyRequest req =
            new Database.NewReplyRequest(null);
        assertThrows(RuntimeException.class, req::validate);
    }

    @Test
    void reply_contentTooLong_shouldThrow() {
        String longContent = "a".repeat(501);
        Database.NewReplyRequest req =
            new Database.NewReplyRequest(longContent);
        assertThrows(RuntimeException.class, req::validate);
    }
}