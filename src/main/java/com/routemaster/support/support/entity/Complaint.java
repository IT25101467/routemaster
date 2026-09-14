package com.routemaster.support.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;

    /**
     * Unique Tracking Reference UUID (BR-14).
     * Example: "CMP-4f9a73c1-9d2a-4318-912f-9296e8557a2c"
     */
    @Column(name = "ref_no", nullable = false, unique = true, length = 64)
    private String refNo;

    @Column(name = "booking_ref", length = 64)
    private String bookingRef;

    @Column(name = "passenger_name", length = 128)
    private String passengerName;

    @Column(name = "contact_number", length = 32)
    private String contactNumber;

    @Column(name = "category", nullable = false, length = 64)
    private String category;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /**
     * Officer assigned to resolve this complaint (Architectural Enhancement).
     */
    @Column(name = "assigned_officer", length = 128)
    private String assignedOfficer;
}
