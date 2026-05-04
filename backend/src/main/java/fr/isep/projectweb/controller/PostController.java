package fr.isep.projectweb.controller;

import fr.isep.projectweb.model.dto.request.PostRequest;
import fr.isep.projectweb.model.dto.response.PostResponse;
import fr.isep.projectweb.model.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "CRUD endpoints for posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @Operation(summary = "Create a post")
    public PostResponse createPost(@RequestBody PostRequest request, @AuthenticationPrincipal Jwt jwt) {
        return postService.createPost(request, jwt);
    }

    @GetMapping
    @Operation(
            summary = "Get recommended posts for the main community feed",
            description = """
                    Returns a recommendation-ranked feed. Optional keyword searches post title/content plus related \
                    location name/city and related event title/category. Use this endpoint for the main post feed and \
                    contextual discovery, for example keyword=paris can match posts attached to Paris locations.
                    """
    )
    public List<PostResponse> getAllPosts(@RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) UUID locationId,
                                          @RequestParam(required = false) UUID eventId,
                                          @RequestParam(required = false) Integer limit) {
        return postService.getMainFeedPosts(keyword, status, locationId, eventId, limit);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get posts by user id")
    public List<PostResponse> getPostsByUserId(@PathVariable UUID userId) {
        return postService.getPostsByUserId(userId);
    }

    @GetMapping("/location/{locationId}")
    @Operation(summary = "Get posts by location id")
    public List<PostResponse> getPostsByLocationId(@PathVariable UUID locationId) {
        return postService.getPostsByLocationId(locationId);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get posts by event id")
    public List<PostResponse> getPostsByEventId(@PathVariable UUID eventId) {
        return postService.getPostsByEventId(eventId);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search posts by post text only",
            description = """
                    Legacy keyword search. This endpoint only searches the post title and content, then returns matches \
                    ordered by creation time. It does not search related location or event fields. For recommendation \
                    ranking and location/event keyword matches, use GET /api/posts with the keyword query parameter.
                    """
    )
    public List<PostResponse> searchPosts(@RequestParam String keyword) {
        return postService.searchPosts(keyword);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one post by id")
    public PostResponse getPostById(@PathVariable UUID id) {
        return postService.getPostById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post")
    public PostResponse updatePost(@PathVariable UUID id, @RequestBody PostRequest request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
