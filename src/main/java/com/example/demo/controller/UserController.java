package com.example.demo.controller;

import com.example.demo.dto.UserExtendedDTO;
import com.example.demo.model.Bookmark;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.BookmarkService;
import com.example.demo.service.ImageService;
import com.example.demo.service.ReviewService;
import com.example.demo.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.demo.Utils.makeResponse;
import static com.example.demo.Utils.makeResponseString;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final ImageService imageService;
    private final BookmarkService bookmarkService;

    @PostMapping("/api/delete-user/{id}")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable long id) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            boolean isOwnId = user.getId() == id;
            User otherUser = isOwnId ? user : userService.findById(id);
            boolean isAdmin = user.getRole() == Role.ADMIN;
            boolean isOtherUserFound = otherUser != null;
            boolean isOtherUserAdmin = isOtherUserFound && otherUser.getRole() == Role.ADMIN;
            if (id != -1 && ((!isAdmin && isOwnId) ||
                    (isAdmin && !isOwnId && isOtherUserFound && !isOtherUserAdmin))) {
                userService.deleteById(id);
                return makeResponseString("Account deleted successfully", HttpStatus.OK);
            } else {
                if (id == -1)
                    return makeResponseString(
                            "Dead cannot die again",
                            HttpStatus.FORBIDDEN);
                if (!isOtherUserFound)
                    return makeResponseString(
                            "User not found with id: " + id,
                            HttpStatus.NOT_FOUND);
                if (!isAdmin)
                    return makeResponseString(
                            "Permission denied, you're not an administrator",
                            HttpStatus.FORBIDDEN);
                if (isOwnId)
                    return makeResponseString(
                            "Admin can't delete themself",
                            HttpStatus.BAD_REQUEST);
                return makeResponseString(
                        "Admin can't delete other administrator",
                        HttpStatus.FORBIDDEN);
            }
        } catch (Exception e) {
            return makeResponseString(
                    "Failed to delete account: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/api/update-profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                           @RequestParam String email,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam String visibleName) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            if (user == null) {
                return makeResponseString("User not found", HttpStatus.NOT_FOUND);
            }

            boolean changed = false;
            if (!email.equals(user.getEmail())) {
                if (userService.existsByEmail(email)) {
                    return makeResponseString("Email already exists", HttpStatus.BAD_REQUEST);
                }
                user.setEmail(email);
                changed = true;
            }

            if (!visibleName.equals(user.getVisibleName())) {
                user.setVisibleName(visibleName);
                changed = true;
            }

            if (phone != null && !phone.equals(user.getPhoneNumber())) {
                user.setPhoneNumber(phone);
                changed = true;
            }

            if (changed) {
                userService.save(user);
            }
            return ResponseEntity.ok(Map.of(
                    "changed", changed,
                    "userData", new UserExtendedDTO(user)
            ));
        } catch (Exception e) {
            return makeResponseString(
                    "Failed to update profile: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/api/update-role/{id}")
    public ResponseEntity<?> updateRole(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable long id,
                                        @RequestBody Map<String, String> request) {
        String role = request.get("role");
        System.out.println(role);
        Role otherUserOldRole = null;
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            boolean isAdmin = user.getRole() == Role.ADMIN;

            if (!isAdmin) {
                return makeResponse(
                        Map.of(
                                "response", "Permission denied, you're not an administrator",
                                "role", false),
                        HttpStatus.FORBIDDEN);
            }
            System.out.println("User is admin");
            if (user.getId() == id) {
                return makeResponse(
                        Map.of(
                                "response", "Admin can't demote/promote themself",
                                "role", user.getRoleString()),
                        HttpStatus.BAD_REQUEST);
            }
            System.out.println("User is not other user");
            Role newRole = Role.valueOf(role);
            if (newRole == Role.ADMIN) {
                return makeResponse(
                        Map.of(
                                "response", "Promoting to admin is not allowed",
                                "role", false),
                        HttpStatus.BAD_REQUEST);
            }
            System.out.println("New role is " + newRole);
            User otherUser = userService.findById(id);
            if (otherUser == null) {
                return makeResponse(
                        Map.of(
                                "response", "User not found with id: " + id,
                                "role", false),
                        HttpStatus.NOT_FOUND);
            }
            System.out.println("Other user found");
            otherUserOldRole = otherUser.getRole();
            if (otherUserOldRole == Role.ADMIN) {
                return makeResponse(
                        Map.of(
                                "response", "Admin can't demote/promote other admin",
                                "role", otherUserOldRole),
                        HttpStatus.FORBIDDEN);
            }
            System.out.println("Other user is not admin");
            otherUser.setRole(newRole);
            userService.save(otherUser);
            return makeResponseString("Role updated successfully", HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            return makeResponse(
                    Map.of(
                            "response", "Unknown role: " + role,
                            "role", false),
                    HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return makeResponse(
                    Map.of(
                            "response", "Failed to update role: " + e.getMessage(),
                            "role", otherUserOldRole != null ? otherUserOldRole : false),
                    HttpStatus.BAD_REQUEST);
        }
    }


    @PostMapping("/api/update-avatar")
    public ResponseEntity<?> updateAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("avatar") MultipartFile avatar) {
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            String avatarPath = user.getAvatarPath();
            if (avatarPath != null)
                imageService.deleteImage(avatarPath);
            avatarPath = imageService.storeImage(avatar);
            user.setAvatarPath(avatarPath);
            userService.save(user);
            return ResponseEntity.ok(Map.of(
                    "response", "Avatar updated successfully",
                    "avatarPath", avatarPath
            ));
        } catch (Exception e) {
            return makeResponseString("Failed to update avatar: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/api/get-bookmarks")
    public ResponseEntity<?> getBookmarks(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Bookmark> bookmarks = bookmarkService.findAllByUser(user);
        return ResponseEntity.ok(bookmarks.stream()
                .map(bookmark -> Map.of("reviewId", bookmark.getReview().getId(), "reviewTitle", bookmark.getReview().getTitle()))
                .collect(Collectors.toList()));
    }


}
