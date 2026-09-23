package com.internalmarketplace.api.post;

import com.google.cloud.firestore.FieldValue;
import com.internalmarketplace.api.category.CategoryRepository;
import com.internalmarketplace.api.category.dto.CategoryResponse;
import com.internalmarketplace.api.common.exception.BadRequestException;
import com.internalmarketplace.api.common.exception.ConflictException;
import com.internalmarketplace.api.common.exception.ForbiddenException;
import com.internalmarketplace.api.common.exception.NotFoundException;
import com.internalmarketplace.api.common.exception.UnprocessableEntityException;
import com.internalmarketplace.api.common.search.SearchKeywords;
import com.internalmarketplace.api.common.web.PagedResponse;
import com.internalmarketplace.api.post.dto.CreatePostRequest;
import com.internalmarketplace.api.post.dto.PostImageResponse;
import com.internalmarketplace.api.post.dto.PostResponse;
import com.internalmarketplace.api.post.dto.UpdatePostRequest;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import com.internalmarketplace.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Business rules for posts, a direct port of post.service.js / post.controller.js. */
@Service
@RequiredArgsConstructor
public class PostService {

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final StorageService storageService;
    private final UserRepository userRepository;

    public PagedResponse<PostResponse> listPosts(String categoryId, String status, String cursor, String query) {
        String effectiveStatus = (status == null || status.isBlank()) ? "ACTIVE" : status;
        List<String> searchTerms = (query == null || query.isBlank())
                ? List.of()
                : SearchKeywords.normalizeSearchQuery(query);

        PostRepository.PagedResult result = postRepository.listPosts(categoryId, effectiveStatus, cursor, searchTerms);
        List<PostResponse> withImageUrls = withImageUrls(withAuthors(result.items()));
        return PagedResponse.cursor(withImageUrls, result.nextCursor());
    }

    // Page-number pagination for Home ("all posts") and My Posts ("mine=true" +
    // a specific status tab) -- see PostRepository#listPostsPage for why this
    // uses offset() rather than a cursor.
    public PagedResponse<PostResponse> listPostsPaged(String categoryId, String status, String query, boolean mine,
                                                        String currentUserId, Integer page, Integer size,
                                                        String sortDir) {
        int effectivePage = page == null ? 1 : page;
        int effectiveSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (effectivePage < 1) {
            throw new BadRequestException("page must be >= 1");
        }
        if (effectiveSize < 1 || effectiveSize > MAX_PAGE_SIZE) {
            throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        boolean ascending = resolveAscending(sortDir);

        String effectiveStatus = (status == null || status.isBlank()) ? "ACTIVE" : status;
        List<String> searchTerms = (query == null || query.isBlank())
                ? List.of()
                : SearchKeywords.normalizeSearchQuery(query);
        String userId = mine ? currentUserId : null;

        PostRepository.PageResult result = postRepository.listPostsPage(
                categoryId, effectiveStatus, searchTerms, userId, effectivePage, effectiveSize, ascending);
        List<PostResponse> withImageUrls = withImageUrls(withAuthors(result.items()));
        return PagedResponse.page(withImageUrls, result.page(), result.size(), result.totalElements());
    }

    private static boolean resolveAscending(String sortDir) {
        if (sortDir == null || sortDir.isBlank() || "desc".equalsIgnoreCase(sortDir)) {
            return false;
        }
        if ("asc".equalsIgnoreCase(sortDir)) {
            return true;
        }
        throw new BadRequestException("sortDir must be 'asc' or 'desc'");
    }

    private List<PostResponse> withImageUrls(List<PostResponse> posts) {
        return posts.stream()
                .map(post -> post.coverImageKey() != null
                        ? post.withImageUrl(storageService.signedReadUrl(post.coverImageKey()))
                        : post)
                .toList();
    }

    public PostResponse getPost(String id) {
        PostResponse found = postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        PostResponse post = userRepository.findSummaryById(found.userId())
                .map(summary -> found.withAuthor(summary.email(), summary.name()))
                .orElse(found);
        List<PostImageResponse> images = postRepository.listImages(id).stream()
                .map(image -> image.withUrl(storageService.signedReadUrl(image.objectKey())))
                .toList();
        return post.withImages(images);
    }

    private List<PostResponse> withAuthors(List<PostResponse> posts) {
        Set<String> userIds = new LinkedHashSet<>();
        for (PostResponse post : posts) {
            userIds.add(post.userId());
        }
        // One combined round trip for every distinct author on the page instead of
        // one sequential Firestore get per author (see UserRepository#findSummariesByIds).
        Map<String, UserRepository.UserSummary> summaryByUid = userRepository.findSummariesByIds(userIds);
        return posts.stream()
                .map(post -> {
                    UserRepository.UserSummary summary = summaryByUid.get(post.userId());
                    return summary != null ? post.withAuthor(summary.email(), summary.name()) : post;
                })
                .toList();
    }

    public String createPost(String userId, CreatePostRequest request) {
        CategoryResponse category = categoryRepository.findById(request.categoryId()).orElse(null);
        if (category == null || !"ACTIVE".equals(category.status())) {
            throw new UnprocessableEntityException("Posts can only be created in an active category");
        }

        List<String> keywords = SearchKeywords.buildSearchKeywords(request.title(), request.description(), category.name());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("category_id", request.categoryId());
        data.put("user_id", userId);
        data.put("title", request.title());
        data.put("description", request.description());
        data.put("post_type", request.postType());
        data.put("price", request.price());
        data.put("tags", request.tags());
        data.put("expiry_date", request.expiryDate());
        data.put("status", "ACTIVE");
        data.put("search_keywords", keywords);
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("updated_at", FieldValue.serverTimestamp());

        return postRepository.create(data);
    }

    // Admins may edit any post for moderation/policy purposes; everyone else
    // only their own.
    public void updatePost(String id, FirebaseUserPrincipal user, UpdatePostRequest patch) {
        PostResponse existing = requireEditAccess(id, user, "You can only edit your own posts");

        Map<String, Object> update = new LinkedHashMap<>();
        if (patch.title() != null) {
            update.put("title", patch.title());
        }
        if (patch.description() != null) {
            update.put("description", patch.description());
        }
        if (patch.price() != null) {
            update.put("price", patch.price());
        }
        if (patch.status() != null) {
            update.put("status", patch.status().name());
        }

        if (patch.title() != null || patch.description() != null) {
            CategoryResponse category = categoryRepository.findById(existing.categoryId()).orElse(null);
            update.put("search_keywords", SearchKeywords.buildSearchKeywords(
                    patch.title() != null ? patch.title() : existing.title(),
                    patch.description() != null ? patch.description() : existing.description(),
                    category != null ? category.name() : null));
        }

        postRepository.update(id, update);
    }

    // Admins may delete any post regardless of status, for moderation/policy
    // enforcement. A non-admin owner is still restricted to closed/archived
    // posts only (their own self-service cleanup, not full moderation power).
    public void deletePost(String id, FirebaseUserPrincipal user) {
        PostResponse post = requireEditAccess(id, user, "You can only delete your own posts");
        boolean deletable = user.isAdmin() || "CLOSED".equals(post.status()) || "ARCHIVED".equals(post.status());
        if (!deletable) {
            throw new ConflictException("Only closed or archived posts can be deleted");
        }
        postRepository.updateStatus(id, "REMOVED");
    }

    public UploadImageResult uploadPostImage(String postId, String userId, byte[] bytes, String contentType) {
        if (!ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new com.internalmarketplace.api.common.exception.BadRequestException("Image must be JPEG, PNG, or WebP");
        }
        requireOwnedPost(postId, userId, "You can only add images to your own posts");

        String objectKey = "posts/" + postId + "/" + Instant.now().toEpochMilli() + "-" + randomToken();
        storageService.upload(objectKey, bytes, contentType);

        Map<String, Object> imageData = new LinkedHashMap<>();
        imageData.put("object_key", objectKey);
        imageData.put("sort_order", 0);
        imageData.put("metadata", Map.of("contentType", contentType));
        imageData.put("created_at", FieldValue.serverTimestamp());
        String imageId = postRepository.addImage(postId, imageData);

        postRepository.setCoverImageKey(postId, objectKey);

        return new UploadImageResult(imageId, storageService.signedReadUrl(objectKey));
    }

    private PostResponse requireOwnedPost(String id, String userId, String forbiddenMessage) {
        PostResponse post = postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        if (!userId.equals(post.userId())) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return post;
    }

    // Same ownership check as requireOwnedPost, but admins bypass it -- used by
    // update/delete, which are the two operations admins need moderation
    // access to. Image upload still goes through the owner-only check above.
    private PostResponse requireEditAccess(String id, FirebaseUserPrincipal user, String forbiddenMessage) {
        PostResponse post = postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        if (!user.isAdmin() && !user.uid().equals(post.userId())) {
            throw new ForbiddenException(forbiddenMessage);
        }
        return post;
    }

    private static String randomToken() {
        return Long.toString((long) (Math.random() * Long.MAX_VALUE), 36);
    }

    public record UploadImageResult(String imageId, String url) {
    }
}
