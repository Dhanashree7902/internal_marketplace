package com.internalmarketplace.api.post;

import com.internalmarketplace.api.common.exception.BadRequestException;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.common.web.IdResponse;
import com.internalmarketplace.api.common.web.PagedResponse;
import com.internalmarketplace.api.post.dto.CreatePostRequest;
import com.internalmarketplace.api.post.dto.PostResponse;
import com.internalmarketplace.api.post.dto.UpdatePostRequest;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // GET /api/v1/posts
    @GetMapping
    public PagedResponse<PostResponse> listPosts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String q) {
        return postService.listPosts(categoryId, status, cursor, q);
    }

    // GET /api/v1/posts/{id}
    @GetMapping("/{id}")
    public DataResponse<PostResponse> getPost(@PathVariable String id) {
        return DataResponse.of(postService.getPost(id));
    }

    // POST /api/v1/posts
    @PostMapping
    public ResponseEntity<IdResponse> createPost(@Valid @RequestBody CreatePostRequest body,
                                                  @CurrentUser FirebaseUserPrincipal user) {
        String id = postService.createPost(user.uid(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    // PATCH /api/v1/posts/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable String id, @Valid @RequestBody UpdatePostRequest body,
                                            @CurrentUser FirebaseUserPrincipal user) {
        postService.updatePostAsOwner(id, user.uid(), body);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/v1/posts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable String id, @CurrentUser FirebaseUserPrincipal user) {
        postService.deletePostAsOwner(id, user.uid());
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/posts/{id}/image
    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ResponseEntity<PostService.UploadImageResult> uploadImage(
            @PathVariable String id,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @CurrentUser FirebaseUserPrincipal user) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("No image file provided");
        }
        try {
            PostService.UploadImageResult result =
                    postService.uploadPostImage(id, user.uid(), image.getBytes(), image.getContentType());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
