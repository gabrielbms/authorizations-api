# API de Autorizações

## 🏗️ Decisões de Design e Arquitetura

### 1. Modelagem de Dados no DynamoDB (Evitando Scans)
Executar operações de `Scan` no DynamoDB para filtrar dados é um antipadrão que degrada drasticamente a performance e aumenta os custos da AWS conforme o volume de dados cresce. Para permitir buscas eficientes por `userId`, `status` e intervalo de datas, implementei um **Global Secondary Index (GSI)** chamado `user-date-index`.
* **Partition Key (PK):** `userId`
* **Sort Key (SK):** `createdAt`
* **Filtro:** `status`

Este esquema permite que a aplicação execute operações de `Query` altamente eficientes diretamente no índice, buscando apenas os itens necessários com complexidade ideal.

### 2. Paginação Baseada em Cursor
A paginação baseada em offset (`page=2&size=20`) é ineficiente em bancos de dados NoSQL distribuídos. Esta API implementa **Paginação Baseada em Cursor** utilizando a propriedade nativa `LastEvaluatedKey` do DynamoDB.
Quando o resultado de uma busca excede o limite da página, um `nextToken` codificado em Base64 é retornado. O cliente fornece este token nas requisições subsequentes para retomar a busca exatamente de onde parou, garantindo tempos de resposta estáveis independentemente do volume de dados.

### 3. Estratégia de Testes
A suíte de testes prioriza o isolamento e a velocidade de execução, sem depender de configurações pesadas de integração:
* **Camada de Serviço:** Testes unitários utilizando Mockito validam regras de negócio, restrições de entrada e normalização de payload de forma independente do banco de dados.
* **Camada Web:** Testes utilizando `@WebMvcTest` validam as fronteiras HTTP, garantindo os códigos de status corretos (201, 200, 400), o mapeamento de payload e o tratamento centralizado de erros através do `GlobalExceptionHandler`.

## 📌 Premissas Assumidas

1. **`userId` é obrigatório para buscas:** Devido ao design do GSI otimizado para operações de `Query`, a Partition Key (`userId`) deve ser fornecida em todas as requisições GET. Permitir buscas estritamente por `status` acionaria um `Scan` em toda a tabela, o que viola as melhores práticas de performance em NoSQL. Portanto, o `userId` é forçado como um parâmetro obrigatório.
2. **Timestamps ISO-8601:** Todos os parâmetros de data (`startDate`, `endDate`) exigem estritamente os formatos padrão UTC ISO-8601 (ex: `2026-01-01T00:00:00Z`). Datas mal formatadas são explicitamente rejeitadas com um `400 Bad Request` para evitar falhas silenciosas nas consultas ao banco.

## ⚙️ Como Executar Localmente

## Pré-requisitos
* Java 21
* Docker e Docker Compose

### 1. Subir o Banco de Dados
Na raiz do projeto, inicie o DynamoDB Local e a interface administrativa:

```bash
docker-compose up -d
```
* **DynamoDB:** `http://localhost:8000`
* **Interface Admin:** `http://localhost:8001`

### 2. Iniciar a Aplicação
Com a infraestrutura ativa, compile e suba a API usando o Maven Wrapper embutido:

```bash
mvn clean install
mvn spring-boot:run
```
A API estará pronta para receber requisições em `http://localhost:8080`.

## Endpoints para Teste

### 1. Criar Autorização (POST)
```bash
curl -i -X POST http://localhost:8080/authorizations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "status": "ACTIVE"
  }'
```

### 2. Buscar Autorizações - Primeira Página (GET)
Retorna os primeiros itens e o cursor de paginação (`nextToken`).

```bash
curl -i -X GET "http://localhost:8080/authorizations?userId=user-123&status=ACTIVE&startDate=2026-01-01T00:00:00Z&endDate=2026-12-31T23:59:59Z"
```

### 3. Buscar Autorizações - Páginas Seguintes (GET)
Copie o valor de `nextToken` recebido no endpoint anterior e substitua na chamada abaixo para obter a próxima página.

```bash
curl -i -X GET "http://localhost:8080/authorizations?userId=user-123&status=ACTIVE&nextToken=COLOQUE_O_TOKEN_AQUI"
```

## Alguns facilitadores

### Script para popular o banco mais rapidamente
Apenas cole no terminal powershell e ele vai criar 25 registros, para validar a paginação.
```
1..25 | ForEach-Object { Invoke-RestMethod -Uri "http://localhost:8080/authorizations" -Method Post -Headers @{"Content-Type"="application/json"} -Body '{"userId": "user-paginacao", "status": "ACTIVE"}' }
```

### Interface gráfica pra ver o banco de dados localmente
Visando facilitar a visualização dos dados, criei no docker-compose uma interface gráfica, para acessar, abra no navegador:
```
http://localhost:8001/
```