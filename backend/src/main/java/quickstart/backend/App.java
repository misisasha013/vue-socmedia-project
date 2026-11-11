package quickstart.backend;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.sql.SQLException;
import com.google.gson.*;

public class App {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv("PORT"));
        String dbUrl = System.getenv("DATABASE_URL");
        String clientId = System.getenv("CLIENT_ID");
        String clientSecret = System.getenv("CLIENT_SECRET");
        String serverName = System.getenv("SERVER_NAME");
        String staticLocation = System.getenv("STATIC_LOCATION");

        System.out.println("-".repeat(45));
        System.out.println("Using the following environment variables:");
        System.out.println("  PORT=" + port);
        System.out.println("  DATABASE_URL=" + dbUrl);
        System.out.println("  CLIENT_ID=" + clientId);
        System.out.printf("  CLIENT_SECRET=%s%s%n", "*".repeat(clientSecret.length() - 5),
                clientSecret.substring(clientSecret.length() - 5, clientSecret.length()));
        System.out.println("  SERVER_NAME=" + serverName);
        System.out.println("  STATIC_LOCATION=" + staticLocation);
        System.out.println("-".repeat(45));

        if (dbUrl == null || serverName == null ||
                clientId == null || clientSecret == null ||
                port < 80 || port > 65535) {
            System.err.println("Error in environment configuration");
            return;
        }

        Database db;
        try {
            db = new Database(dbUrl);
            db.runMigrations();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Sessions now connects to Memcachier so it can throw an exception
        Sessions sessions;
        try{
            sessions = new Sessions();
        }
        catch (Exception e){
            e.printStackTrace();
            return;
        }

        GoogleCloudStorageService gcs;
        try {
            gcs = new GoogleCloudStorageService();
        } catch (Exception e) {
            System.out.println("WARNING: GCS not available: " + e.getMessage());
            gcs = null;
        }

        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();

        var app = Javalin.create(config -> {
            config.requestLogger.http((ctx, ms) -> {
                System.out.println("=".repeat(80));
                System.out.printf("%-6s%-8s%-25s%s%n", ctx.scheme(), ctx.method().name(), ctx.path(),
                        ctx.fullUrl());
                if (ctx.queryString() != null)
                    System.out.printf("query string:%s%n", ctx.queryString());
                if (ctx.body().length() > 0)
                    System.out.printf("request body:%s%n", ctx.body());
            });
            config.staticFiles.add(staticFiles -> {
                if (staticLocation == null) {
                    System.out.println("Serving files from JAR");
                    staticFiles.location = Location.CLASSPATH;
                    staticFiles.directory = "/public";
                }
              
                else {
                    System.out.println("Serving files from EXTERNAL LOCATION");
                    staticFiles.location = Location.EXTERNAL;
                    staticFiles.directory = staticLocation;
                }
                System.out.printf("Using staticFiles.directory=%s%n", staticFiles.directory);
                staticFiles.precompress = false;  
            });
            
            if (staticLocation == null) {
                String defaultPage = "public/index.html";
                System.out.println(
                        "********************** STATIC_LOCATION == null --> setting spaRoot to " + defaultPage);
                config.spaRoot.addFile("/", defaultPage, Location.CLASSPATH);
            } else {
                String defaultPage = staticLocation + "/index.html";
                System.out.println(
                        "********************** STATIC_LOCATION != null --> setting spaRoot to " + defaultPage);
                config.spaRoot.addFile("/", defaultPage, Location.EXTERNAL);
            }
        });

        // var sessions = new Sessions();
        var gOAuth = new GoogleOAuth(serverName, port, clientId, clientSecret, Routes.RT_AUTH_GOOGLE_CALLBACK);

        app.before(ctx -> {
            try {
                if (ctx.url().equals(gOAuth.redirectUri.replace("https://", "http://"))) {
                    System.out.println(">>>>>>> SETTING UP A NEW SESSION, at " + gOAuth.redirectUri);
                    return;
                }
                String gId = ctx.cookie("auth.gId");
                String key = ctx.cookie("auth.key");
                if (sessions.checkValid(gId, key)) {
                    return;
                }
                System.out.println(">>>>>>> INVALID SESSION, redirecting to " + gOAuth.newAuthUrl);
                ctx.redirect(gOAuth.newAuthUrl);
            } catch (Exception e) {
                System.err.println("Before filter error (redirecting to login): " + e.getMessage());
                ctx.redirect(gOAuth.newAuthUrl);
            }
        });

        app.get(Routes.RT_AUTH_GOOGLE_CALLBACK, ctx -> Routes.authCallback(ctx, db, gson, sessions, gOAuth));
        app.get("/logout", ctx -> Routes.authLogout(ctx, gson, sessions));
        app.get("/api/people", ctx -> Routes.readPersonAll(ctx, db, gson));
        app.get("/api/people/{id}", ctx -> Routes.readPersonOne(ctx, db, gson));
        // Replaced old people route with updated user profile routes
        app.get("/api/users/{id}", ctx -> Routes.getUserProfile(ctx, db, gson, sessions));
        app.put("/api/users/{id}", ctx -> Routes.updateUserProfile(ctx, db, gson, sessions));
        app.post("/api/messages", ctx -> Routes.createMessage(ctx, db, gson, sessions));
        app.get("/api/messages", ctx -> Routes.readMessageAll(ctx, db, gson, sessions));
        app.get("/api/messages/{id}", ctx -> Routes.readMessageOne(ctx, db, gson));
        app.put("/api/messages/{id}", ctx -> Routes.updateMessage(ctx, db, gson, sessions));
        app.delete("/api/messages/{id}", ctx -> Routes.deleteMessage(ctx, db, gson, sessions));
        app.post("/api/messages/{id}/replies", ctx -> Routes.createReply(ctx, db, gson, sessions));
        app.get("/api/messages/{id}/replies", ctx -> Routes.readRepliesForMessage(ctx, db, gson, sessions));
        app.post("/api/messages/{id}/like", ctx -> Routes.likeMessage(ctx, db, gson, sessions));
        app.delete("/api/messages/{id}/like", ctx -> Routes.unlikeMessage(ctx, db, gson, sessions));
        app.post("/api/replies/{id}/like", ctx -> Routes.likeReply(ctx, db, gson, sessions));
        app.delete("/api/replies/{id}/like", ctx -> Routes.unlikeReply(ctx, db, gson, sessions));
        //Phase 4 File routes
        final GoogleCloudStorageService gcsService = gcs;
        app.post("/api/messages/{id}/files", ctx -> Routes.uploadFileToMessage(ctx, db, gson, sessions, gcsService));
        app.post("/api/replies/{id}/files", ctx -> Routes.uploadFileToReply(ctx, db, gson, sessions, gcsService));
        app.get("/api/files/{id}", ctx -> Routes.getFile(ctx, db, gson, sessions));
        app.delete("/api/files/{id}", ctx -> Routes.deleteFile(ctx, db, gson, sessions, gcsService));
        //Phase 4 Link routes
        app.post("/api/messages/{id}/links", ctx -> Routes.addLinkToMessage(ctx, db, gson, sessions));
        app.post("/api/replies/{id}/links", ctx -> Routes.addLinkToReply(ctx, db, gson, sessions));
        app.get("/api/links/{id}", ctx -> Routes.getLink(ctx, db, gson, sessions));
        app.delete("/api/links/{id}", ctx -> Routes.deleteLink(ctx, db, gson, sessions));
        //Phase4 Get routes
        app.get("/api/messages/{id}/files", ctx -> Routes.getFilesForMessage(ctx, db, gson, sessions));
        app.get("/api/messages/{id}/links", ctx -> Routes.getLinksForMessage(ctx, db, gson, sessions));
        app.get("/api/replies/{id}/files", ctx -> Routes.getFilesForReply(ctx, db, gson, sessions));
        app.get("/api/replies/{id}/links", ctx -> Routes.getLinksForReply(ctx, db, gson, sessions));        

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down Javalin...");
                app.stop(); 
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                System.out.println("Shutting down Database...");
                db.close();
                System.out.println("Done");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        app.start(port);
    }
}
/* 
Sakshi's notes for future backend:
- I used tutorial 2 and 4 as a basis, I didn't really go beyond that because the tutorial stayed within the scope of the requirements

Julia's notes for Phase 3:
- new Sessionis() shoudl now throw an excpetions so it needs a try catch
- removed the old people PUT route since it was upsatePersont that no longer exisits
- add the two new user profile routes (get and put)

*/