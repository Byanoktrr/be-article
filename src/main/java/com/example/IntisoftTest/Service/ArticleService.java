package com.example.IntisoftTest.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.IntisoftTest.model.Article;
import com.example.IntisoftTest.repository.ArticleRepository;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    public Article create(Article article) {
        return articleRepository.save(article);
    }

    public Optional<Article> update(Long id, Article updatedArticle) {
        return articleRepository.findById(id).map(article -> {
            article.setTitle(updatedArticle.getTitle());
            article.setContent(updatedArticle.getContent());
            article.setAuthor(updatedArticle.getAuthor());
            article.setUpdatedAt(updatedArticle.getUpdatedAt());
            return articleRepository.save(article);
        });
    }

    public boolean delete(Long id) {
        if (articleRepository.existsById(id)) {
            articleRepository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}
