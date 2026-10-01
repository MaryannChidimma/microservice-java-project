package com.example.productservice.service;

import com.example.productservice.dto.ProductDto;
import com.example.productservice.model.ProductModel;
import com.example.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private ProductDto toDto(ProductModel product) {
        ProductDto dto = new ProductDto(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setCategory(product.getCategory());
        return dto;
    }

    private ProductModel toEntity(ProductDto dto) {
        ProductModel product = new ProductModel(dto.getName(), dto.getDescription(), dto.getPrice(), dto.getStockQuantity());
        product.setImageUrl(dto.getImageUrl());
        product.setCategory(dto.getCategory());
        return product;
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(String id) {
        return productRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public ProductDto createProduct(ProductDto dto) {
        ProductModel saved = productRepository.save(toEntity(dto));
        return toDto(saved);
    }

    public ProductDto updateProduct(String id, ProductDto dto) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setDescription(dto.getDescription());
                    existing.setPrice(dto.getPrice());
                    existing.setStockQuantity(dto.getStockQuantity());
                    existing.setImageUrl(dto.getImageUrl());
                    existing.setCategory(dto.getCategory());
                    return toDto(productRepository.save(existing));
                })
                .orElse(null);
    }

    public boolean deleteProduct(String id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}