package com.example.IntisoftTest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.IntisoftTest.model.Article;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
