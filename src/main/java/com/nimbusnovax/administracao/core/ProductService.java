package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.dto.request.ProductRequest;
import com.nimbusnovax.administracao.dto.response.ProductResponse;
import com.nimbusnovax.administracao.model.Product;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import com.nimbusnovax.administracao.repository.ProductRepository;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.web.FilterSupport;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public ProductResponse findById(UUID id) {
    requireAuthority("PRODUTOS_CONSULT");
    return toResponse(getOrThrow(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<ProductResponse> search(SearchRequest request) {
    requireAuthority("PRODUTOS_CONSULT");
    List<ProductResponse> all = repository.findAll().stream().map(this::toResponse).toList();
    List<ProductResponse> sorted = sortProducts(filterProducts(all, request), request);

    int page = request.page() == null ? 0 : Math.max(0, request.page());
    int size = request.size() == null || request.size() <= 0 ? 20 : request.size();
    int from = Math.min(page * size, sorted.size());
    int to = Math.min(from + size, sorted.size());

    return PageResponse.of(sorted.subList(from, to), page, size, sorted.size());
  }

  private List<ProductResponse> filterProducts(List<ProductResponse> items, SearchRequest request) {
    Map<String, Object> tableFilters = request.tableFilters();
    Map<String, Object> advanced = request.advanced();

    String name = FilterSupport.textFilter(tableFilters, advanced, "name", "name");
    List<String> typeValues = FilterSupport.listFilter(tableFilters, advanced, "typeProduct", "typeProduct");
    List<String> statusValues = FilterSupport.listFilter(tableFilters, advanced, "status", "status");
    String global = request.globalFilter();

    return items.stream()
        .filter(p -> FilterSupport.containsIgnoreCase(p.name(), name))
        .filter(p -> typeValues.isEmpty() || typeValues.contains(p.typeProduct().name()))
        .filter(p -> statusValues.isEmpty() || (p.status() != null && statusValues.contains(p.status().name())))
        .filter(p -> global == null || global.isBlank() || FilterSupport.containsIgnoreCase(p.name(), global))
        .toList();
  }

  private List<ProductResponse> sortProducts(List<ProductResponse> items, SearchRequest request) {
    SearchRequest.SortItem sortItem =
        (request.sort() == null || request.sort().isEmpty()) ? null : request.sort().get(0);
    String field = sortItem != null && sortItem.field() != null ? sortItem.field() : "name";
    boolean desc = sortItem != null && sortItem.order() != null && sortItem.order() < 0;

    Comparator<ProductResponse> comparator = switch (field) {
      case "amount" -> Comparator.comparing(ProductResponse::amount);
      case "createdAt" -> Comparator.comparing(p -> orEpoch(p.createdAt()));
      default -> Comparator.comparing(p -> orEmpty(p.name()), String.CASE_INSENSITIVE_ORDER);
    };

    if (desc) {
      comparator = comparator.reversed();
    }

    return items.stream().sorted(comparator).toList();
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  private static Instant orEpoch(Instant value) {
    return value == null ? Instant.EPOCH : value;
  }

  public ProductResponse create(ProductRequest request) {
    requireAuthority("PRODUTOS_CREATE");
    Product product = new Product();
    applyRequest(product, request);
    UUID userId = currentUserId();
    product.setCreatedById(userId);
    product.setUpdatedById(userId);
    return toResponse(save(product));
  }

  public ProductResponse update(UUID id, ProductRequest request) {
    requireAuthority("PRODUTOS_CHANGE");
    Product product = getOrThrow(id);
    applyRequest(product, request);
    product.setUpdatedById(currentUserId());
    return toResponse(save(product));
  }

  public void activate(UUID id) {
    requireAuthority("PRODUTOS_CHANGE");
    Product product = getOrThrow(id);
    if (product.getStatusEnum() == StatusEnum.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is already active");
    }
    product.setStatusEnum(StatusEnum.ACTIVE);
  }

  public void deactivate(UUID id) {
    requireAuthority("PRODUTOS_CHANGE");
    Product product = getOrThrow(id);
    if (product.getStatusEnum() == StatusEnum.INACTIVE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Product is already inactive");
    }
    product.setStatusEnum(StatusEnum.INACTIVE);
  }

  public void delete(UUID id) {
    requireAuthority("PRODUTOS_DELETE");
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

  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
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
