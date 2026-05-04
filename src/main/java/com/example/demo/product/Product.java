package com.example.demo.product;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Product {

    @Id
    private Long id;
    private String name;

    @Enumerated(EnumType.STRING)
    private ProductCategory category;

    private int price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Product() {
    }

    public Product(Long id, String name, ProductCategory category, int price, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public int getPrice() {
        return price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void changePrice(int price) {
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }
}
