package com.BlackDot.Finance.Tracker.UserActivity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.BlackDot.Finance.Tracker.Auth.CurrentUser;
import com.BlackDot.Finance.Tracker.UserActivity.DTO.UserActivityResponse;

@RestController
@RequestMapping("/api/v1/user-activity")
public class UserActivityController {

    private final UserActivityService service;

    public UserActivityController(UserActivityService service) {
        this.service = service;
    }

    @GetMapping
    public UserActivityResponse getActivity(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) Integer year) {
        return service.getActivity(CurrentUser.id(), days, year);
    }
}
