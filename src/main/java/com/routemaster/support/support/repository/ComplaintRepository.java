package com.routemaster.support.repository;

import com.routemaster.support.entity.Complaint;
import com.routemaster.support.entity.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Optional<Complaint> findByRefNo(String refNo);

    List<Complaint> findByStatusOrderByCreatedAtDesc(ComplaintStatus status);

    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByBookingRefOrderByCreatedAtDesc(String bookingRef);
}
