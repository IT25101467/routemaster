package com.routemaster.notifications.repository;

import com.routemaster.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBookingRefOrderBySentAtDesc(String bookingRef);

    List<Notification> findByBookingRefAndIsActiveTrueOrderBySentAtDesc(String bookingRef);

    List<Notification> findAllByOrderBySentAtDesc();

    List<Notification> findByIsActiveTrueOrderBySentAtDesc();

    List<Notification> findByAlertTypeInAndIsActiveTrueOrderBySentAtDesc(java.util.Collection<com.routemaster.notifications.entity.NotificationType> types);

    List<Notification> findByTripIdAndIsActiveTrueOrderBySentAtDesc(Long tripId);
}
