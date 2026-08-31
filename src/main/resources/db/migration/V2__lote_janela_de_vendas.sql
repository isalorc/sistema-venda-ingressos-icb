-- Virada de lote (docs/viradaDeLote.md): janela de vendas opcional por lote.
-- Ambas as colunas nulas (default) = comportamento anterior — o lote entra na
-- fila só pela ordem de criação e pelo estoque. Migration aditiva, sem backfill.

alter table lote add column inicio_vendas timestamp(6);
alter table lote add column fim_vendas    timestamp(6);
