package quickstart.backend;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import com.google.gson.*;
import java.util.Base64;
import java.util.Map;
import java.util.List;

public class Routes {
    static record StructuredResponse(String status, String message, Object data) {
    }
    public static final String RT_AUTH_GOOGLE_CALLBACK = "/auth/google/callback";
    public static final String RT_AUTHERROR = "/autherror";
    
    public static void readPersonAll(Context ctx, Database db, Gson gson) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            ctx.result(gson.toJson(new StructuredResponse("ok", null, db.getAllPerson())));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void authCallback(Context ctx, Database db, Gson gson, Sessions sessions, GoogleOAuth gOAuth){
        System.out.println(">>>>>>>>>>>>>> invoking authCallBack");
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            GoogleOAuth.OAuthProfile profile = gOAuth.getProfileInformation(ctx.queryParam("code"));
            var user = db.getPersonByEmail(profile.email());
            // redirect if used doenst exit or has been invalidadted by admin
            if(user == null || !user.valid()){
                ctx.redirect(Routes.RT_AUTHERROR);
                return;
            }
            var key = sessions.onLogin(profile.gId(), user.id(), profile.email(), profile.name());
            ctx.cookie("auth.gId", profile.gId());
            ctx.cookie("auth.key", key);
            ctx.cookie("auth.email", profile.email());
            ctx.cookie("auth.name", Base64.getEncoder().encodeToString(profile.name().getBytes()));
            ctx.cookie("auth.id", "" + user.id());
            ctx.redirect("/");
        }
        catch(Exception e){
            System.out.println("Authentication error " + e);
            ctx.redirect(Routes.RT_AUTHERROR);
        }
    }

    public static void authLogout(Context ctx, Gson gson, Sessions sessions){
        String gId = ctx.cookie("auth.gId");
        if(sessions.logOut(gId)){
            ctx.result(gson.toJson("ok"));
        }
        else{
            ctx.result(gson.toJson("error, logging out"));
        }

    }

    public static void readPersonOne(Context ctx,Database db, Gson gson){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            var data = db.getOnePerson(Integer.parseInt(ctx.pathParam("id")));
            if(data == null){
                throw new Exception("No data found");
            }
            ctx.result(gson.toJson(new StructuredResponse("ok", null, data)));
            
        }catch (Exception e){
            ctx.result(gson.toJson(new StructuredResponse("Error", e.getMessage(), null)));
        }
    }

    // Like I did in Database.java, changed old methods so instead of updating Person, will update entire profile for new addaion of GI, SO and note
    // had to add this get method too
    public static void getUserProfile(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            int requestedId = Integer.parseInt(ctx.pathParam("id"));
            int callerID = sessions.get(ctx.cookie("auth.gId")).id();
            var profile = db.getUserProfile(requestedId);
            if(profile == null){
                throw new Exception("User not found");
            }
            // if caller is the owner, return eveything including Gi and So but if not, return only name email and note
            if(callerID == requestedId){
                ctx.result(gson.toJson(new StructuredResponse("ok", null, profile)));
            }
            else{ // SO builds a limited version of the profile so other users cant see GI and SO
                ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of("id", profile.id(), "name", profile.name(), "email", profile.email(), "note", profile.note() == null ? " " : profile.note()
                ))));
            }
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }
    
    public static void updateUserProfile(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            int requestedId = Integer.parseInt(ctx.pathParam("id"));
            int callerId = sessions.get(ctx.cookie("auth.gId")).id();
            // made sure the user is only editing their own profile
            if(callerId != requestedId){
                throw new Exception("You can only edit your own profile.");
            }
            var req = gson.fromJson(ctx.body(), Database.UpdateProfileRequest.class);
            db.updateUserProfile(requestedId, req);
            ctx.result(gson.toJson(new StructuredResponse("ok", null, null)));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("Error", e.getMessage(), null)));
        }
    }
    
    public static void createMessage(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            var req = gson.fromJson(ctx.body(), Database.NewMessageRequest.class);
            long id = db.insertMessage(req, sessions.get(ctx.cookie("auth.gId")).id());
            if(id == -1){
                throw new Exception("Error Inserting");
            }
            ctx.result(gson.toJson(new StructuredResponse("ok", null, "" + id)));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void readMessageAll(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            int currentUserId = -1;
            try {
                currentUserId = sessions.get(ctx.cookie("auth.gId")).id();
            } catch (Exception ignored) {}
            
            ctx.result(gson.toJson(new StructuredResponse("ok", null, db.getAllMessage(currentUserId))));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
}

    public static void readMessageOne(Context ctx, Database db, Gson gson){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            var data = db.getOneMessage(Integer.parseInt(ctx.pathParam("id")));
            if(data == null){
                throw new Exception("No data found");
            }
            ctx.result(gson.toJson(new StructuredResponse("ok", null, data)));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void updateMessage(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            var req = gson.fromJson(ctx.body(), Database.UpdateMessageRequest.class);
            db.updateMessage(Integer.parseInt(ctx.pathParam("id")), req, sessions.get(ctx.cookie("auth.gId")).id() );
            ctx.result(gson.toJson(new StructuredResponse("ok", null, null)));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void deleteMessage(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            db.deleteMessage(Integer.parseInt(ctx.pathParam("id")), sessions.get(ctx.cookie("auth.gId")).id());
            ctx.result(gson.toJson(new StructuredResponse("ok", null, null)));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void createReply(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            var req = gson.fromJson(ctx.body(), Database.NewReplyRequest.class);
            long id = db.insertReply(
                req,
                sessions.get(ctx.cookie("auth.gId")).id(),
                Integer.parseInt(ctx.pathParam("id"))
            );
            if(id == -1){
                throw new Exception("Error Inserting");
            }
            ctx.result(gson.toJson(new StructuredResponse("ok", null, "" + id)));
        }
        catch(Exception e){
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void readRepliesForMessage(Context ctx, Database db, Gson gson, Sessions sessions){
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try{
            int currentUserId = -1;
            try {
                currentUserId = sessions.get(ctx.cookie("auth.gId")).id();
            } catch (Exception ignored) {}
            
            ctx.result(gson.toJson(new StructuredResponse(
                "ok",
                null,
                db.getRepliesForMessage(Integer.parseInt(ctx.pathParam("id")), currentUserId)
            )));
        }
        catch(Exception e){
            System.out.println("ERROR in readRepliesForMessage: " + e.getMessage());
            e.printStackTrace();
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }
    
    //like message
    public static void likeMessage(Context ctx, Database db, Gson gson, Sessions sessions) {
    ctx.status(200);
    ctx.contentType(ContentType.APPLICATION_JSON);
    try {
        int messageId = Integer.parseInt(ctx.pathParam("id"));
        int userId = sessions.get(ctx.cookie("auth.gId")).id();
        
        int likeCount = db.likeMessage(messageId, userId);
        
        ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
            "success", true,
            "likeCount", likeCount,
            "userLiked", true
        ))));
    } catch (Exception e) {
        ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
    }
    }
    
    //unlike a message
    public static void unlikeMessage(Context ctx, Database db, Gson gson, Sessions sessions) {
    ctx.status(200);
    ctx.contentType(ContentType.APPLICATION_JSON);
    try {
        int messageId = Integer.parseInt(ctx.pathParam("id"));
        int userId = sessions.get(ctx.cookie("auth.gId")).id();
        
        int likeCount = db.unlikeMessage(messageId, userId);
        
        ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
            "success", true,
            "likeCount", likeCount,
            "userLiked", false
        ))));
    } catch (Exception e) {
        ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
    }
}
    
    //like a reply
    public static void likeReply(Context ctx, Database db, Gson gson, Sessions sessions) {
    ctx.status(200);
    ctx.contentType(ContentType.APPLICATION_JSON);
    try {
        int replyId = Integer.parseInt(ctx.pathParam("id"));
        int userId = sessions.get(ctx.cookie("auth.gId")).id();
        
        int likeCount = db.likeReply(replyId, userId);
        
        ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
            "success", true,
            "likeCount", likeCount,
            "userLiked", true
        ))));
    } catch (Exception e) {
        ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
    }
    }
    
    //unlike a reply
    public static void unlikeReply(Context ctx, Database db, Gson gson, Sessions sessions) {
    ctx.status(200);
    ctx.contentType(ContentType.APPLICATION_JSON);
    try {
        int replyId = Integer.parseInt(ctx.pathParam("id"));
        int userId = sessions.get(ctx.cookie("auth.gId")).id();
        
        int likeCount = db.unlikeReply(replyId, userId);
        
        ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
            "success", true,
            "likeCount", likeCount,
            "userLiked", false
        ))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    //Phase 4 file uploading routes

    public static void uploadFileToMessage(Context ctx, Database db, Gson gson, Sessions sessions, GoogleCloudStorageService gcs) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int messageId = Integer.parseInt(ctx.pathParam("id"));
            int userId = sessions.get(ctx.cookie("auth.gId")).id();

            var body = gson.fromJson(ctx.body(), JsonObject.class);
            String fileName = body.get("fileName").getAsString();
            String mimeType = body.get("mimeType").getAsString();
            String fileDataBase64 = body.get("fileData").getAsString();

            if (!gcs.validateMimeType(mimeType)) {
                ctx.status(415);
                ctx.result(gson.toJson(new StructuredResponse("error", "Invalid file type", null)));
                return;
            }

            byte[] fileData = Base64.getDecoder().decode(fileDataBase64);

            if (!gcs.validateFileSize(fileData)) {
                ctx.status(413);
                ctx.result(gson.toJson(new StructuredResponse("error", "File too large (max 10MB)", null)));
                return;
            }

            String fileUrl = gcs.uploadFile(fileData, fileName, mimeType);
            System.out.println("GCS upload OK, url=" + fileUrl + ", about to insert to DB");
            long fileId = db.insertFile(userId, messageId, null, fileName, fileUrl, mimeType);
            System.out.println("DB insert returned fileId=" + fileId);

            ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
                "file_id", fileId,
                "file_name", fileName,
                "file_url", fileUrl
            ))));
        } catch (Exception e) {
                System.out.println("UPLOAD FAILED: " + e.getMessage());
                e.printStackTrace();
                ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
            }
    }

    public static void uploadFileToReply(Context ctx, Database db, Gson gson, Sessions sessions, GoogleCloudStorageService gcs) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int replyId = Integer.parseInt(ctx.pathParam("id"));
            int userId = sessions.get(ctx.cookie("auth.gId")).id();

            var body = gson.fromJson(ctx.body(), JsonObject.class);
            String fileName = body.get("fileName").getAsString();
            String mimeType = body.get("mimeType").getAsString();
            String fileDataBase64 = body.get("fileData").getAsString();

            if (!gcs.validateMimeType(mimeType)) {
                ctx.status(415);
                ctx.result(gson.toJson(new StructuredResponse("error", "Invalid file type", null)));
                return;
            }

            byte[] fileData = Base64.getDecoder().decode(fileDataBase64);

            if (!gcs.validateFileSize(fileData)) {
                ctx.status(413);
                ctx.result(gson.toJson(new StructuredResponse("error", "File too large (max 10MB)", null)));
                return;
            }

            String fileUrl = gcs.uploadFile(fileData, fileName, mimeType);
            long fileId = db.insertFile(userId, null, replyId, fileName, fileUrl, mimeType);

            ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
                "file_id", fileId,
                "file_name", fileName,
                "file_url", fileUrl
            ))));
        } catch (Exception e) {
            System.out.println("UPLOAD FAILED: " + e.getMessage());
            e.printStackTrace();
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void getFile(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int fileId = Integer.parseInt(ctx.pathParam("id"));
            var file = db.getFile(fileId);
            if (file == null) {
                throw new Exception("File not found");
            }
            db.updateFileLastAccessed(fileId);
            ctx.result(gson.toJson(new StructuredResponse("ok", null, file)));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void deleteFile(Context ctx, Database db, Gson gson, Sessions sessions, GoogleCloudStorageService gcs) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int fileId = Integer.parseInt(ctx.pathParam("id"));
            int userId = sessions.get(ctx.cookie("auth.gId")).id();
            var file = db.getFile(fileId);
            if (file == null) {
                throw new Exception("File not found");
            }
            if (file.creatorId() != userId) {
                ctx.status(403);
                ctx.result(gson.toJson(new StructuredResponse("error", "Not authorized", null)));
                return;
            }
            gcs.deleteFile(file.fileUrl());
            db.deleteFile(fileId);
            ctx.result(gson.toJson(new StructuredResponse("ok", null, null)));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void getFilesForMessage(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int messageId = Integer.parseInt(ctx.pathParam("id"));
            ctx.result(gson.toJson(new StructuredResponse("ok", null, db.getFilesByMessage(messageId))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void getFilesForReply(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int replyId = Integer.parseInt(ctx.pathParam("id"));
            ctx.result(gson.toJson(new StructuredResponse("ok", null, db.getFilesByReply(replyId))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    //Phase 4 Link Routes!

    public static void addLinkToMessage(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int messageId = Integer.parseInt(ctx.pathParam("id"));
            int userId = sessions.get(ctx.cookie("auth.gId")).id();

            var body = gson.fromJson(ctx.body(), JsonObject.class);
            String url = body.get("url").getAsString();

            if (url == null || url.isBlank()) {
                throw new Exception("Invalid URL");
            }

            long linkId = db.insertLink(userId, messageId, null, url);
            ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
                "link_id", linkId,
                "url", url
            ))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void addLinkToReply(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int replyId = Integer.parseInt(ctx.pathParam("id"));
            int userId = sessions.get(ctx.cookie("auth.gId")).id();

            var body = gson.fromJson(ctx.body(), JsonObject.class);
            String url = body.get("url").getAsString();

            if (url == null || url.isBlank()) {
                throw new Exception("Invalid URL");
            }

            long linkId = db.insertLink(userId, null, replyId, url);
            ctx.result(gson.toJson(new StructuredResponse("ok", null, Map.of(
                "link_id", linkId,
                "url", url
            ))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void deleteLink(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int linkId = Integer.parseInt(ctx.pathParam("id"));
            int userId = sessions.get(ctx.cookie("auth.gId")).id();
            var link = db.getLink(linkId);
            if (link == null) {
                throw new Exception("Link not found");
            }
            if (link.creatorId() != userId) {
                ctx.status(403);
                ctx.result(gson.toJson(new StructuredResponse("error", "Not authorized", null)));
                return;
            }
            db.deleteLink(linkId);
            ctx.result(gson.toJson(new StructuredResponse("ok", null, null)));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void getLink(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int linkId = Integer.parseInt(ctx.pathParam("id"));
            var link = db.getLink(linkId);
            if (link == null) {
                throw new Exception("Link not found");
            }
            ctx.result(gson.toJson(new StructuredResponse("ok", null, link)));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void getLinksForMessage(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int messageId = Integer.parseInt(ctx.pathParam("id"));
            ctx.result(gson.toJson(new StructuredResponse("ok", null, db.getLinksByMessage(messageId))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }

    public static void getLinksForReply(Context ctx, Database db, Gson gson, Sessions sessions) {
        ctx.status(200);
        ctx.contentType(ContentType.APPLICATION_JSON);
        try {
            int replyId = Integer.parseInt(ctx.pathParam("id"));
            ctx.result(gson.toJson(new StructuredResponse("ok", null, db.getLinksByReply(replyId))));
        } catch (Exception e) {
            ctx.result(gson.toJson(new StructuredResponse("error", e.getMessage(), null)));
        }
    }


}
