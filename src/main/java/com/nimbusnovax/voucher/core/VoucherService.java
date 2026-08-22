package com.nimbusnovax.voucher.core;

import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.Product;
import com.nimbusnovax.administracao.repository.AgentRepository;
import com.nimbusnovax.administracao.repository.ProductRepository;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.web.FilterSupport;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
 * typePerson} é sempre copiado do cliente atual, nunca escolhido livremente no formulário. Regra
 * nova (sem equivalente no legado): um cliente não pode ter dois vouchers DEALING simultâneos -
 * ver {@link #requireNoOtherDealingVoucher}.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  /** Status escondidos da listagem por padrão (voucher já "resolvido") - a tela só os mostra
   *  quando o usuário aplica um filtro de status explícito, mesmo comportamento do sistema
   *  legado (VoucherConsultComponent nunca aplicava esse filtro; era a query padrão do backend). */
  private static final Set<Integer> HIDDEN_BY_DEFAULT = Set.of(
      StatusVoucherEnum.EXCHANGED.getCode(), StatusVoucherEnum.CALLED_OFF.getCode(),
      StatusVoucherEnum.NOT_CLOSED.getCode());

  private final VoucherRepository repository;
  private final AgentRepository agentRepository;
  private final ProductRepository productRepository;
  private final ConfigVoucherService configVoucherService;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public VoucherResponse findById(UUID id) {
    requireAuthority("VOUCHERS_CONSULT");
    return toResponse(getOrThrow(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<VoucherResponse> search(SearchRequest request) {
    requireAuthority("VOUCHERS_CONSULT");
    List<VoucherResponse> all = repository.findAll().stream().map(this::toResponse).toList();
    List<VoucherResponse> sorted = sortVouchers(filterVouchers(all, request), request);

    int page = request.page() == null ? 0 : Math.max(0, request.page());
    int size = request.size() == null || request.size() <= 0 ? 20 : request.size();
    int from = Math.min(page * size, sorted.size());
    int to = Math.min(from + size, sorted.size());

    return PageResponse.of(sorted.subList(from, to), page, size, sorted.size());
  }

  private List<VoucherResponse> filterVouchers(List<VoucherResponse> items, SearchRequest request) {
    Map<String, Object> tableFilters = request.tableFilters();
    Map<String, Object> advanced = request.advanced();

    String code = FilterSupport.textFilter(tableFilters, advanced, "voucher", "voucher");
    String clientName = FilterSupport.textFilter(tableFilters, advanced, "client", "client");
    List<String> promoterIds = FilterSupport.listFilter(tableFilters, advanced, "promoter", "promoter");
    List<String> statusValues = FilterSupport.listFilter(tableFilters, advanced, "status", "status");
    LocalDate[] visitRange = FilterSupport.periodLocalDateRange(
        tableFilters, advanced, "visitDate", "periodVisitDate", "visitDate");
    String global = request.globalFilter();

    boolean hasStatusFilter = !statusValues.isEmpty();

    return items.stream()
        .filter(v -> FilterSupport.containsIgnoreCase(v.code(), code))
        .filter(v -> v.client() == null || FilterSupport.containsIgnoreCase(v.client().name(), clientName))
        .filter(v -> promoterIds.isEmpty()
            || (v.promoter() != null && promoterIds.contains(String.valueOf(v.promoter().id()))))
        .filter(v -> hasStatusFilter
            ? statusValues.contains(v.status().name())
            : !HIDDEN_BY_DEFAULT.contains(StatusVoucherEnum.toCode(v.status())))
        .filter(v -> FilterSupport.withinRange(v.visitDate(), visitRange))
        .filter(v -> global == null || global.isBlank()
            || FilterSupport.containsIgnoreCase(v.code(), global)
            || (v.client() != null && FilterSupport.containsIgnoreCase(v.client().name(), global)))
        .toList();
  }

  private List<VoucherResponse> sortVouchers(List<VoucherResponse> items, SearchRequest request) {
    SearchRequest.SortItem sortItem =
        (request.sort() == null || request.sort().isEmpty()) ? null : request.sort().get(0);
    String field = sortItem != null && sortItem.field() != null ? sortItem.field() : "createdAt";
    boolean desc = sortItem == null || sortItem.order() == null || sortItem.order() < 0;

    Comparator<VoucherResponse> comparator = switch (field) {
      case "voucher", "code" -> Comparator.comparing(v -> orEmpty(v.code()), String.CASE_INSENSITIVE_ORDER);
      case "visitDate" -> Comparator.comparing(v -> v.visitDate() == null ? LocalDate.MIN : v.visitDate());
      case "totalPrice" -> Comparator.comparing(VoucherResponse::totalPrice);
      default -> Comparator.comparing(v -> orEpoch(v.createdAt()));
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

  public VoucherResponse create(VoucherRequest request) {
    requireAuthority("VOUCHERS_CREATE");

    Agent client = findAgentOrThrow(request.clientId());
    Agent promoter = findAgentOrThrow(request.promoterId());
    Agent tourGuide = request.tourGuideId() == null ? null : findAgentOrThrow(request.tourGuideId());

    requireNoOtherDealingVoucher(client, null);

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
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);

    if (!voucher.canBeModified()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Voucher com status " + voucher.getStatusEnum() + " não pode ser alterado.");
    }

    Agent client = findAgentOrThrow(request.clientId());

    if (voucher.getStatusEnum() == StatusVoucherEnum.DEALING) {
      requireNoOtherDealingVoucher(client, voucher.getId());
    }

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
    requireAuthority("VOUCHERS_DELETE");
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

  /** Um cliente não pode ter dois vouchers em negociação (DEALING) ao mesmo tempo - regra própria
   *  além do limite geral de "vouchers pendentes" (ConfigVoucher.numberPendingVouchers, que conta
   *  DEALING+OVERDUE+CONFIRMED e é bem mais permissivo). {@code excludeId} é o próprio voucher
   *  sendo editado (null na criação), pra não se autobloquear. */
  private void requireNoOtherDealingVoucher(Agent client, UUID excludeId) {
    long dealing = excludeId == null
        ? repository.countByClientAndStatus(client.getId(), StatusVoucherEnum.DEALING.getCode())
        : repository.countByClientAndStatusExcludingId(client.getId(), StatusVoucherEnum.DEALING.getCode(), excludeId);

    if (dealing > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
          "O cliente '%s' já possui um voucher em negociação.", client.getName()));
    }
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

  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
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
