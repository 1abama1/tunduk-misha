package org.misha.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rental_points")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String phone;

    private String email;
}

