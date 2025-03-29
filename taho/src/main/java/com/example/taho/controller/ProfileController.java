package com.example.taho.controller;

import com.example.taho.dao.UserRepository;
import com.example.taho.entity.UserProfile;
import com.example.taho.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import com.example.taho.entity.User;


@RequestMapping("/menu")
@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    // プロフィール表示
    @GetMapping("/profile")
    public String showProfile(Model model, Principal principal) {
        String username = principal.getName();  // 今ログイン中のユーザー名を取得
        User user = userRepository.findByUsername(username); // 🔥 AuthService使ってもOK

        if (user == null) {
        // エラー処理（普通は起きないけど）
            return "redirect:/login";
        }

        // UserProfile を取得（または新規作成）
        UserProfile profile = profileService.getProfileByUsername(user.getUsername());
        if (profile == null) {
            profile = new UserProfile();
            profile.setUsername(user.getUsername());
            profileService.createProfile(profile);  // save() でDBに入れる
        }

        model.addAttribute("profile", profile);
        return "menu/profile";
    }


    // プロフィール編集画面へ
    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal) {
        String username = principal.getName();
        UserProfile profile = profileService.getProfileByUsername(username);
        model.addAttribute("profile", profile);
        return "menu/profile_edit";  
    }

    // プロフィールの更新
    @PostMapping("/update")
    public String updateProfile(@ModelAttribute UserProfile profile, Principal principal) {
        String username = principal.getName();
        profile.setUsername(username);  // 念のため再設定
        profileService.updateProfile(profile);
        return "redirect:/menu";
    }
}
