package com.pat.hours_calculator.model.auth.entities;


import com.pat.hours_calculator.model.user.entities.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id", nullable = false)
    private Long refreshTokenId;


    @Column(name = "token", nullable = false)
    private String token;


    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name="expiration_date", nullable = false)
    private LocalDate expirationDate;

    public RefreshToken() {
    }

    public RefreshToken(String token, User user) {
        this.token = token;
        this.user = user;
    }

}
