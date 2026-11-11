package quickstart.backend;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;

public class GoogleOAuth {

    public final String redirectUri;
    public final String newAuthUrl;
    public static final String APPLICATION_NAME = "CSE216 TEAM4";
    private NetHttpTransport httpTransport;
    private GoogleAuthorizationCodeFlow flow;
    public GoogleOAuth(String serverName, int port, String clientId, String clientSecret, String authCallbackEndpoint) {
        var gApis = new ArrayList<String>(Arrays.asList(
                "https://www.googleapis.com/auth/userinfo.email",
                "https://www.googleapis.com/auth/userinfo.profile"));
        if (serverName.toLowerCase().endsWith("dokku.cse.lehigh.edu"))
            this.redirectUri = serverName + authCallbackEndpoint;
        else 
            this.redirectUri = serverName + ":" + port + authCallbackEndpoint;
        this.httpTransport = new NetHttpTransport();
        this.flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, GsonFactory.getDefaultInstance(), clientId,
                clientSecret, gApis)
                .setAccessType("offline").build();
        this.newAuthUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
        System.out.println("<".repeat(45));
        System.out.println("Using the following oauth information:");
        System.out.println("  redirectURI: " + redirectUri);
        System.out.println("  newAuthUrl: " + newAuthUrl);
        System.out.println(">".repeat(45));
    }

    public static record OAuthProfile(String gId, String email, String name) {
    }

    public OAuthProfile getProfileInformation(String queryParamCode) throws IOException {
        var ps = new PeopleService.Builder(
                httpTransport,
                GsonFactory.getDefaultInstance(),
                flow.createAndStoreCredential(
                        flow.newTokenRequest(queryParamCode).setRedirectUri(redirectUri).execute(),
                        null))
                .setApplicationName(APPLICATION_NAME)
                .build();
        var profile = ps.people().get("people/me").setPersonFields("names,emailAddresses").execute();
        String email = profile.getEmailAddresses().get(0).getValue();
        String name = profile.getNames().get(0).getDisplayName();
        String gId = profile.getEmailAddresses().get(0).getMetadata().getSource().getId();
        return new OAuthProfile(gId, email, name);
    }
}
/* 
Sakshi's notes for future backend:
- I used tutorial 2 and 4 as a basis, I didn't really go beyond that because the tutorial stayed within the scope of the requirements

*/