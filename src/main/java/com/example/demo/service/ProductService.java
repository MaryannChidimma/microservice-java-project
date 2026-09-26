package com.example.demo.service;

import com.example.demo.dto.ProductDto;
import com.example.demo.model.ProductModel;
import com.example.demo.repository.ProductRepository;
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
        return new ProductDto(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStockQuantity());
    }

    private ProductModel toEntity(ProductDto dto) {
        return new ProductModel(dto.getName(), dto.getDescription(), dto.getPrice(), dto.getStockQuantity());
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductDto getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    public ProductDto createProduct(ProductDto dto) {
        ProductModel saved = productRepository.save(toEntity(dto));
        return toDto(saved);
    }

    public ProductDto updateProduct(Long id, ProductDto dto) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(dto.getName());
                    existing.setDescription(dto.getDescription());
                    existing.setPrice(dto.getPrice());
                    existing.setStockQuantity(dto.getStockQuantity());
                    return toDto(productRepository.save(existing));
                })
                .orElse(null);
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }
}