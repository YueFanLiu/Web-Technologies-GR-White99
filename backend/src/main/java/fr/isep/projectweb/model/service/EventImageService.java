package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.EventImageRepository;
import fr.isep.projectweb.model.dao.EventRepository;
import fr.isep.projectweb.model.dto.request.ImageRequest;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventImage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class EventImageService {

    private final EventImageRepository eventImageRepository;
    private final EventRepository eventRepository;
    private final SupabaseStorageService supabaseStorageService;

    public EventImageService(EventImageRepository eventImageRepository,
                             EventRepository eventRepository,
                             SupabaseStorageService supabaseStorageService) {
        this.eventImageRepository = eventImageRepository;
        this.eventRepository = eventRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    public List<ImageResponse> getByEventId(UUID eventId) {
        findEvent(eventId);
        return eventImageRepository.findByEventIdOrderByCreatedAtAsc(eventId)
                .stream()
                .map(ResponseMapper::toEventImageResponse)
                .toList();
    }

    public ImageResponse create(UUID eventId, ImageRequest request) {
        validateImageUrl(request.getImageUrl());

        EventImage image = new EventImage();
        image.setEvent(findEvent(eventId));
        image.setImageUrl(request.getImageUrl().trim());
        return ResponseMapper.toEventImageResponse(eventImageRepository.save(image));
    }

    public ImageResponse upload(UUID eventId, MultipartFile file, String authorizationHeader) {
        Event event = findEvent(eventId);
        String imageUrl = supabaseStorageService.uploadEventImage(eventId, file, authorizationHeader);

        EventImage image = new EventImage();
        image.setEvent(event);
        image.setImageUrl(imageUrl);
        return ResponseMapper.toEventImageResponse(eventImageRepository.save(image));
    }

    public void delete(UUID eventId, UUID imageId) {
        EventImage image = eventImageRepository.findByIdAndEventId(imageId, eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event image not found"));
        eventImageRepository.delete(image);
    }

    private Event findEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL must not be blank");
        }
    }
}
