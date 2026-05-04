package fr.isep.projectweb.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private static final long DEFAULT_MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String publishableKey;
    private final String serviceRoleKey;
    private final String imagesBucket;
    private final long maxImageSizeBytes;

    public SupabaseStorageService(@Value("${supabase.url}") String supabaseUrl,
                                  @Value("${supabase.publishable-key:}") String publishableKey,
                                  @Value("${supabase.service-role-key:}") String serviceRoleKey,
                                  @Value("${supabase.storage.images-bucket:images}") String imagesBucket,
                                  @Value("${supabase.storage.max-image-size-bytes:" + DEFAULT_MAX_IMAGE_SIZE_BYTES + "}") long maxImageSizeBytes) {
        this.supabaseUrl = removeTrailingSlash(supabaseUrl);
        this.publishableKey = publishableKey;
        this.serviceRoleKey = serviceRoleKey;
        this.imagesBucket = imagesBucket;
        this.maxImageSizeBytes = maxImageSizeBytes;
        this.restClient = RestClient.builder()
                .baseUrl(this.supabaseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String uploadEventImage(UUID eventId, MultipartFile file, String authorizationHeader) {
        return uploadImage("eventImages", eventId, file, authorizationHeader);
    }

    public String uploadLocationImage(UUID locationId, MultipartFile file, String authorizationHeader) {
        return uploadImage("locationImages", locationId, file, authorizationHeader);
    }

    public String uploadPostImage(UUID postId, MultipartFile file, String authorizationHeader) {
        return uploadImage("postImages", postId, file, authorizationHeader);
    }

    public String uploadUserAvatar(UUID userId, MultipartFile file, String authorizationHeader) {
        return uploadImage("userAvatar", userId, file, authorizationHeader);
    }

    private String uploadImage(String directory, UUID ownerId, MultipartFile file, String authorizationHeader) {
        validateImage(file);
        ensureConfigured();

        String objectPath = "%s/%s/%s%s".formatted(
                directory,
                ownerId,
                UUID.randomUUID(),
                extensionFor(file)
        );

        try {
            restClient.post()
                    .uri("/storage/v1/object/%s/%s".formatted(
                            encodePathSegment(imagesBucket),
                            encodeObjectPath(objectPath)
                    ))
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header("apikey", publishableKey)
                    .header(HttpHeaders.AUTHORIZATION, authorizationForStorage(authorizationHeader))
                    .header("x-upsert", "false")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(exception.getStatusCode().value()),
                    "Supabase Storage upload failed: " + storageErrorMessage(exception),
                    exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded image", exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to upload image to Supabase Storage", exception);
        }

        return "%s/storage/v1/object/public/%s/%s".formatted(
                supabaseUrl,
                encodePathSegment(imagesBucket),
                encodeObjectPath(objectPath)
        );
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file must not be empty");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is too large");
        }

        String contentType = file.getContentType();
        if (!isAllowedImageContentType(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must be a JPEG or PNG image");
        }
    }

    private boolean isAllowedImageContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        String normalized = contentType.toLowerCase(Locale.ROOT);
        return MediaType.IMAGE_JPEG_VALUE.equals(normalized) || "image/png".equals(normalized);
    }

    private void ensureConfigured() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase URL is not configured");
        }
        if (publishableKey == null || publishableKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase publishable key is not configured");
        }
        if (imagesBucket == null || imagesBucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Supabase images bucket is not configured");
        }
    }

    private String authorizationForStorage(String authorizationHeader) {
        if (serviceRoleKey != null && !serviceRoleKey.isBlank()) {
            return "Bearer " + serviceRoleKey;
        }
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            return authorizationHeader;
        }
        return "Bearer " + publishableKey;
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType();
        if (MediaType.IMAGE_JPEG_VALUE.equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        return "";
    }

    private String storageErrorMessage(RestClientResponseException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return exception.getStatusText();
        }
        return responseBody;
    }

    private String encodeObjectPath(String objectPath) {
        String[] segments = objectPath.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodePathSegment(segments[i]));
        }
        return encoded.toString();
    }

    private String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
