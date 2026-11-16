-- Área do cliente (login obrigatório para comprar) e compra de vários ingressos.
--
-- usuario.senha_hash: hash BCrypt da senha do cliente. Nulo = usuário criado no
-- fluxo antigo de compra sem login, que ainda não ativou a conta.
alter table usuario add column senha_hash varchar(100);

-- ingresso.nome_participante: quem vai usar o ingresso (um nome por ingresso).
-- Nulo em ingressos disponíveis e nos vendidos antes desta migração.
alter table ingresso add column nome_participante varchar(255);

-- "Meus pedidos": pedidos do cliente, mais recentes primeiro.
create index ix_pedido_usuario_data on pedido (usuario_id, data_pedido desc);

-- pedido.lote_id / quantidade_ingressos: o pedido passa a guardar de qual lote é
-- e quantos ingressos tem. Necessário porque, quando a reserva expira ou o
-- pagamento é recusado, os ingressos voltam ao estoque e perdem o vínculo com o
-- pedido; sem isso, "Meus pedidos" não saberia de que evento era.
alter table pedido add column lote_id bigint references lote (id) on delete set null;
alter table pedido add column quantidade_ingressos integer not null default 1;
alter table pedido add constraint ck_pedido_quantidade check (quantidade_ingressos > 0);

-- Pedidos antigos (RN-1 era 1 ingresso por pedido): recupera o lote pelo
-- ingresso ainda vinculado, quando houver.
update pedido p
   set lote_id = (select min(i.lote_id) from ingresso i where i.pedido_id = p.id)
 where p.lote_id is null;
