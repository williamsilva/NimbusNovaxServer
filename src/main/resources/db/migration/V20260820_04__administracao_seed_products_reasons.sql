-- Produtos e motivos de cancelamento migrados do MySQL legado (ws_product/
-- ws_cancellation_reason).

INSERT INTO products (id, name, status, type_product, description, amount, initial_validate, final_validate,
  created_at, updated_at, created_by_id, updated_by_id) VALUES
  ('a841a517-0614-ebc5-6e22-6f8f2ed1fdfb', 'Grupo Excursão Integral', 2, 1, 'Para grupos acima de 20 pessoas', 90.00, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', TIMESTAMP '2026-04-11 19:10:52', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'),
  ('bf0d5ffc-d152-6461-1dc7-3a4512114b5e', 'Grupo Excursão R$ 120,00', 1, 1, 'Para grupos acima de 20 pessoas', 120.00, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', TIMESTAMP '2026-04-11 19:11:05', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'),
  ('ee004eb1-bd56-c8f2-00eb-77ca8b6dfa9b', 'Cortesia Grupo', 1, 3, 'Cortesia para cada 20 pagantes', 0.00, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', NULL, '00000000-0000-0000-0000-000000000001', NULL),
  ('eb90d2c9-c5e8-0958-7dd9-01ea17979283', 'Cortesia Motorista', 1, 3, 'Cortesia para o motorista do grupo', 0.00, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', NULL, '00000000-0000-0000-0000-000000000001', NULL),
  ('534df1d0-ae30-ec0b-a890-ef24d483d07f', 'Cortesia Guia', 1, 3, 'Cortesia para o guia da excursão', 0.00, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', NULL, '00000000-0000-0000-0000-000000000001', NULL),
  ('b373d568-0b39-3d2f-6212-2fcb7e00f512', 'Almoço excursão', 1, 2, 'Almoço para grupo excursão', 34.90, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', TIMESTAMP '2022-11-04 12:32:09', '00000000-0000-0000-0000-000000000001', NULL),
  ('d6fffe0c-2b15-4ea7-74ff-ee707a08710e', 'Self Service excursão', 1, 2, 'Self Service para grupo excursão', 39.90, NULL, NULL, TIMESTAMP '2020-10-25 17:55:22', TIMESTAMP '2023-08-31 18:59:52', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002'),
  ('0463d7b7-5523-6625-87d4-8e9a4c803966', 'Grupo Excursão R$ 100,00', 1, 1, 'Grupo Valor Unico', 100.00, NULL, NULL, TIMESTAMP '2020-10-30 16:42:55', TIMESTAMP '2024-12-11 15:44:14', '21b1d414-e51a-409e-aaaa-2f4a9d6d8d87', '00000000-0000-0000-0000-000000000002'),
  ('0c973f81-3299-84d8-c281-3ab27479c347', 'Ingresso Promocional', 2, 1, 'Ingresso Promocional', 60.00, NULL, NULL, TIMESTAMP '2023-05-31 16:38:14', TIMESTAMP '2026-04-11 19:10:43', '771e2aa6-e201-4400-9f80-75dc1bfd9829', '00000000-0000-0000-0000-000000000002'),
  ('0fa4213a-bcfe-7851-5c1e-c63830fde62c', 'Grupo Excursão Meia', 2, 1, 'Grupo Excursão Meia', 45.00, NULL, NULL, TIMESTAMP '2023-05-31 16:39:01', TIMESTAMP '2026-04-11 19:10:36', '771e2aa6-e201-4400-9f80-75dc1bfd9829', '00000000-0000-0000-0000-000000000002'),
  ('011bee54-56cb-31d2-7e79-2850732a579a', 'Ingresso Escolar', 2, 1, 'Ingresso Escolar', 70.00, NULL, NULL, TIMESTAMP '2023-08-31 19:00:33', TIMESTAMP '2026-04-11 19:10:41', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
  ('42a4cc4c-0395-fcf1-5792-5ca68b3a5092', 'Grupo Excursão R$ 80,00', 2, 1, 'Grupo Excursão R$ 80,00', 80.00, NULL, NULL, TIMESTAMP '2024-12-11 15:44:40', TIMESTAMP '2026-04-11 19:10:50', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002');

INSERT INTO cancellation_reasons (id, name, status, generation, description, created_at, updated_at, created_by_id, updated_by_id) VALUES
  ('a71fc0b7-3049-526e-48a5-eef819fafcb0', 'System', 1, 2, 'Cancelado Automaticamente pelo sistema', TIMESTAMP '2023-11-01 00:55:31', TIMESTAMP '2023-11-01 00:55:31', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
  ('18e515da-3913-fe7a-0265-a1b8c0bbfcc5', 'Chuva', 1, 1, 'Previsão de chuva no dia da visita', TIMESTAMP '2023-11-01 00:58:07', TIMESTAMP '2023-11-01 00:58:07', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
  ('6aca6183-1aee-d207-7f0e-63087c45ab0a', 'Promoção', 1, 1, 'Comprou na promoção', TIMESTAMP '2024-03-07 15:02:51', TIMESTAMP '2024-03-07 15:02:51', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002');
