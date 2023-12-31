package com.compassuol.sp.challenge.msproducts.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.compassuol.sp.challenge.msproducts.constants.ProductsConstants.VALID_PRODUCT;
import static com.compassuol.sp.challenge.msproducts.constants.ProductsConstants.VALID_PRODUCT_DTO;
import static com.compassuol.sp.challenge.msproducts.constants.ProductsConstants.INVALID_PRODUCT_DTO;

import com.compassuol.sp.challenge.msproducts.controller.exception.errorTypes.ProductNotFoundException;
import com.compassuol.sp.challenge.msproducts.dto.ProductDTO;
import com.compassuol.sp.challenge.msproducts.model.ProductModel;
import com.compassuol.sp.challenge.msproducts.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @BeforeEach
    public void setUp() {
        // Limpar a tabela de produtos antes de cada teste
        Mockito.when(productService.getAllProducts()).thenReturn(
                Arrays.asList(
                        new ProductModel("Product 1", "Product 1 description", 10.0),
                        new ProductModel("Product 2", "Product 2 description", 20.0)
                )
        );
        when(productService.isProductExistsByName("ProdutoExistente")).thenReturn(true);
        when(productService.isProductExistsByName("ProdutoNovo")).thenReturn(false);
    }

    @Test
    public void testGetProducts() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/products"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Product 1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].value").value(10.0))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].name").value("Product 2"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[1].value").value(20.0));
    }

    @Test
    public void testGetProductsEmpty() throws Exception {
        // Limpar a tabela de produtos para simular uma lista vazia
        Mockito.when(productService.getAllProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get("/products"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetProductByIdSuccess() throws Exception {
        long productId = 1L;
        ProductModel product = new ProductModel("Product 1", "Product 1 description", 10.0);
        when(productService.findProductByIdService(productId)).thenReturn(Optional.of(product));

        mockMvc.perform(MockMvcRequestBuilders.get("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Product 1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.value").value(10.0));
        //.andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
    }

    @Test
    public void testGetProductById_ProductNotFound() throws Exception {
        long productId = 999L; // Use um ID que não corresponda a um produto existente no seu banco de dados simulado
        when(productService.findProductByIdService(productId)).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void updateProduct_withValidData_ReturnsCreated() throws Exception {
        VALID_PRODUCT.setId(1L);
        when(productService.updateProductService(VALID_PRODUCT, VALID_PRODUCT_DTO)).thenReturn(VALID_PRODUCT);
        when(productService.findProductByIdService(1L)).thenReturn(Optional.of(VALID_PRODUCT));
        mockMvc
                .perform(put("/products/{id}", 1L).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(VALID_PRODUCT_DTO)))
                .andExpect(status().isCreated());
    }

    @Test
    public void updateProduct_withInvalidId_ReturnsException() throws Exception {
        VALID_PRODUCT.setId(2L);
        when(productService.updateProductService(VALID_PRODUCT, VALID_PRODUCT_DTO)).thenReturn(VALID_PRODUCT);
        when(productService.findProductByIdService(1L)).thenThrow(ProductNotFoundException.class);
        mockMvc
                .perform(put("/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(VALID_PRODUCT_DTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateProduct_withInvalidData_returnsException() throws Exception {
        VALID_PRODUCT.setId(1L);
        //when(productService.updateProductService(VALID_PRODUCT, VALID_PRODUCT_DTO)).thenReturn();
        when(productService.findProductByIdService(1L)).thenReturn(Optional.of(VALID_PRODUCT));
        mockMvc
                .perform(put("/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(INVALID_PRODUCT_DTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteProduct_withInvalidId_ReturnsException() throws Exception {
        when(productService.findProductByIdService(2L)).thenThrow(ProductNotFoundException.class);
        mockMvc
                .perform(delete("/products/{id}", 2L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deleteProduct_withValidId_ReturnsNoContent() throws Exception {
        when(productService.findProductByIdService(1L)).thenReturn(Optional.of(VALID_PRODUCT));
        mockMvc
                .perform(delete("/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testCreateProduct_UniqueName() throws Exception {
        ProductDTO productDTO = new ProductDTO("ProdutoNovo", "Descrição do Produto", 9.99);
        mockMvc.perform(MockMvcRequestBuilders.post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated());

        verify(productService, times(1)).createProductService(any(ProductModel.class));
    }
}
