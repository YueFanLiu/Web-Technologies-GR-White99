package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.PostRepository;
import fr.isep.projectweb.model.dto.request.PostRequest;
import fr.isep.projectweb.model.dto.response.PostResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.Post;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class PostService {

    private static final int SEARCH_RESULT_LIMIT = 20;

    private final PostRepository postRepository;
    private final LocationDAO locationDAO;
    private final EventRepository eventRepository;
    private final CurrentUserService currentUserService;

    public PostService(PostRepository postRepository,
                       LocationDAO locationDAO,
                       EventRepository eventRepository,
                       CurrentUserService currentUserService) {
        this.postRepository = postRepository;
        this.locationDAO = locationDAO;
        this.eventRepository = eventRepository;
        this.currentUserService = currentUserService;
    }

    public PostResponse createPost(PostRequest request, Jwt jwt) {
        Post post = new Post();
        post.setUser(currentUserService.getOrCreateCurrentUser(jwt));
        applyRequest(post, request);
        return ResponseMapper.toPostResponse(postRepository.save(post));
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll()
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getPostsByUserId(UUID userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getPostsByLocationId(UUID locationId) {
        return postRepository.findByLocationIdOrderByCreatedAtDesc(locationId)
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> getPostsByEventId(UUID eventId) {
        return postRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public List<PostResponse> searchPosts(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keyword must not be blank");
        }

        return postRepository.searchByKeyword(keyword.trim(), PageRequest.of(0, SEARCH_RESULT_LIMIT))
                .stream()
                .map(ResponseMapper::toPostResponse)
                .toList();
    }

    public PostResponse getPostById(UUID id) {
        return ResponseMapper.toPostResponse(findPostById(id));
    }

    public PostResponse updatePost(UUID id, PostRequest request) {
        Post post = findPostById(id);
        applyRequest(post, request);
        return ResponseMapper.toPostResponse(postRepository.save(post));
    }

    public void deletePost(UUID id) {
        Post post = findPostById(id);
        postRepository.delete(post);
    }

    private void applyRequest(Post post, PostRequest request) {
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setStatus(request.getStatus());
        post.setLocation(findLocation(request.getLocationId()));
        post.setEvent(findEvent(request.getEventId()));
    }

    private Post findPostById(UUID id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private Location findLocation(UUID locationId) {
        if (locationId == null) {
            return null;
        }

        return locationDAO.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    private Event findEvent(UUID eventId) {
        if (eventId == null) {
            return null;
        }

        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }
}
