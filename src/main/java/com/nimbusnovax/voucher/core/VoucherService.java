package com.nimbusnovax.voucher.core;

import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.Product;
import com.nimbusnovax.administracao.repository.AgentRepository;
import com.nimbusnovax.administracao.repository.ProductRepository;
import com.nimbussystems.commons.security.CurrentUserProvider;
import com.nimbussystems.commons.web.SearchRequest;
import com.nimbusnovax.voucher.dto.request.VoucherRequest;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import com.nimbusnovax.voucher.model.ConfigVoucher;
import com.nimbusnovax.voucher.model.Food;
import com.nimbusnovax.voucher.model.Ticket;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.model.VoucherItem;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import com.nimbusnovax.voucher.repository.VoucherRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regras de negócio replicadas do {@code VoucherService}/{@code Voucher} do sistema legado (Novax
 * antigo): código gerado no servidor (3 letras aleatórias + maior sufixo numérico já usado + 1,
 * nunca escolhido pelo usuário - ver {@link #generateCode}), limite de vouchers pendentes por
 * cliente (DEALING/OVERDUE/CONFIRMED) contra {@link ConfigVoucher#getNumberPendingVouchers()},
 * contagem de visitas (nº de vouchers já EXCHANGED do cliente + 1), itens (tickets/foods)
 * substituídos por inteiro a cada save. Duas divergências deliberadas do legado, ambas para sanar
 * comportamento acidental de lá (não regra de negócio intencional): (1) o PUT genérico nunca altera
 * status - só os endpoints de fluxo em {@link VoucherFlowService} fazem isso; (2) {@code
 * typePerson} é sempre copiado do cliente atual, nunca escolhido livremente no formulário.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private final VoucherRepository repository;
  private final VoucherSpecs voucherSpecs;
  private final AgentRepository agentRepository;
  private final ProductRepository productRepository;
  private final ConfigVoucherService configVoucherService;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public VoucherResponse findById(UUID id) {
    return toResponse(getOrThrow(id));
  }

  /** Filtro/ordenação/paginação reais no banco via {@link VoucherSpecs} (Specification) - ver
   *  ali o motivo de continuar reaproveitando {@code FilterSupport} pra extrair os valores em vez
   *  de introduzir um DTO de filtro tipado novo. */
  @Transactional(readOnly = true)
  public Page<Voucher> search(SearchRequest request, Pageable pageable) {
    Specification<Voucher> spec = voucherSpecs.fromRequest(request);
    return repository.findAll(spec, pageable);
  }

  public VoucherResponse create(VoucherRequest request) {
    Agent client = findAgentOrThrow(request.clientId());
    Agent promoter = findAgentOrThrow(request.promoterId());
    Agent tourGuide = request.tourGuideId() == null ? null : findAgentOrThrow(request.tourGuideId());

    ConfigVoucher config = configVoucherService.getOrCreate();
    long pending = repository.countByClientAndStatusIn(client.getId(), List.of(
        StatusVoucherEnum.OVERDUE.getCode(), StatusVoucherEnum.DEALING.getCode(),
        StatusVoucherEnum.CONFIRMED.getCode()));

    if (pending >= config.getNumberPendingVouchers()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
          "O cliente '%s' já possui %d vouchers pendentes - não é possível cadastrar um novo voucher.",
          client.getName(), pending));
    }

    Voucher voucher = new Voucher();
    voucher.setClient(client);
    voucher.setPromoter(promoter);
    voucher.setTourGuide(tourGuide);
    voucher.setTypePersonEnum(client.getTypePersonEnum());
    voucher.setStatusEnum(StatusVoucherEnum.DEALING);
    voucher.setCode(generateCode());
    voucher.setNumberOfVisit((int) repository.countByClientAndStatus(
        client.getId(), StatusVoucherEnum.EXCHANGED.getCode()) + 1);

    applyRequest(voucher, request);
    UUID userId = currentUserId();
    voucher.setCreatedById(userId);
    voucher.setUpdatedById(userId);

    return toResponse(repository.save(voucher));
  }

  public VoucherResponse update(UUID id, VoucherRequest request) {
    Voucher voucher = getOrThrow(id);

    if (!voucher.canBeModified()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Voucher com status " + voucher.getStatusEnum() + " não pode ser alterado.");
    }

    Agent client = findAgentOrThrow(request.clientId());

    voucher.setClient(client);
    voucher.setPromoter(findAgentOrThrow(request.promoterId()));
    voucher.setTourGuide(request.tourGuideId() == null ? null : findAgentOrThrow(request.tourGuideId()));
    voucher.setTypePersonEnum(client.getTypePersonEnum());

    applyRequest(voucher, request);
    voucher.setUpdatedById(currentUserId());

    return toResponse(repository.save(voucher));
  }

  /** Delete físico só permitido em DEALING - mesma regra do sistema legado
   *  (VoucherConsultComponent.canCancel só habilitava o botão nesse status). */
  public void delete(UUID id) {
    Voucher voucher = getOrThrow(id);

    if (voucher.getStatusEnum() != StatusVoucherEnum.DEALING) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Voucher com status " + voucher.getStatusEnum() + " não pode ser excluído.");
    }

    try {
      repository.delete(voucher);
      repository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível excluir um voucher que possui vínculos.");
    }
  }

  /** Usado por {@link VoucherFlowService} - sem checagem de permissão própria, cada endpoint de
   *  fluxo já checa a permissão que lhe cabe antes de chamar isto. */
  Voucher getOrThrowInternal(UUID id) {
    return getOrThrow(id);
  }

  private void applyRequest(Voucher voucher, VoucherRequest request) {
    voucher.setNote(request.note());
    voucher.setVisitDate(request.visitDate());
    voucher.setAdvanceValue(request.advanceValue());

    replaceItems(voucher.getTickets(), request.tickets(), Ticket::new, voucher);
    replaceItems(voucher.getFoods(), request.foods(), Food::new, voucher);
    voucher.calculateTotalPrice();
  }

  private <T extends VoucherItem> void replaceItems(
      List<T> target, List<VoucherRequest.ItemRequest> source, Supplier<T> factory, Voucher voucher) {
    target.clear();
    if (source == null) {
      return;
    }
    for (VoucherRequest.ItemRequest itemRequest : source) {
      Product product = productRepository.findById(itemRequest.productId())
          .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Product not found: " + itemRequest.productId()));

      T item = factory.get();
      item.setVoucher(voucher);
      item.setProduct(product);
      item.setQuantity(itemRequest.quantity());
      item.setUnitPrice(itemRequest.unitPrice() != null ? itemRequest.unitPrice() : product.getAmount());
      target.add(item);
    }
  }

  /** 3 letras aleatórias maiúsculas + (maior sufixo numérico já usado + 1) - mesmo esquema do
   *  legado ({@code RandomStringUtils.randomAlphabetic(3)} + {@code maxId + 1}), ex.: "DWP1926". */
  private String generateCode() {
    StringBuilder prefix = new StringBuilder(3);
    for (int i = 0; i < 3; i++) {
      prefix.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
    }
    Integer max = repository.findMaxNumericSuffix();
    long suffix = (max == null ? 1000L : max) + 1;
    return prefix + String.valueOf(suffix);
  }

  private Agent findAgentOrThrow(UUID id) {
    return agentRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found: " + id));
  }

  private UUID currentUserId() {
    try {
      return UUID.fromString(currentUserProvider.requireUserId());
    } catch (IllegalStateException | IllegalArgumentException e) {
      return null;
    }
  }

  private Voucher getOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found: " + id));
  }

  VoucherResponse toResponse(Voucher voucher) {
    List<VoucherResponse.ItemResponse> tickets = voucher.getTickets().stream().map(this::toItemResponse).toList();
    List<VoucherResponse.ItemResponse> foods = voucher.getFoods().stream().map(this::toItemResponse).toList();

    return new VoucherResponse(
        voucher.getId(),
        voucher.getCode(),
        voucher.getStatusEnum(),
        voucher.getTypePersonEnum(),
        voucher.getNote(),
        voucher.getVisitDate(),
        voucher.getNumberOfVisit(),
        voucher.getTotalPrice(),
        voucher.getAdvanceValue(),
        voucher.getTotalPriceTickets(),
        voucher.getTotalPriceFoods(),
        voucher.getConfirmationDate(),
        voucher.getCancellationDate(),
        toAgentRef(voucher.getClient()),
        toAgentRef(voucher.getPromoter()),
        toAgentRef(voucher.getTourGuide()),
        voucher.getCancellationReason() == null ? null
            : new VoucherResponse.CancellationReasonRefResponse(
                voucher.getCancellationReason().getId(), voucher.getCancellationReason().getName()),
        tickets,
        foods,
        voucher.getCreatedAt(),
        voucher.getUpdatedAt());
  }

  private VoucherResponse.AgentRefResponse toAgentRef(Agent agent) {
    return agent == null ? null : new VoucherResponse.AgentRefResponse(agent.getId(), agent.getName(), agent.getDocument());
  }

  private VoucherResponse.ItemResponse toItemResponse(VoucherItem item) {
    return new VoucherResponse.ItemResponse(
        item.getId(),
        item.getProduct() != null ? item.getProduct().getId() : null,
        item.getProduct() != null ? item.getProduct().getName() : null,
        item.getQuantity(),
        item.getUnitPrice(),
        item.getTotalPrice());
  }
}
