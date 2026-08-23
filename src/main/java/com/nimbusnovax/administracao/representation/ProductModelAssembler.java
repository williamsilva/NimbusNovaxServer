package com.nimbusnovax.administracao.representation;

import com.nimbusnovax.administracao.controller.ProductController;
import com.nimbusnovax.administracao.model.Product;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class ProductModelAssembler extends RepresentationModelAssemblerSupport<Product, ProductModel> {

  public ProductModelAssembler() {
    super(ProductController.class, ProductModel.class);
  }

  @Override
  public ProductModel toModel(Product product) {
    ProductModel model = createModelWithId(product.getId(), product);

    model.setId(product.getId());
    model.setName(product.getName());
    model.setDescription(product.getDescription());
    model.setTypeProduct(product.getTypeProductEnum());
    model.setAmount(product.getAmount());
    model.setInitialValidate(product.getInitialValidate());
    model.setFinalValidate(product.getFinalValidate());
    model.setStatus(product.getStatusEnum());
    model.setCreatedAt(product.getCreatedAt());
    model.setUpdatedAt(product.getUpdatedAt());

    return model;
  }
}
