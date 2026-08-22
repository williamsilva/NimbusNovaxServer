-- Dados da empresa emissora do voucher (nome/CNPJ/endereço/telefone) - linha única (mesmo padrão
-- de email_settings), usados no cabeçalho do e-mail e do PDF enviados ao cliente. Configurável em
-- Configurações > Empresa (ver CompanySettingsEntity/Service/BffCompanySettingsController).

CREATE TABLE company_settings (
  id            UUID         PRIMARY KEY,
  name          VARCHAR(255),
  document      VARCHAR(20),
  address_line  VARCHAR(255),
  city          VARCHAR(100),
  state         VARCHAR(2),
  postal_code   VARCHAR(20),
  phone         VARCHAR(20),
  email         VARCHAR(255),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by_id UUID,
  updated_by_id UUID
);

-- Seed com os dados reais da empresa (mesmos do voucher de referência do sistema legado) - evita
-- o cabeçalho do primeiro e-mail/PDF sair em branco antes de alguém preencher a tela manualmente;
-- pode ser editado a qualquer momento em Configurações > Empresa.
INSERT INTO company_settings (id, name, document, address_line, city, state, postal_code, phone)
VALUES (
  gen_random_uuid(),
  'ACQUAMANIA MÚLTIPLO LAZER S.A',
  '39.303.847/0001-80',
  'Rua das Acácias, n° S/N - Comunidade Urbana de Lagoa Dourada',
  'Guarapari',
  'ES',
  '29226-766',
  '(27) 3221-6666'
);
