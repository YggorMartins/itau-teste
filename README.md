# 📊 Desafio Técnico Itaú - API de Transações e Estatísticas

Este repositório contém a minha solução para o desafio técnico do Itaú Unibanco. A aplicação consiste em uma API REST robusta, desenvolvida em **Java** com **Spring Boot**, capaz de receber transações financeiras e calcular estatísticas consolidadas em tempo real (dos últimos 60 segundos).

## 🎯 Objetivo do Projeto

O principal objetivo é demonstrar competência em:
* Arquitetura de software escalável e limpa.
* Manipulação eficiente de coleções de dados em memória utilizando Java Streams.
* Agendamento de tarefas (`@Scheduled`) para manutenção de estado da aplicação.
* Validação rigorosa de regras de negócio.

## 🚀 Tecnologias e Ferramentas

* **Linguagem:** Java 17
* **Framework:** Spring Boot 3
* **Gerenciador de Dependências:** Maven
* **Bibliotecas:** Lombok (produtividade), Spring Web, Spring Scheduling.
* **Ferramenta de Teste de API:** Insomnia / Postman.

## 🛠️ Funcionalidades e Endpoints

A API foi testada e validada utilizando o **Insomnia**. Abaixo estão os endpoints disponíveis:

### 1. Criar Transação
`POST /transacao`
* Recebe um JSON com `valor` e `dataHora`.
* **Regras de Negócio:**
    * O valor deve ser maior ou igual a zero.
    * A data não pode ser no futuro.
    * **HTTP 201:** Transação aceita.
    * **HTTP 422:** Transação inválida (valor negativo ou data futura).
    * **HTTP 400:** JSON inválido ou malformado.

### 2. Calcular Estatísticas
`GET /estatistica`
* Retorna os dados estatísticos das transações que ocorreram nos últimos 60 segundos.
* **Campos retornados:** `count` (contagem), `sum` (soma), `avg` (média), `min` (mínimo) e `max` (máximo).

### 3. Limpar Dados
`DELETE /transacao`
* Limpa todas as transações armazenadas na memória.

## ⚙️ Diferenciais da Implementação

* **Processamento em Memória:** Para garantir a performance exigida pelo desafio, os dados são geridos em memória, eliminando latência de I/O de disco.
* **Limpeza Automática:** Implementei um processo agendado que limpa automaticamente transações com mais de 60 segundos, garantindo que o endpoint de estatísticas esteja sempre preciso sem processar dados irrelevantes.
* **Tratamento de Exceções:** Uso de blocos `try-catch` e lançamentos de exceções específicas para garantir que a API responda com os códigos HTTP corretos de acordo com a especificação.

## 🏁 Como Rodar o Projeto

1.  Clone o repositório:
    ```bash
    git clone https://github.com/YggorMartins/itau-teste.git
    ```
2.  Abra o projeto em sua IDE (IntelliJ, VS Code ou Eclipse).
3.  Execute a aplicação através da classe `ItauTesteApplication.java`.
4.  Utilize o **Insomnia** para realizar as chamadas nos endpoints listados acima.

---

### 👨‍💻 Autor
**Yggor Martins**
*Focado em Desenvolvimento Fullstack (Java/Spring Boot | React | Node.js)*
