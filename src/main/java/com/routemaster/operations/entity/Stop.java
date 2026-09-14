package com.routemaster.operations.entity;

import com.routemaster.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stop")
@Getter
@Setter
@NoArgsConstructor
public class Stop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;

    @NotBlank(message = "Stop name is mandatory")
    @Column(name = "stop_name", nullable = false, length = 100)
    private String stopName;

    @NotBlank(message = "City is mandatory")
    @Column(nullable = false, length = 80)
    private String city;

    public Stop(String stopName, String city) {
        this.stopName = stopName;
        this.city = city;
    }
}
