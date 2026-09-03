package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.dto.request.ProductRequest;
import com.nimbusnovax.administracao.dto.response.ProductOptionResponse;
import com.nimbusnovax.administracao.dto.response.ProductResponse;
import com.nimbusnovax.administracao.model.Product;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import com.nimbusnovax.administracao.repository.ProductRepository;
import com.nimbussystems.commons.security.CurrentUserProvider;
import com.nimbussystems.commons.web.SearchRequest;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Regras replicadas do {@code ProductService} do sistema legado: nome único; valor zero só é
 *  aceito na aba Courtesy (produtos de cortesia). */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

  private final ProductRepository repository;
  private final ProductSpecs productSpecs;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public ProductResponse findById(UUID id) {
    return toResponse(getOrThrow(id));
  }

  /** Filtro/ordenação/paginação reais no banco via {@link ProductSpecs} (Specification) - ver
   *  {@link com.nimbusnovax.voucher.core.VoucherSpecs} para o padrão. */
  @Transactional(readOnly = true)
  public Page<Product> search(SearchRequest request, Pageable pageable) {
    Specification<Product> spec = productSpecs.fromRequest(request);
    return repository.findAll(spec, pageable);
  }

  /** Item leve para os selects de item do formulário de Voucher (ingressos/alimentação) - só
   *  produtos ATIVOS do tipo pedido (produto inativo não deve ser oferecido para venda em um
   *  voucher novo). */
  @Transactional(readOnly = true)
  public List<ProductOptionResponse> findOptions(TypeProductEnum typeProduct) {
    return repository.findAll().stream()
        .filter(p -> p.getTypeProductEnum() == typeProduct && p.getStatusEnum() == StatusEnum.ACTIVE)
        .map(p -> new ProductOptionResponse(p.getId(), p.getName(), p.getAmount()))
        .sorted(Comparator.comparing(ProductOptionResponse::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public ProductResponse create(ProductRequest request) {
    Product product = new Product();
    applyRequest(product, request);
    UUID userId = currentUserId();
    product.setCreatedById(userId);
    product.setUpdatedById(userId);
    return toResponse(save(product));
  }

  public ProductResponse update(UUID id, ProductRequest request) {
    Product product = getOrThrow(id);
    applyRequest(product, request);
    product.setUpdatedById(currentUserId());
    return toResponse(save(product));
  }

  public void activate(UUID id) {
    Product product = getOrThrow(id);
    if (product.getStatusEnum() == StatusEnum.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is already active");
    }
    product.setStatusEnum(StatusEnum.ACTIVE);
  }

  public void deactivate(UUID id) {
    Product product = getOrThrow(id);
    if (product.getStatusEnum() == StatusEnum.INACTIVE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is already inactive");
    }
    product.setStatusEnum(StatusEnum.INACTIVE);
  }

  public void delete(UUID id) {
    Product product = getOrThrow(id);
    try {
      repository.delete(product);
      repository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a product that has links");
    }
  }

  private Product save(Product product) {
    repository.findByName(product.getName()).ifPresent(existing -> {
      if (product.getId() == null || !existing.getId().equals(product.getId())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "A product with name already exists: " + product.getName());
      }
    });

    if (product.getAmount().compareTo(BigDecimal.ZERO) == 0 && product.getTypeProductEnum() != TypeProductEnum.COURTESY) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Products with zero value must be saved as courtesy");
    }

    return repository.save(product);
  }

  private UUID currentUserId() {
    try {
      return UUID.fromString(currentUserProvider.requireUserId());
    } catch (IllegalStateException | IllegalArgumentException e) {
      return null;
    }
  }

  private Product getOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
  }

  private void applyRequest(Product product, ProductRequest request) {
    product.setName(request.name());
    product.setDescription(request.description());
    product.setTypeProductEnum(request.typeProduct());
    product.setAmount(request.amount());
    product.setInitialValidate(request.initialValidate());
    product.setFinalValidate(request.finalValidate());
    if (request.status() != null) {
      product.setStatusEnum(request.status());
    }
  }

  private ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getTypeProductEnum(),
        product.getAmount(),
        product.getInitialValidate(),
        product.getFinalValidate(),
        product.getStatusEnum(),
        product.getCreatedAt(),
        product.getUpdatedAt());
  }
}
