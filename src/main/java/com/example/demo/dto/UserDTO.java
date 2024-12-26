package com.example.demo.dto;

import com.example.demo.model.User;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String avatarPath;
}