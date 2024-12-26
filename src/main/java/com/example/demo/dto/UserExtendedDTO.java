package com.example.demo.dto;

import com.example.demo.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserExtendedDTO {
    private Long id;
    private String username;
    private String avatarPath;
    private String email;
    private String visibleName;
    private String registrationDate;
    private String phoneNumber;

    public UserExtendedDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.avatarPath = user.getAvatarPath();
        this.email = user.getEmail();
        this.visibleName = user.getVisibleName();
        this.registrationDate = user.getRegistrationDate().toString();
        this.phoneNumber = user.getPhoneNumber();
    }
}
