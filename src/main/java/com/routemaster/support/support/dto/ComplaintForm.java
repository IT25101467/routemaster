package com.routemaster.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintForm {

    private String bookingRef;

    @NotBlank(message = "Passenger name is required.")
    @Size(max = 128, message = "Name must not exceed 128 characters.")
    private String passengerName;

    @NotBlank(message = "Contact number is required.")
    @Size(max = 32, message = "Contact number must not exceed 32 characters.")
    private String contactNumber;

    @NotBlank(message = "Category selection is mandatory.")
    private String category;

    @NotBlank(message = "Complaint description is required.")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters.")
    private String description;
}
