package com.nimbusnovax.common.backup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** API interna machine-to-machine (rota /internal/backup/**, ver InternalBackupSecretFilter) -
 *  consumida pelo NimbusAuth pra compor o backup centralizado do ecossistema Nimbus. Espelha o
 *  InternalBackupController já existente no NimbusAuthServer (que este próprio app já consome
 *  hoje via NimbusAuthInternalClient) - agora na direção inversa.
 *
 * <p>Só expõe /database, sem /files: este app não tem storage externo (S3/R2) nem volume de
 * disco - o único arquivo binário do domínio (logo da empresa, ver CompanySettingsEntity) fica
 * como BYTEA na própria tabela, já coberto pelo dump do banco. Ver investigação registrada antes
 * desta implementação. */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InternalBackupController {

  private final PgDumpRunner pgDumpRunner;

  @GetMapping("/internal/backup/database")
  public ResponseEntity<byte[]> database() {
    log.info("Backup do banco NimbusNovax solicitado via API interna.");
    byte[] dump = pgDumpRunner.dump();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(dump);
  }
}
