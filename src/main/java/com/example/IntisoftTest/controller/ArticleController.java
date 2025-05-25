package com.example.IntisoftTest.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.IntisoftTest.Security.AccessControlService;
import com.example.IntisoftTest.Service.AuditLogService;
import com.example.IntisoftTest.model.Article;
import com.example.IntisoftTest.model.Role;
import com.example.IntisoftTest.model.User;
import com.example.IntisoftTest.repository.ArticleRepository;
import com.example.IntisoftTest.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AccessControlService accessControlService;

    @Autowired
    private UserRepository userRepository;

    private Map<String, Object> response(boolean success, String message, Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", success);
        res.put("message", message);
        if (data != null) {
            res.put("data", data);
        }
        return res;
    }

    @GetMapping
    public ResponseEntity<?> getAllArticles() {
        List<Article> articles = articleRepository.findAll();
        return ResponseEntity.ok(response(true, "Get all articles", articles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getArticleById(@PathVariable Long id) {
        Optional<Article> articleOpt = articleRepository.findById(id);
        if (articleOpt.isPresent()) {
            return ResponseEntity.ok(response(true, "Article found", articleOpt.get()));
        } else {
            return ResponseEntity.status(404).body(response(false, "Article not found", null));
        }
    }

    @PostMapping
    public ResponseEntity<?> createArticle(@RequestBody Article article, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(response(false, "User not found", null));
        }

        if (!accessControlService.hasAnyRole(Role.SUPER_ADMIN, Role.EDITOR, Role.CONTRIBUTOR)) {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        article.setAuthor(user);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());

        Article savedArticle = articleRepository.save(article);

        auditLogService.log(
                savedArticle.getId(),
                "ARTICLE_CREATE",
                "Created article titled \"" + savedArticle.getTitle() + "\" by " + username,
                getClientIp(request)
        );

        return ResponseEntity.ok(response(true, "Article created successfully", savedArticle));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateArticle(@PathVariable Long id, @RequestBody Article updatedArticle, HttpServletRequest request) {
        Optional<Article> articleOpt = articleRepository.findById(id);
        if (articleOpt.isEmpty()) {
            return ResponseEntity.status(404).body(response(false, "Article not found", null));
        }

        Article article = articleOpt.get();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(response(false, "User not found", null));
        }

        if (accessControlService.hasRole(Role.SUPER_ADMIN)) {
        } else if (accessControlService.hasRole(Role.EDITOR) || accessControlService.hasRole(Role.CONTRIBUTOR)) {
            if (!article.getAuthor().getUsername().equals(username)) {
                return ResponseEntity.status(403).body(response(false, "Access Denied (Only own article can be edited)", null));
            }
        } else {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        article.setTitle(updatedArticle.getTitle());
        article.setContent(updatedArticle.getContent());
        article.setUpdatedAt(LocalDateTime.now());

        Article saved = articleRepository.save(article);

        auditLogService.log(
                article.getId(),
                "ARTICLE_UPDATE",
                "Updated article titled \"" + saved.getTitle() + "\" by " + username,
                getClientIp(request)
        );

        return ResponseEntity.ok(response(true, "Article updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable Long id, HttpServletRequest request) {
        Optional<Article> articleOpt = articleRepository.findById(id);
        if (articleOpt.isEmpty()) {
            return ResponseEntity.status(404).body(response(false, "Article not found", null));
        }

        Article article = articleOpt.get();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(response(false, "User not found", null));
        }

        if (accessControlService.hasRole(Role.SUPER_ADMIN)) {
        } else if (accessControlService.hasRole(Role.EDITOR)) {
            if (!article.getAuthor().getUsername().equals(username)) {
                return ResponseEntity.status(403).body(response(false, "Access Denied (Only own article can be deleted)", null));
            }
        } else {
            return ResponseEntity.status(403).body(response(false, "Access Denied", null));
        }

        articleRepository.deleteById(id);

        auditLogService.log(
                article.getId(),
                "ARTICLE_DELETE",
                "Deleted article titled \"" + article.getTitle() + "\" by " + username,
                getClientIp(request)
        );

        return ResponseEntity.ok(response(true, "Article deleted successfully", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }
}
