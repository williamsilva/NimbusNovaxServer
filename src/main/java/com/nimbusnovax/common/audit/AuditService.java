package com.nimbusnovax.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusnovax.common.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grava o registro de auditoria (ver AuditLog). Nunca lança exceção pro chamador - uma falha ao
 * auditar (serialização, banco fora do ar) não pode derrubar a ação de negócio que está sendo
 * auditada; só loga um warning e segue. Roda em REQUIRES_NEW pelo mesmo motivo: se a transação
 * principal for revertida por outro erro depois da ação sensível já ter ocorrido, o log de
 * auditoria da tentativa não deve ser revertido junto.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditLogRepository repository;
  private final CurrentUserProvider currentUserProvider;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(String entityName, String entityId, String action, Object before, Object after) {
    try {
      AuditLog entry = AuditLog.builder()
          .entityName(entityName)
          .entityId(entityId)
          .action(action)
          .userId(currentUserProvider.getCurrentUser().userId())
          .dataBefore(serialize(before))
          .dataAfter(serialize(after))
          .build();
      repository.save(entry);
    } catch (Exception e) {
      log.warn("Failed to write audit log for {}.{} (entityId={})", entityName, action, entityId, e);
    }
  }

  private String serialize(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      log.warn("Failed to serialize audit payload for {}", value.getClass(), e);
      return null;
    }
  }
}
