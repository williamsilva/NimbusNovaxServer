package com.nimbusnovax.voucher.repository;

import com.nimbusnovax.voucher.model.ConfigVoucher;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigVoucherRepository extends JpaRepository<ConfigVoucher, UUID> {

  Optional<ConfigVoucher> findByKey(String key);
}
