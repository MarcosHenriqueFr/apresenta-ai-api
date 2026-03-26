
# Apresenta AI - API 🤖 📊
### Um sistema para criação de apresentações personalizadas

## Tecnologias 🖥️
<ul>
    <li>Java 21
    <li>Maven
    <li>Spring Boot
    <li>JUnit e Mockito
    <li> PDFBox
    <li> Apache POI
    <li> GroqCloud
</ul>

## Ideia geral do projeto 💡
O projeto surgiu de uma necessidade da criação de slides rapidamente,
mas ainda com a presença de uma validação humana em relação à qualidade
dos dados criados pelo modelo de inteligência artificial. <br>
Não é incomum que ao tentar apresentar um projeto/ideia, tenha se sentido confuso
em relação a como conseguiria fazer um arquivo de apresentação para ele.
Dessa forma, o "Apresenta AÍ" busca facilitar esse processo de criação
usando documentos que o usuário fez como base ou alimentando de forma fina o contexto
para o LLM.

## Como testar o projeto 🚀

### Pré-requisitos

Antes de iniciar o projeto, é necessário baixar os itens a seguir:
<ul>
    <li>JDK 21
    <li>Git
</ul>

### Clonando

Primeiro clone o projeto para uma pasta da sua máquina:

```bash
git clone https://github.com/MarcosHenriqueFr/apresenta-ai-api.git
```
Depois entre na pasta criada:

```bash 
cd apresenta-ai-api/
```

### Configurando as variáveis de ambiente
<em>Obs: essa etapa pode sofrer alterações.</em>

#### 1. application.yaml

Mude de acordo com as suas necessidades, para melhor entendimento
esse é o que está sendo dentro do projeto:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]

  application:
    name: projetogroq

  servlet:
    multipart:
      max-file-size: 3MB
      max-request-size: 9MB

groq:
  api-key: ${GROQ_SECRET_KEY}

server:
  servlet:
    session:
      timeout: 1h
      cookie:
        max-age: 1h
      tracking-modes: cookie

logging:
  level:
    com.example.projetogroq.service: DEBUG
```

#### 2. Arquivo .env
É essencial que você crie um arquivo .env na raiz do seu projeto,
nesse arquivo é importante que você adicione uma linha informando a sua
API KEY do serviço GroqCloud para se comunicar com o modelo de IA.

Acrescente essa linha no arquivo .env e substitua com sua chave:
```env
GROQ_SECRET_KEY=SUA_CHAVE
```

#### **Agora volte para a raiz do projeto**

### Rodando o projeto

Por enquanto, a única forma de rodar o projeto é usando Maven:

```bash
mvn spring-boot:run
```

Caso não possua o maven digite:
```bash
./mvnw spring-boot:run
```

## Endpoints 🚩

| Endpoint                                   | Descrição                                                                            |
|--------------------------------------------|--------------------------------------------------------------------------------------|
| <kbd>POST /api/presentation/generate</kbd> | Cria uma apresentação e coloca na Sessão                                             |
| <kbd>POST /api/files/pptx</kbd>            | Converte o objeto em Sessão em um arquivo baixável, de acordo com o estilo informado |
Pretendo fazer um front-end para esse projeto, então as requisições não estão disponíveis em um arquivo baixável.
<br>

## O que foi aprendido 📝

<ul>
    <li> Como fazer uma requisição para uma API Externa;
    <li> Reduzir problemas de duplicidade de código usando Clean Code;
    <li> Uso de CompletableFuture para a leitura de arquivos de forma Assíncrona;
    <li> Fazer a utilização de Sessões para guardar objetos;
    <li> Otimização de prompts de LLM para diminuir alucinações da IA;
    <li> Configurar o CORS para que a API possa ser acessada por um navegador;
    <li> Fazer o uso de templates existentes para manipulação de arquivos .pptx;
    <li> Validação customizada de dados Enums usando Java Bean Validation.
</ul>

## Mudanças futuras 📈


<ul>
    <li> Aplicação de um frontend para o projeto.
</ul>
<br><br>

**Obrigado pela sua atenção. Qualquer feedback é bem-vindo!**
