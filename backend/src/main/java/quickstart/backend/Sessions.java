package quickstart.backend;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;

import java.security.SecureRandom;

// Helper class to make random session keys
class RandStringCreator{ 
    private static final String uppers = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String lowers = "abcdefghijklmnopqrstuvwxyz";
    private static final String nums = "0123456789";
    private static char[] chars = (uppers + lowers + nums).toCharArray();
    private final SecureRandom rng;

    RandStringCreator(){
        this.rng = new SecureRandom();
    }

    public String createSessionKey(){
        return getRandString(21);
    }

    private synchronized String getRandString(int length){
        if(length < 1){
            return null;
        }
        char[] buf = new char[length];
        for(int idx = 0; idx < buf.length; ++idx){
            buf[idx] = chars[rng.nextInt(chars.length)];
        }
        return new String(buf);
    }
}

// Holds the info we stroe per logged in user
record UserRecord(int id, String sessionKey, String emailAddress, String displayName ){}

public class Sessions{

    private static final int SESSION_TIMEOUT = 3600; // sesssion will timeout after 1 hours (3600 secods)

    private final MemcachedClient cache;
    private final RandStringCreator rsc = new RandStringCreator();
    
// COnnect to memchaier using the env vars for Dokku
Sessions() throws Exception{
    String servers = System.getenv("MEMCACHIER_SERVERS");
    String username = System.getenv("MEMCACHIER_USERNAME");
    String pswd = System.getenv("MEMCACHIER_PASSWORD");

    // need all three vars or we cant connect
    if (servers == null || username == null || pswd == null){
        throw new RuntimeException("Missing Memchacier env var");
    }

    AuthDescriptor auth = new AuthDescriptor(new String[]{"PLAIN"}, new PlainCallbackHandler(username, pswd));

    cache = new MemcachedClient(
            new ConnectionFactoryBuilder()
                .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                .setAuthDescriptor(auth)
                .build(),
            AddrUtil.getAddresses(servers)
        );
 
        System.out.println("Connected to Memcachier: " + servers);
        Thread heartbeat = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000);
                    cache.get("__heartbeat__");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {}
            }
        });
        heartbeat.setDaemon(true); // won't prevent JVM shutdown
        heartbeat.start();
        System.out.println("Memcachier heartbeat started");
}

    // Called when a user logs in and stores their session in Memcachier
    String onLogin(String googleId, int id, String email, String name){
        String sessionKey = rsc.createSessionKey();
        // store as a simple strign seperated by lines so we don't need extra libraries (email usually cant include lines (|) so this shoudlnt break)
        String value = (id + "|" + sessionKey + "|" + email + "|" + name);
        cache.set(googleId, SESSION_TIMEOUT, value);
        return sessionKey;
    }

    // // checks if the cookies from the request match an active session in Memcachier
    boolean checkValid(String id, String session){
        if(id == null || session == null || session.equals("")){
            return false;
        }
        var foundSession = get(id);
        return foundSession != null && foundSession.sessionKey().equals(session);
    }

    UserRecord get(String id){
        if (id == null){
            return null;
        }
        Object raw = cache.get(id);
        if (raw == null){
            return null;
        } 
        // split back into parts - limit 4 so names with dashes don't break
        String[] parts = raw.toString().split("\\|", 4);
        if (parts.length < 4) return null;
        try {
            return new UserRecord(Integer.parseInt(parts[0]), parts[1], parts[2], parts[3]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Removes the session from Memcachier when user logs out
    boolean logOut(String mId){
        if (mId == null){
            return false;
        } 
        if (cache.get(mId) == null){
            return false;
        } 
        cache.delete(mId);
        return true;
    }

 }

/* 
Sakshi's notes for future backend:
- I used tutorial 2, I didn't really go beyond that because the tutorial stayed within the scope of the requirements

*/