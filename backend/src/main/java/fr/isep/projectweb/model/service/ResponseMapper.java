package fr.isep.projectweb.model.service;

import fr.isep.projectweb.model.dto.response.EventSummaryResponse;
import fr.isep.projectweb.model.dto.response.EventSaveResponse;
import fr.isep.projectweb.model.dto.response.ImageResponse;
import fr.isep.projectweb.model.dto.response.LocationResponse;
import fr.isep.projectweb.model.dto.response.NotificationResponse;
import fr.isep.projectweb.model.dto.response.PostResponse;
import fr.isep.projectweb.model.dto.response.PublicUserResponse;
import fr.isep.projectweb.model.dto.response.RegistrationResponse;
import fr.isep.projectweb.model.dto.response.ReviewResponse;
import fr.isep.projectweb.model.dto.response.UserSummaryResponse;
import fr.isep.projectweb.model.entity.Event;
import fr.isep.projectweb.model.entity.EventImage;
import fr.isep.projectweb.model.entity.EventReview;
import fr.isep.projectweb.model.entity.EventSave;
import fr.isep.projectweb.model.entity.Location;
import fr.isep.projectweb.model.entity.LocationImage;
import fr.isep.projectweb.model.entity.Notification;
import fr.isep.projectweb.model.entity.Post;
import fr.isep.projectweb.model.entity.PostImage;
import fr.isep.projectweb.model.entity.PostReview;
import fr.isep.projectweb.model.entity.Registration;
import fr.isep.projectweb.model.entity.User;

final class ResponseMapper {

    private ResponseMapper() {
    }

    static UserSummaryResponse toUserSummary(User user) {
        if (user == null) {
            return null;
        }

        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole());
        return response;
    }

    static PublicUserResponse toPublicUserResponse(User user) {
        if (user == null) {
            return null;
        }

        PublicUserResponse response = new PublicUserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setPhoto(user.getPhoto());
        response.setRole(user.getRole());
        return response;
    }

    static NotificationResponse toNotificationResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setBody(notification.getBody());
        response.setActor(toPublicUserResponse(notification.getActor()));
        response.setTargetType(notification.getTargetType());
        response.setTargetId(notification.getTargetId());
        response.setSourceType(notification.getSourceType());
        response.setSourceId(notification.getSourceId());
        response.setPayload(notification.getPayload());
        response.setReadAt(notification.getReadAt());
        response.setArchivedAt(notification.getArchivedAt());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }

    static LocationResponse toLocationResponse(Location location) {
        if (location == null) {
            return null;
        }

        LocationResponse response = new LocationResponse();
        response.setId(location.getId());
        response.setName(location.getName());
        response.setDescription(location.getDescription());
        response.setAddress(location.getAddress());
        response.setCity(location.getCity());
        response.setCountry(location.getCountry());
        response.setLatitude(location.getLatitude());
        response.setLongitude(location.getLongitude());
        response.setCreatedAt(location.getCreatedAt());
        response.setUpdatedAt(location.getUpdatedAt());
        return response;
    }

    static EventSummaryResponse toEventSummary(Event event) {
        if (event == null) {
            return null;
        }

        EventSummaryResponse response = new EventSummaryResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setCategory(event.getCategory());
        response.setStartTime(event.getStartTime());
        response.setEndTime(event.getEndTime());
        response.setStatus(event.getStatus());
        return response;
    }

    static EventSaveResponse toEventSaveResponse(EventSave eventSave) {
        EventSaveResponse response = new EventSaveResponse();
        response.setId(eventSave.getId());
        response.setEvent(toEventSummary(eventSave.getEvent()));
        response.setCreatedAt(eventSave.getCreatedAt());
        return response;
    }

    static PostResponse toPostResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setStatus(post.getStatus());
        response.setUser(toUserSummary(post.getUser()));
        response.setLocation(toLocationResponse(post.getLocation()));
        response.setEvent(toEventSummary(post.getEvent()));
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        return response;
    }

    static RegistrationResponse toRegistrationResponse(Registration registration) {
        RegistrationResponse response = new RegistrationResponse();
        response.setId(registration.getId());
        response.setEvent(toEventSummary(registration.getEvent()));
        response.setUser(toUserSummary(registration.getUser()));
        response.setStatus(registration.getStatus());
        response.setRegisteredAt(registration.getRegisteredAt());
        response.setContactFullName(registration.getContactFullName());
        response.setContactEmail(registration.getContactEmail());
        response.setContactPhone(registration.getContactPhone());
        return response;
    }

    static ImageResponse toEventImageResponse(EventImage image) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }

    static ImageResponse toLocationImageResponse(LocationImage image) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }

    static ImageResponse toPostImageResponse(PostImage image) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setImageUrl(image.getImageUrl());
        response.setCreatedAt(image.getCreatedAt());
        return response;
    }

    static ReviewResponse toEventReviewResponse(EventReview review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setUser(toPublicUserResponse(review.getUser()));
        response.setParentId(review.getParent() != null ? review.getParent().getId() : null);
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    static ReviewResponse toPostReviewResponse(PostReview review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setUser(toPublicUserResponse(review.getUser()));
        response.setParentId(review.getParent() != null ? review.getParent().getId() : null);
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

}
