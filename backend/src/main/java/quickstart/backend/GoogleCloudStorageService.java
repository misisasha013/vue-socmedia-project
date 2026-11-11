package quickstart.backend;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.util.UUID;

public class GoogleCloudStorageService {
    private final Storage storage;
    private final String bucketName = "team-04-buzz-files";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; //10MB

    public GoogleCloudStorageService() {
    String credentialsB64 = System.getenv("GOOGLE_CLOUD_STORAGE_CREDENTIALS_B64");
    if (credentialsB64 == null || credentialsB64.isEmpty()) {
        throw new RuntimeException("GOOGLE_CLOUD_STORAGE_CREDENTIALS_B64 env var not set");
    }
    try {
        byte[] decoded = java.util.Base64.getDecoder().decode(credentialsB64);
        storage = StorageOptions.newBuilder()
            .setCredentials(ServiceAccountCredentials.fromStream(
                new ByteArrayInputStream(decoded)))
            .build()
            .getService();
    } catch (Exception e) {
        throw new RuntimeException("Failed to initialize GCS: " + e.getMessage(), e);
    }
}


    public String uploadFile(byte[] fileData, String fileName, String mimeType) {
        String uniqueName = UUID.randomUUID() + "_" + fileName;
        BlobId blobId = BlobId.of(bucketName, uniqueName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(mimeType)
            .build();
        storage.create(blobInfo, fileData);
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, uniqueName);
    }

    public void deleteFile(String fileUrl) {
        //Extract blob name from URL
        String prefix = "https://storage.googleapis.com/" + bucketName + "/";
        if (!fileUrl.startsWith(prefix)) return;
        String blobName = fileUrl.substring(prefix.length());
        storage.delete(BlobId.of(bucketName, blobName));
    }

    public boolean validateMimeType(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("image/") ||
               mimeType.equals("application/pdf") ||
               mimeType.equals("text/plain");
    }

    public boolean validateFileSize(byte[] data) {
        return data != null && data.length <= MAX_FILE_SIZE;
    }
}
