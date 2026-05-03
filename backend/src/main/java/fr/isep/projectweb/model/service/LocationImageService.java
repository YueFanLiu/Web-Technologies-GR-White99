package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dao.LocationDAO;
import fr.isep.projectweb.model.dao.LocationImageRepository;
import fr.isep.projectweb.model.dto.request.ImageRequest;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.LocationImage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class LocationImageService {

    private final LocationImageRepository locationImageRepository;
    private final LocationDAO locationDAO;

    public LocationImageService(LocationImageRepository locationImageRepository, LocationDAO locationDAO) {
        this.locationImageRepository = locationImageRepository;
        this.locationDAO = locationDAO;
    }

    public List<ImageResponse> getByLocationId(UUID locationId) {
        findLocation(locationId);
        return locationImageRepository.findByLocationIdOrderByCreatedAtAsc(locationId)
                .stream()
                .map(ResponseMapper::toLocationImageResponse)
                .toList();
    }

    public ImageResponse create(UUID locationId, ImageRequest request) {
        validateImageUrl(request.getImageUrl());

        LocationImage image = new LocationImage();
        image.setLocation(findLocation(locationId));
        image.setImageUrl(request.getImageUrl().trim());
        return ResponseMapper.toLocationImageResponse(locationImageRepository.save(image));
    }

    public void delete(UUID locationId, UUID imageId) {
        LocationImage image = locationImageRepository.findByIdAndLocationId(imageId, locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location image not found"));
        locationImageRepository.delete(image);
    }

    private Location findLocation(UUID locationId) {
        return locationDAO.findById(locationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image URL must not be blank");
        }
    }
}
