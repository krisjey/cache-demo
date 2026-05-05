package com.example.demo.support;

import com.example.demo.product.Product;
import com.example.demo.product.ProductCategory;
import com.example.demo.product.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (productRepository.count() > 0) {
            log.info("Sample products already exist. skip initialization.");
            return;
        }

        try {
            productRepository.saveAll(createProducts());
            productRepository.flush();
            log.info("Sample products initialized.");
        } catch (DataIntegrityViolationException e) {
            log.info("Sample products were initialized by another application instance. skip duplicated initialization.");
        }
    }

    private List<Product> createProducts() {
        LocalDateTime now = LocalDateTime.now();
        List<Product> products = new ArrayList<>();
        ProductCategory[] categories = ProductCategory.values();

        for (long i = 1; i <= 20; i++) {
            ProductCategory category = categories[(int) ((i - 1) % categories.length)];
            products.add(new Product(
                    i,
                    "Product-" + i,
                    category,
                    1_000 + (int) i * 700,
                    now.minusDays(20 - i),
                    now.minusDays(20 - i)
            ));
        }

        return products;
    }
}
