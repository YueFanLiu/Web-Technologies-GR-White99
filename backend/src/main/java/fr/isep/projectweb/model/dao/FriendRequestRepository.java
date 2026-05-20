package fr.isep.projectweb.model.dao;

import fr.isep.projectweb.model.entity.FriendRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    List<FriendRequest> findByAddresseeIdAndStatusOrderByCreatedAtDesc(UUID addresseeId, String status);

    List<FriendRequest> findByRequesterIdAndStatusOrderByCreatedAtDesc(UUID requesterId, String status);

    @Query("""
            SELECT fr
            FROM FriendRequest fr
            WHERE fr.status = 'ACCEPTED'
              AND (fr.requester.id = :userId OR fr.addressee.id = :userId)
            ORDER BY fr.updatedAt DESC, fr.createdAt DESC
            """)
    List<FriendRequest> findAcceptedByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT fr
            FROM FriendRequest fr
            WHERE ((fr.requester.id = :firstUserId AND fr.addressee.id = :secondUserId)
                   OR (fr.requester.id = :secondUserId AND fr.addressee.id = :firstUserId))
              AND fr.status = 'ACCEPTED'
            """)
    Optional<FriendRequest> findAcceptedBetweenUsers(@Param("firstUserId") UUID firstUserId,
                                                     @Param("secondUserId") UUID secondUserId);

    @Query("""
            SELECT fr
            FROM FriendRequest fr
            WHERE ((fr.requester.id = :firstUserId AND fr.addressee.id = :secondUserId)
                   OR (fr.requester.id = :secondUserId AND fr.addressee.id = :firstUserId))
            ORDER BY fr.createdAt DESC
            """)
    List<FriendRequest> findLatestBetweenUsers(@Param("firstUserId") UUID firstUserId,
                                               @Param("secondUserId") UUID secondUserId,
                                               Pageable pageable);
}
