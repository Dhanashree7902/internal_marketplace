package com.internalmarketplace.api.post;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Wraps Firebase Storage uploads and signed read URLs, matching
 * post.service.js's getSignedReadUrl/uploadPostImage exactly: images are
 * proxied through this backend (not a client-direct signed PUT) and read
 * back via a 1-hour v4-signed URL. Every operation here references the
 * bucket purely by name (via BlobId), with no eager bucket-existence check,
 * matching the Node SDK's lazy storage.bucket() handle.
 */
@Service
public class StorageService {

    private static final long SIGNED_URL_DURATION_HOURS = 1;

    private final Storage storage;
    private final String bucketName;

    public StorageService(Storage storage, @Value("${firebase.storage-bucket:}") String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    public void upload(String objectKey, byte[] content, String contentType) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectKey))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, content);
    }

    public String signedReadUrl(String objectKey) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectKey)).build();
        URL url = storage.signUrl(blobInfo, SIGNED_URL_DURATION_HOURS, TimeUnit.HOURS,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }
}
