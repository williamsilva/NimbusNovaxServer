-- Fase inicial do módulo Administração (Agentes/Produtos/Motivo Cancelamento), dados migrados do
-- sistema legado "Novax antigo" (MySQL, ver V20260820_02/03/04 - dados, não schema). Diferente de
-- usuários/grupos (que ficam no NimbusAuth), estas são entidades de negócio do próprio
-- NimbusNovax - created_by_id/updated_by_id são colunas UUID simples sem FK (usuário é externo,
-- gerenciado pelo NimbusAuth - mesmo padrão de Addendum.requestedById no NimbusFlow).

CREATE TABLE states (
  id   UUID PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  uf   VARCHAR(3)  NOT NULL UNIQUE
);

CREATE TABLE cities (
  id       UUID PRIMARY KEY,
  name     VARCHAR(50) NOT NULL,
  state_id UUID NOT NULL REFERENCES states (id)
);

CREATE INDEX cities_state_id_idx ON cities (state_id);

-- code/name/document únicos (mesmas regras do AgentService legado). type_person, civil_state e os
-- 5 status por papel (client/provider/promoter/employee/tour_guide) são enums codificados como
-- INT (0=NULL/1=ACTIVE/2=INACTIVE/3=BLOCKED pros status; ver Agent.java legado pros demais) -
-- mapeados no lado Java, não com CHECK constraint aqui (mesmo padrão de StatusUserEnum no
-- NimbusAuth).
CREATE TABLE agents (
  id                UUID PRIMARY KEY,
  code              VARCHAR(20)  NOT NULL UNIQUE,
  name              VARCHAR(50)  NOT NULL UNIQUE,
  social_reason     VARCHAR(50),
  document          VARCHAR(20)  NOT NULL UNIQUE,
  is_attendant      BOOLEAN      NOT NULL DEFAULT false,
  is_manager        BOOLEAN      NOT NULL DEFAULT false,
  status_client     INT          NOT NULL DEFAULT 0,
  status_provider   INT          NOT NULL DEFAULT 0,
  status_promoter   INT          NOT NULL DEFAULT 0,
  status_employee   INT          NOT NULL DEFAULT 0,
  status_tour_guide INT          NOT NULL DEFAULT 0,
  sex               VARCHAR(1),
  rg                VARCHAR(20),
  type_person       INT          NOT NULL DEFAULT 0,
  civil_state       INT,
  birth_date        DATE,
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ,
  created_by_id     UUID,
  updated_by_id     UUID
);

-- Papéis do agente (Client/Provider/Promoter/Employee/TourGuide) - um agente pode ter vários.
CREATE TABLE agent_types (
  id         UUID PRIMARY KEY,
  agent_id   UUID NOT NULL REFERENCES agents (id) ON DELETE CASCADE,
  type_agent INT  NOT NULL
);

CREATE INDEX agent_types_agent_id_idx ON agent_types (agent_id);

CREATE TABLE agent_addresses (
  id           UUID PRIMARY KEY,
  agent_id     UUID NOT NULL REFERENCES agents (id) ON DELETE CASCADE,
  street       VARCHAR(255),
  number       VARCHAR(255),
  complement   VARCHAR(255),
  burgh        VARCHAR(255),
  postal_code  VARCHAR(255),
  city_id      UUID REFERENCES cities (id)
);

CREATE INDEX agent_addresses_agent_id_idx ON agent_addresses (agent_id);

CREATE TABLE agent_contacts (
  id         UUID PRIMARY KEY,
  agent_id   UUID NOT NULL REFERENCES agents (id) ON DELETE CASCADE,
  name       VARCHAR(100),
  cellphone  VARCHAR(20),
  telephone  VARCHAR(20),
  email      VARCHAR(100)
);

CREATE INDEX agent_contacts_agent_id_idx ON agent_contacts (agent_id);

-- type_product: 0=NULL/1=TICKET/2=FOOD/3=COURTESY (mesmo enum do ProductService legado - amount
-- só pode ser 0 quando COURTESY, regra fica no service, não em CHECK constraint).
CREATE TABLE products (
  id                UUID PRIMARY KEY,
  name              VARCHAR(100)   NOT NULL UNIQUE,
  status            INT            NOT NULL DEFAULT 1,
  type_product      INT            NOT NULL,
  description       VARCHAR(100),
  amount            NUMERIC(10,2)  NOT NULL,
  initial_validate  DATE,
  final_validate    DATE,
  created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ,
  created_by_id     UUID,
  updated_by_id     UUID
);

-- generation: 1=USER/2=SYSTEM - sempre forçado pra USER no save pelo service (mesma regra do
-- CancellationReasonService legado); linhas SYSTEM (seed) ficam protegidas contra edição/exclusão
-- só na UI, mesmo padrão do sistema antigo.
CREATE TABLE cancellation_reasons (
  id            UUID PRIMARY KEY,
  name          VARCHAR(20)  NOT NULL UNIQUE,
  status        INT          NOT NULL DEFAULT 1,
  generation    INT          NOT NULL DEFAULT 1,
  description   VARCHAR(50),
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ,
  created_by_id UUID,
  updated_by_id UUID
);
