-- Gestão de eventos (docs/gestaoDeEventos.md): período (data de fim), imagem e
-- cancelamento do evento. Colunas aditivas e nullable — o `data_hora` existente
-- passa a significar o início. Sem backfill.

alter table evento add column data_fim     timestamp(6);
alter table evento add column imagem_url   varchar(2048);
alter table evento add column cancelado_em timestamp(6);
