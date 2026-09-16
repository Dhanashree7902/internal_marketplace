package com.internalmarketplace.api.comment;

import com.internalmarketplace.api.comment.dto.CommentResponse;
import com.internalmarketplace.api.comment.dto.CreateCommentRequest;
import com.internalmarketplace.api.common.web.DataResponse;
import com.internalmarketplace.api.common.web.IdResponse;
import com.internalmarketplace.api.security.CurrentUser;
import com.internalmarketplace.api.security.FirebaseUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts/{id}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // GET /api/v1/posts/{id}/comments
    @GetMapping
    public DataResponse<List<CommentResponse>> listComments(@PathVariable String id) {
        return DataResponse.of(commentService.listComments(id));
    }

    // POST /api/v1/posts/{id}/comments
    @PostMapping
    public ResponseEntity<IdResponse> createComment(@PathVariable String id,
                                                     @Valid @RequestBody CreateCommentRequest body,
                                                     @CurrentUser FirebaseUserPrincipal user) {
        String commentId = commentService.createComment(id, user.uid(), user.email(), body.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(commentId));
    }
}
