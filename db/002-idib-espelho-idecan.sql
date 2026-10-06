/*
 * Espelha no IDIB_v2 a estrutura e os domínios do módulo de fases do IDECAN_v2.
 *
 * Rodar SOMENTE no IDIB_v2, depois de 001-fases-e-etapas.sql ter rodado no IDECAN_v2.
 * Referência: IDECAN_v2 (tabelas geradas a partir do próprio banco; domínios com os mesmos ids).
 *
 * - Recria fases, etapa e dom_tipo_etapa no formato do IDECAN (tipos, nulos, padrões, índices, FKs, CHECKs).
 * - Cria as tabelas do domínio de fases que faltavam: dom_status_*, dom_contexto_documento,
 *   tipo_documento, candidato_fases, candidato_etapa, etapa_documento, etapa_recurso,
 *   etapa_recurso_documento, etapa_tipo_documento.
 * - Copia os valores de domínio com os mesmos ids do IDECAN.
 *
 * Travas: não faz nada se o IDIB já estiver espelhado; aborta se fases/etapa/publicações tiverem linhas.
 * Tudo numa transação: qualquer erro desfaz o script inteiro.
 */
SET XACT_ABORT ON;
GO
IF OBJECT_ID('dbo.dom_status_fase', 'U') IS NOT NULL
BEGIN
    PRINT 'IDIB já espelhado com o IDECAN: nada a fazer.';
    SET NOEXEC ON;
END;
GO
IF DB_NAME() <> 'IDIB_v2'
   OR EXISTS (SELECT 1 FROM dbo.fases) OR EXISTS (SELECT 1 FROM dbo.etapa)
   OR EXISTS (SELECT 1 FROM dbo.dom_tipo_etapa) OR EXISTS (SELECT 1 FROM dbo.dom_tipo_publicacao)
   OR EXISTS (SELECT 1 FROM dbo.dom_tipo_etapa_publicacao) OR EXISTS (SELECT 1 FROM dbo.publicacao)
   OR EXISTS (SELECT 1 FROM dbo.fase_grupo_cargos) OR OBJECT_ID('dbo.tipo_documento', 'U') IS NOT NULL
BEGIN
    RAISERROR('Abortado: banco não é IDIB_v2 ou as tabelas de fases já têm dados.', 16, 1);
    SET NOEXEC ON;
END;
GO
BEGIN TRANSACTION;
GO
ALTER TABLE dbo.dom_tipo_etapa_publicacao DROP CONSTRAINT FK_dom_tipo_etapa_publicacao_tipo_etapa;
ALTER TABLE dbo.fase_grupo_cargos DROP CONSTRAINT FK_fase_grupo_cargos_fase;
ALTER TABLE dbo.publicacao DROP CONSTRAINT FK_publicacao_fase;
ALTER TABLE dbo.publicacao DROP CONSTRAINT FK_publicacao_etapa;
DROP TABLE dbo.etapa;
DROP TABLE dbo.fases;
DROP TABLE dbo.dom_tipo_etapa;
GO
/* ---------- tipo_documento ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tipo_documento](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[nome] [nvarchar](150) NOT NULL,
	[descricao] [nvarchar](max) NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
 CONSTRAINT [PK_tipo_documento] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
ALTER TABLE [dbo].[tipo_documento] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
/* ---------- dom_status_fase ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_status_fase](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](30) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_status_fase] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_status_fase] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_status_etapa ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_status_etapa](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](30) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_status_etapa] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_status_etapa] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_status_candidato_etapa ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_status_candidato_etapa](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](30) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_status_cand_eta] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_status_cand_eta] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_status_candidato_fase ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_status_candidato_fase](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](30) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_status_cand_fas] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_status_cand_fas] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_status_documento ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_status_documento](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](20) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_status_doc] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_status_doc] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_status_recurso ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_status_recurso](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](15) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_status_recurso] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_status_recurso] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_contexto_documento ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_contexto_documento](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](25) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_contexto_doc] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_contexto_doc] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- dom_tipo_etapa ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[dom_tipo_etapa](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[valor] [varchar](30) NOT NULL,
	[descricao] [nvarchar](200) NULL,
 CONSTRAINT [PK_dom_tipo_etapa] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_dom_tipo_etapa] UNIQUE NONCLUSTERED 
(
	[valor] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
/* ---------- fases ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[fases](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[edital_id] [int] NOT NULL,
	[nome] [nvarchar](150) NOT NULL,
	[descricao] [nvarchar](max) NULL,
	[ordem] [smallint] NOT NULL,
	[status_id] [int] NOT NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
	[updated_at] [datetimeoffset](7) NOT NULL,
	[data_abertura] [datetime] NULL,
	[data_fechamento] [datetime] NULL,
 CONSTRAINT [PK_fases] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_fases_edital_ordem] UNIQUE NONCLUSTERED 
(
	[edital_id] ASC,
	[ordem] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
CREATE NONCLUSTERED INDEX [idx_fase_edital] ON [dbo].[fases]
(
	[edital_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [idx_fase_status] ON [dbo].[fases]
(
	[status_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
ALTER TABLE [dbo].[fases] ADD  CONSTRAINT [DF_fases_status_id]  DEFAULT ((1)) FOR [status_id]
GO
ALTER TABLE [dbo].[fases] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
ALTER TABLE [dbo].[fases] ADD  DEFAULT (sysdatetimeoffset()) FOR [updated_at]
GO
ALTER TABLE [dbo].[fases]  WITH CHECK ADD  CONSTRAINT [FK_fases_edital] FOREIGN KEY([edital_id])
REFERENCES [dbo].[Edital] ([ediId])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[fases] CHECK CONSTRAINT [FK_fases_edital]
GO
ALTER TABLE [dbo].[fases]  WITH CHECK ADD  CONSTRAINT [FK_fases_status] FOREIGN KEY([status_id])
REFERENCES [dbo].[dom_status_fase] ([id])
GO
ALTER TABLE [dbo].[fases] CHECK CONSTRAINT [FK_fases_status]
GO
/* ---------- etapa ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[etapa](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[fases_id] [int] NOT NULL,
	[nome] [nvarchar](150) NOT NULL,
	[tipo_id] [int] NOT NULL,
	[descricao] [nvarchar](max) NULL,
	[data_abertura] [datetimeoffset](7) NOT NULL,
	[data_fechamento] [datetimeoffset](7) NULL,
	[status_id] [int] NOT NULL,
	[reprovavel] [bit] NOT NULL,
	[obrigatoria_para_fases] [bit] NOT NULL,
	[todos_candidatos] [bit] NOT NULL,
	[elegibilidade_etapa_id] [int] NULL,
	[elegibilidade_status_id] [int] NULL,
	[aceita_documentos] [bit] NOT NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
	[updated_at] [datetimeoffset](7) NOT NULL,
	[publicada_em] [datetime] NULL,
	[preliminar] [bit] NULL,
	[aceita_recurso] [bit] NULL,
	[responsavel_id] [int] NULL,
 CONSTRAINT [PK_etapa] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
CREATE NONCLUSTERED INDEX [idx_etapa_abertura] ON [dbo].[etapa]
(
	[data_abertura] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [idx_etapa_fase] ON [dbo].[etapa]
(
	[fases_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [idx_etapa_fechamento] ON [dbo].[etapa]
(
	[data_fechamento] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [idx_etapa_status] ON [dbo].[etapa]
(
	[status_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [idx_etapa_tipo] ON [dbo].[etapa]
(
	[tipo_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT ((1)) FOR [status_id]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT ((0)) FOR [reprovavel]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT ((1)) FOR [obrigatoria_para_fases]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT ((1)) FOR [todos_candidatos]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT ((0)) FOR [aceita_documentos]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
ALTER TABLE [dbo].[etapa] ADD  DEFAULT (sysdatetimeoffset()) FOR [updated_at]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [FK_etapa_eleg_status] FOREIGN KEY([elegibilidade_status_id])
REFERENCES [dbo].[dom_status_candidato_etapa] ([id])
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [FK_etapa_eleg_status]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [FK_etapa_elegibilidade] FOREIGN KEY([elegibilidade_etapa_id])
REFERENCES [dbo].[etapa] ([id])
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [FK_etapa_elegibilidade]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [FK_etapa_fases] FOREIGN KEY([fases_id])
REFERENCES [dbo].[fases] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [FK_etapa_fases]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [FK_etapa_status] FOREIGN KEY([status_id])
REFERENCES [dbo].[dom_status_etapa] ([id])
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [FK_etapa_status]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [FK_etapa_tipo] FOREIGN KEY([tipo_id])
REFERENCES [dbo].[dom_tipo_etapa] ([id])
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [FK_etapa_tipo]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [CHK_elegibilidade_completa] CHECK  (([elegibilidade_etapa_id] IS NULL AND [elegibilidade_status_id] IS NULL OR [elegibilidade_etapa_id] IS NOT NULL AND [elegibilidade_status_id] IS NOT NULL))
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [CHK_elegibilidade_completa]
GO
ALTER TABLE [dbo].[etapa]  WITH CHECK ADD  CONSTRAINT [CHK_etapa_datas] CHECK  (([data_fechamento] IS NULL OR [data_fechamento]>[data_abertura]))
GO
ALTER TABLE [dbo].[etapa] CHECK CONSTRAINT [CHK_etapa_datas]
GO
/* ---------- candidato_fases ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[candidato_fases](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[candidato_id] [varchar](11) NOT NULL,
	[fases_id] [int] NOT NULL,
	[status_id] [int] NOT NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
	[updated_at] [datetimeoffset](7) NOT NULL,
	[inscricao_id] [int] NULL,
 CONSTRAINT [PK_candidato_fases] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_candidato_fases] UNIQUE NONCLUSTERED 
(
	[candidato_id] ASC,
	[fases_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
ALTER TABLE [dbo].[candidato_fases] ADD  DEFAULT ((1)) FOR [status_id]
GO
ALTER TABLE [dbo].[candidato_fases] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
ALTER TABLE [dbo].[candidato_fases] ADD  DEFAULT (sysdatetimeoffset()) FOR [updated_at]
GO
ALTER TABLE [dbo].[candidato_fases]  WITH CHECK ADD  CONSTRAINT [FK_cf_candidato] FOREIGN KEY([candidato_id])
REFERENCES [dbo].[Candidato] ([canCPF])
GO
ALTER TABLE [dbo].[candidato_fases] CHECK CONSTRAINT [FK_cf_candidato]
GO
ALTER TABLE [dbo].[candidato_fases]  WITH CHECK ADD  CONSTRAINT [FK_cf_fases] FOREIGN KEY([fases_id])
REFERENCES [dbo].[fases] ([id])
GO
ALTER TABLE [dbo].[candidato_fases] CHECK CONSTRAINT [FK_cf_fases]
GO
ALTER TABLE [dbo].[candidato_fases]  WITH CHECK ADD  CONSTRAINT [FK_cf_status] FOREIGN KEY([status_id])
REFERENCES [dbo].[dom_status_candidato_fase] ([id])
GO
ALTER TABLE [dbo].[candidato_fases] CHECK CONSTRAINT [FK_cf_status]
GO
/* ---------- candidato_etapa ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[candidato_etapa](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[candidato_id] [varchar](11) NOT NULL,
	[etapa_id] [int] NOT NULL,
	[elegivel] [bit] NOT NULL,
	[status_id] [int] NOT NULL,
	[finalizado_em] [datetimeoffset](7) NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
	[updated_at] [datetimeoffset](7) NOT NULL,
	[inscricao_id] [int] NULL,
 CONSTRAINT [PK_candidato_etapa] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY],
 CONSTRAINT [UQ_candidato_etapa] UNIQUE NONCLUSTERED 
(
	[candidato_id] ASC,
	[etapa_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
ALTER TABLE [dbo].[candidato_etapa] ADD  DEFAULT ((1)) FOR [elegivel]
GO
ALTER TABLE [dbo].[candidato_etapa] ADD  DEFAULT ((1)) FOR [status_id]
GO
ALTER TABLE [dbo].[candidato_etapa] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
ALTER TABLE [dbo].[candidato_etapa] ADD  DEFAULT (sysdatetimeoffset()) FOR [updated_at]
GO
ALTER TABLE [dbo].[candidato_etapa]  WITH CHECK ADD  CONSTRAINT [FK_ce_candidato] FOREIGN KEY([candidato_id])
REFERENCES [dbo].[Candidato] ([canCPF])
GO
ALTER TABLE [dbo].[candidato_etapa] CHECK CONSTRAINT [FK_ce_candidato]
GO
ALTER TABLE [dbo].[candidato_etapa]  WITH CHECK ADD  CONSTRAINT [FK_ce_etapa] FOREIGN KEY([etapa_id])
REFERENCES [dbo].[etapa] ([id])
GO
ALTER TABLE [dbo].[candidato_etapa] CHECK CONSTRAINT [FK_ce_etapa]
GO
ALTER TABLE [dbo].[candidato_etapa]  WITH CHECK ADD  CONSTRAINT [FK_ce_status] FOREIGN KEY([status_id])
REFERENCES [dbo].[dom_status_candidato_etapa] ([id])
GO
ALTER TABLE [dbo].[candidato_etapa] CHECK CONSTRAINT [FK_ce_status]
GO
/* ---------- etapa_documento ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[etapa_documento](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[candidato_id] [varchar](11) NOT NULL,
	[etapa_id] [int] NOT NULL,
	[tipo_documento_id] [int] NULL,
	[contexto_id] [int] NOT NULL,
	[arquivo_nome] [nvarchar](max) NOT NULL,
	[arquivo_url] [nvarchar](max) NOT NULL,
	[mime_type] [varchar](100) NOT NULL,
	[tamanho_bytes] [bigint] NULL,
	[status_id] [int] NOT NULL,
	[analisado_por] [int] NULL,
	[analisado_em] [datetimeoffset](7) NULL,
	[motivo_rejeicao] [nvarchar](max) NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
	[updated_at] [datetimeoffset](7) NOT NULL,
 CONSTRAINT [PK_etapa_documento] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
ALTER TABLE [dbo].[etapa_documento] ADD  DEFAULT ((1)) FOR [status_id]
GO
ALTER TABLE [dbo].[etapa_documento] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
ALTER TABLE [dbo].[etapa_documento] ADD  DEFAULT (sysdatetimeoffset()) FOR [updated_at]
GO
ALTER TABLE [dbo].[etapa_documento]  WITH CHECK ADD  CONSTRAINT [FK_edoc_analisado_por] FOREIGN KEY([analisado_por])
REFERENCES [dbo].[UsuarioSistema] ([usiId])
GO
ALTER TABLE [dbo].[etapa_documento] CHECK CONSTRAINT [FK_edoc_analisado_por]
GO
ALTER TABLE [dbo].[etapa_documento]  WITH CHECK ADD  CONSTRAINT [FK_edoc_candidato] FOREIGN KEY([candidato_id])
REFERENCES [dbo].[Candidato] ([canCPF])
GO
ALTER TABLE [dbo].[etapa_documento] CHECK CONSTRAINT [FK_edoc_candidato]
GO
ALTER TABLE [dbo].[etapa_documento]  WITH CHECK ADD  CONSTRAINT [FK_edoc_contexto] FOREIGN KEY([contexto_id])
REFERENCES [dbo].[dom_contexto_documento] ([id])
GO
ALTER TABLE [dbo].[etapa_documento] CHECK CONSTRAINT [FK_edoc_contexto]
GO
ALTER TABLE [dbo].[etapa_documento]  WITH CHECK ADD  CONSTRAINT [FK_edoc_etapa] FOREIGN KEY([etapa_id])
REFERENCES [dbo].[etapa] ([id])
GO
ALTER TABLE [dbo].[etapa_documento] CHECK CONSTRAINT [FK_edoc_etapa]
GO
ALTER TABLE [dbo].[etapa_documento]  WITH CHECK ADD  CONSTRAINT [FK_edoc_status] FOREIGN KEY([status_id])
REFERENCES [dbo].[dom_status_documento] ([id])
GO
ALTER TABLE [dbo].[etapa_documento] CHECK CONSTRAINT [FK_edoc_status]
GO
ALTER TABLE [dbo].[etapa_documento]  WITH CHECK ADD  CONSTRAINT [FK_edoc_tipo] FOREIGN KEY([tipo_documento_id])
REFERENCES [dbo].[tipo_documento] ([id])
GO
ALTER TABLE [dbo].[etapa_documento] CHECK CONSTRAINT [FK_edoc_tipo]
GO
/* ---------- etapa_recurso ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[etapa_recurso](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[candidato_id] [varchar](11) NOT NULL,
	[etapa_id] [int] NOT NULL,
	[texto] [nvarchar](max) NOT NULL,
	[status_id] [int] NOT NULL,
	[resposta] [nvarchar](max) NULL,
	[respondido_por] [int] NULL,
	[respondido_em] [datetimeoffset](7) NULL,
	[created_at] [datetimeoffset](7) NOT NULL,
	[updated_at] [datetimeoffset](7) NOT NULL,
 CONSTRAINT [PK_etapa_recurso] PRIMARY KEY CLUSTERED 
(
	[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

GO
ALTER TABLE [dbo].[etapa_recurso] ADD  DEFAULT ((1)) FOR [status_id]
GO
ALTER TABLE [dbo].[etapa_recurso] ADD  DEFAULT (sysdatetimeoffset()) FOR [created_at]
GO
ALTER TABLE [dbo].[etapa_recurso] ADD  DEFAULT (sysdatetimeoffset()) FOR [updated_at]
GO
ALTER TABLE [dbo].[etapa_recurso]  WITH CHECK ADD  CONSTRAINT [FK_erec_candidato] FOREIGN KEY([candidato_id])
REFERENCES [dbo].[Candidato] ([canCPF])
GO
ALTER TABLE [dbo].[etapa_recurso] CHECK CONSTRAINT [FK_erec_candidato]
GO
ALTER TABLE [dbo].[etapa_recurso]  WITH CHECK ADD  CONSTRAINT [FK_erec_etapa] FOREIGN KEY([etapa_id])
REFERENCES [dbo].[etapa] ([id])
GO
ALTER TABLE [dbo].[etapa_recurso] CHECK CONSTRAINT [FK_erec_etapa]
GO
ALTER TABLE [dbo].[etapa_recurso]  WITH CHECK ADD  CONSTRAINT [FK_erec_status] FOREIGN KEY([status_id])
REFERENCES [dbo].[dom_status_recurso] ([id])
GO
ALTER TABLE [dbo].[etapa_recurso] CHECK CONSTRAINT [FK_erec_status]
GO
/* ---------- etapa_recurso_documento ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[etapa_recurso_documento](
	[recurso_id] [int] NOT NULL,
	[documento_id] [int] NOT NULL,
 CONSTRAINT [PK_etapa_recurso_documento] PRIMARY KEY CLUSTERED 
(
	[recurso_id] ASC,
	[documento_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
ALTER TABLE [dbo].[etapa_recurso_documento]  WITH CHECK ADD  CONSTRAINT [FK_erd_documento] FOREIGN KEY([documento_id])
REFERENCES [dbo].[etapa_documento] ([id])
GO
ALTER TABLE [dbo].[etapa_recurso_documento] CHECK CONSTRAINT [FK_erd_documento]
GO
ALTER TABLE [dbo].[etapa_recurso_documento]  WITH CHECK ADD  CONSTRAINT [FK_erd_recurso] FOREIGN KEY([recurso_id])
REFERENCES [dbo].[etapa_recurso] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[etapa_recurso_documento] CHECK CONSTRAINT [FK_erd_recurso]
GO
/* ---------- etapa_tipo_documento ---------- */
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[etapa_tipo_documento](
	[etapa_id] [int] NOT NULL,
	[tipo_documento_id] [int] NOT NULL,
	[obrigatorio] [bit] NOT NULL,
 CONSTRAINT [PK_etapa_tipo_documento] PRIMARY KEY CLUSTERED 
(
	[etapa_id] ASC,
	[tipo_documento_id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]

GO
ALTER TABLE [dbo].[etapa_tipo_documento] ADD  DEFAULT ((1)) FOR [obrigatorio]
GO
ALTER TABLE [dbo].[etapa_tipo_documento]  WITH CHECK ADD  CONSTRAINT [FK_etd_etapa] FOREIGN KEY([etapa_id])
REFERENCES [dbo].[etapa] ([id])
ON DELETE CASCADE
GO
ALTER TABLE [dbo].[etapa_tipo_documento] CHECK CONSTRAINT [FK_etd_etapa]
GO
ALTER TABLE [dbo].[etapa_tipo_documento]  WITH CHECK ADD  CONSTRAINT [FK_etd_tipo_doc] FOREIGN KEY([tipo_documento_id])
REFERENCES [dbo].[tipo_documento] ([id])
GO
ALTER TABLE [dbo].[etapa_tipo_documento] CHECK CONSTRAINT [FK_etd_tipo_doc]
GO
/* ---------- FKs das tabelas do módulo que apontam para as recriadas ---------- */
ALTER TABLE dbo.dom_tipo_etapa_publicacao ADD CONSTRAINT FK_dom_tipo_etapa_publicacao_tipo_etapa
    FOREIGN KEY (tipo_etapa_id) REFERENCES dbo.dom_tipo_etapa (id);
ALTER TABLE dbo.fase_grupo_cargos ADD CONSTRAINT FK_fase_grupo_cargos_fase
    FOREIGN KEY (fases_id) REFERENCES dbo.fases (id);
ALTER TABLE dbo.publicacao ADD CONSTRAINT FK_publicacao_fase
    FOREIGN KEY (fases_id) REFERENCES dbo.fases (id);
ALTER TABLE dbo.publicacao ADD CONSTRAINT FK_publicacao_etapa
    FOREIGN KEY (etapa_id) REFERENCES dbo.etapa (id);
GO
/* ---------- Valores de domínio (mesmos ids do IDECAN_v2) ---------- */
SET IDENTITY_INSERT dbo.dom_status_fase ON;
INSERT INTO dbo.dom_status_fase (id, valor, descricao) VALUES (1, N'rascunho', N'Sendo configurada');
INSERT INTO dbo.dom_status_fase (id, valor, descricao) VALUES (2, N'em_andamento', N'Pelo menos uma etapa aberta');
INSERT INTO dbo.dom_status_fase (id, valor, descricao) VALUES (3, N'concluida', N'Todas etapas obrigatórias encerradas');
INSERT INTO dbo.dom_status_fase (id, valor, descricao) VALUES (4, N'cancelada', N'Fase cancelada');
SET IDENTITY_INSERT dbo.dom_status_fase OFF;
DBCC CHECKIDENT ('dbo.dom_status_fase', RESEED, 4) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_status_etapa ON;
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (1, N'aguardando', N'Configurada mas ainda não abriu');
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (2, N'aberta', N'Dentro da janela de tempo');
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (3, N'encerrada', N'Passou a data_fechamento');
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (4, N'cancelada', N'Etapa cancelada');
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (5, N'Andamento', N'Andamento');
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (6, N'Concluída', N'Concluída');
INSERT INTO dbo.dom_status_etapa (id, valor, descricao) VALUES (7, N'Publicada', N'Publicada');
SET IDENTITY_INSERT dbo.dom_status_etapa OFF;
DBCC CHECKIDENT ('dbo.dom_status_etapa', RESEED, 7) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_status_candidato_etapa ON;
INSERT INTO dbo.dom_status_candidato_etapa (id, valor, descricao) VALUES (1, N'pendente', N'Elegível, ainda não agiu');
INSERT INTO dbo.dom_status_candidato_etapa (id, valor, descricao) VALUES (2, N'em_andamento', N'Iniciou mas não finalizou');
INSERT INTO dbo.dom_status_candidato_etapa (id, valor, descricao) VALUES (3, N'aprovado', N'Concluiu com sucesso');
INSERT INTO dbo.dom_status_candidato_etapa (id, valor, descricao) VALUES (4, N'reprovado', N'Falhou na etapa');
INSERT INTO dbo.dom_status_candidato_etapa (id, valor, descricao) VALUES (5, N'nao_elegivel', N'Regra de elegibilidade não satisfeita');
INSERT INTO dbo.dom_status_candidato_etapa (id, valor, descricao) VALUES (6, N'nao_participou', N'Elegível mas não agiu dentro da janela');
SET IDENTITY_INSERT dbo.dom_status_candidato_etapa OFF;
DBCC CHECKIDENT ('dbo.dom_status_candidato_etapa', RESEED, 6) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_status_candidato_fase ON;
INSERT INTO dbo.dom_status_candidato_fase (id, valor, descricao) VALUES (1, N'aguardando', N'Fase ainda não abriu para ele');
INSERT INTO dbo.dom_status_candidato_fase (id, valor, descricao) VALUES (2, N'em_andamento', N'Participando da fase');
INSERT INTO dbo.dom_status_candidato_fase (id, valor, descricao) VALUES (3, N'aprovado', N'Passou em todas etapas reprováveis elegíveis');
INSERT INTO dbo.dom_status_candidato_fase (id, valor, descricao) VALUES (4, N'reprovado', N'Falhou em pelo menos uma etapa reprovável');
SET IDENTITY_INSERT dbo.dom_status_candidato_fase OFF;
DBCC CHECKIDENT ('dbo.dom_status_candidato_fase', RESEED, 4) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_status_documento ON;
INSERT INTO dbo.dom_status_documento (id, valor, descricao) VALUES (1, N'pendente', N'Enviado, aguardando análise');
INSERT INTO dbo.dom_status_documento (id, valor, descricao) VALUES (2, N'aprovado', N'Documento aceito pela banca');
INSERT INTO dbo.dom_status_documento (id, valor, descricao) VALUES (3, N'rejeitado', N'Documento recusado com motivo');
SET IDENTITY_INSERT dbo.dom_status_documento OFF;
DBCC CHECKIDENT ('dbo.dom_status_documento', RESEED, 3) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_status_recurso ON;
INSERT INTO dbo.dom_status_recurso (id, valor, descricao) VALUES (1, N'aberto', N'Aberto pelo candidato, aguardando análise');
INSERT INTO dbo.dom_status_recurso (id, valor, descricao) VALUES (2, N'em_analise', N'Em análise pela banca');
INSERT INTO dbo.dom_status_recurso (id, valor, descricao) VALUES (3, N'deferido', N'Recurso aceito');
INSERT INTO dbo.dom_status_recurso (id, valor, descricao) VALUES (4, N'indeferido', N'Recurso negado');
SET IDENTITY_INSERT dbo.dom_status_recurso OFF;
DBCC CHECKIDENT ('dbo.dom_status_recurso', RESEED, 4) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_contexto_documento ON;
INSERT INTO dbo.dom_contexto_documento (id, valor, descricao) VALUES (1, N'inscricao', N'Enviado durante a inscrição');
INSERT INTO dbo.dom_contexto_documento (id, valor, descricao) VALUES (2, N'isencao_taxa', N'Enviado no pedido de isenção');
INSERT INTO dbo.dom_contexto_documento (id, valor, descricao) VALUES (3, N'heteroidentificacao', N'Enviado na heteroidentificação');
INSERT INTO dbo.dom_contexto_documento (id, valor, descricao) VALUES (4, N'recurso', N'Anexado a um recurso');
INSERT INTO dbo.dom_contexto_documento (id, valor, descricao) VALUES (5, N'envio_documentos', N'Enviado em etapa exclusiva de documentos');
SET IDENTITY_INSERT dbo.dom_contexto_documento OFF;
DBCC CHECKIDENT ('dbo.dom_contexto_documento', RESEED, 5) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_tipo_etapa ON;
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (1, N'inscricao', N'Inscrição do candidato');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (2, N'isencao_taxa', N'Pedido de isenção de taxa');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (3, N'heteroidentificacao', N'Verificação presencial de autodeclaração');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (4, N'recurso', N'Recurso administrativo');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (5, N'resultado', N'Publicação de resultado');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (6, N'prova', N'Aplicação de prova');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (7, N'envio_documentos', N'Entrega de documentação (ex: pós-prova)');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (8, N'outro', N'Tipo não categorizado');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (9, N'prova_objetiva', N'Prova objetiva');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (10, N'prova_discursiva', N'Prova discursiva');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (11, N'taf', N'Teste de aptidão física (TAF)');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (12, N'the', N'Teste de habilidade específica (THE)');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (13, N'biopsicossocial', N'Avaliação biopsicossocial');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (14, N'analise_atendimento_especial', N'Análise de solicitações: atendimento especial');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (15, N'analise_pcd', N'Análise de solicitações: PcD');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (16, N'analise_hipossuficiencia', N'Análise de solicitações: hipossuficiência');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (17, N'analise_isencao_pagamento', N'Análise de solicitações: isenção de pagamento');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (18, N'analise_gabarito', N'Análise de solicitações: gabarito');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (19, N'analise_impugnacao', N'Análise de solicitações: impugnação');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (20, N'analise_prova_titulos', N'Análise de solicitações: prova de títulos');
INSERT INTO dbo.dom_tipo_etapa (id, valor, descricao) VALUES (21, N'analise_locais_prova', N'Análise de solicitações: locais de prova (ensalamento)');
SET IDENTITY_INSERT dbo.dom_tipo_etapa OFF;
DBCC CHECKIDENT ('dbo.dom_tipo_etapa', RESEED, 21) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_tipo_publicacao ON;
INSERT INTO dbo.dom_tipo_publicacao (id, valor, descricao, abre_recurso) VALUES (1, N'local_prova', N'Local de prova', 0);
INSERT INTO dbo.dom_tipo_publicacao (id, valor, descricao, abre_recurso) VALUES (2, N'gabarito_preliminar', N'Gabarito preliminar', 1);
INSERT INTO dbo.dom_tipo_publicacao (id, valor, descricao, abre_recurso) VALUES (3, N'gabarito_definitivo', N'Gabarito definitivo', 0);
INSERT INTO dbo.dom_tipo_publicacao (id, valor, descricao, abre_recurso) VALUES (4, N'resultado_preliminar', N'Resultado preliminar', 1);
INSERT INTO dbo.dom_tipo_publicacao (id, valor, descricao, abre_recurso) VALUES (5, N'resultado_oficial', N'Resultado oficial', 0);
SET IDENTITY_INSERT dbo.dom_tipo_publicacao OFF;
DBCC CHECKIDENT ('dbo.dom_tipo_publicacao', RESEED, 5) WITH NO_INFOMSGS;
SET IDENTITY_INSERT dbo.dom_tipo_etapa_publicacao ON;
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (1, 14, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (2, 14, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (3, 18, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (4, 18, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (5, 16, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (6, 16, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (7, 19, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (8, 19, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (9, 17, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (10, 17, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (11, 21, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (12, 21, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (13, 15, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (14, 15, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (15, 20, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (16, 20, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (17, 13, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (18, 13, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (19, 3, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (20, 3, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (21, 10, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (22, 10, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (23, 9, 3);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (24, 9, 2);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (25, 9, 1);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (26, 9, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (27, 9, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (28, 11, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (29, 11, 4);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (30, 12, 5);
INSERT INTO dbo.dom_tipo_etapa_publicacao (id, tipo_etapa_id, tipo_publicacao_id) VALUES (31, 12, 4);
SET IDENTITY_INSERT dbo.dom_tipo_etapa_publicacao OFF;
DBCC CHECKIDENT ('dbo.dom_tipo_etapa_publicacao', RESEED, 31) WITH NO_INFOMSGS;
GO
COMMIT TRANSACTION;
GO
SET NOEXEC OFF;
GO
/* Conferência */
SELECT 'dom_tipo_etapa' AS tabela, COUNT(*) AS linhas FROM dbo.dom_tipo_etapa
UNION ALL SELECT 'dom_tipo_publicacao', COUNT(*) FROM dbo.dom_tipo_publicacao
UNION ALL SELECT 'dom_tipo_etapa_publicacao', COUNT(*) FROM dbo.dom_tipo_etapa_publicacao
UNION ALL SELECT 'dom_status_fase', COUNT(*) FROM dbo.dom_status_fase
UNION ALL SELECT 'dom_status_etapa', COUNT(*) FROM dbo.dom_status_etapa;
