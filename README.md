# Conversor de Arquivos para PDF via gRPC (Java)

Este projeto é uma demonstração prática do uso do framework **gRPC** com a linguagem **Java (compatível com Java 8)**. A aplicação realiza a conversão de arquivos de texto (`.txt`) e imagens (`.png`, `.jpg`, `.jpeg`) para o formato PDF.

Toda a comunicação de envio do arquivo original e retorno do PDF gerado ocorre por meio de **Streaming Bidirecional (`Bidirectional Streaming`)**, permitindo transferir arquivos de qualquer tamanho de forma extremamente eficiente, divididos em blocos (chunks) de 4KB na memória, sem sobrecarregar a memória RAM do cliente ou do servidor.

---

## Estrutura do Projeto

*   `src/main/proto/filetransfer.proto`: Definição do serviço gRPC e mensagens do Protocol Buffers.
*   `src/main/java/com/proto/filetransfer/PdfConverter.java`: Lógica de conversão usando a biblioteca **OpenPDF**.
*   `src/main/java/com/proto/filetransfer/FileServiceImpl.java`: Implementação do serviço gRPC que recebe o fluxo do cliente, converte e envia o PDF de volta em partes.
*   `src/main/java/com/proto/filetransfer/FileServer.java`: Servidor gRPC que escuta na porta `50051`.
*   `src/main/java/com/proto/filetransfer/FileClient.java`: Cliente interativo no terminal para conexão e envio de arquivos.

---

## Como Compilar e Empacotar

O projeto inclui uma instalação local portátil do Apache Maven na pasta `.maven/`. Assim, você não precisa instalar nenhuma ferramenta global de build na sua máquina.

Para compilar o código do protobuf e empacotar a aplicação em um único arquivo JAR executável, execute o seguinte comando no terminal (PowerShell) dentro da pasta raiz do projeto:

```powershell
.\.maven\bin\mvn.cmd clean package
```

Após o término da execução, o arquivo JAR contendo todas as dependências será gerado na pasta `target/`:
`target/grpc-pdf-converter-1.0-SNAPSHOT-shaded.jar`

---

## Como Executar a Aplicação

### 1. Executando o Servidor
Para iniciar o servidor gRPC, execute o comando abaixo no terminal da máquina servidora:

```powershell
java -cp target/grpc-pdf-converter-1.0-SNAPSHOT-shaded.jar com.proto.filetransfer.FileServer
```
O servidor começará a rodar na porta `50051`.

### 2. Executando o Cliente
Para iniciar o cliente interativo, execute o comando abaixo no terminal da máquina cliente:

```powershell
java -cp target/grpc-pdf-converter-1.0-SNAPSHOT-shaded.jar com.proto.filetransfer.FileClient
```

---

## Demonstração com Dois Computadores na Rede

Para apresentar o projeto em rede (dois nodos):

1.  **Conecte ambos os computadores na mesma rede** (ex: o mesmo Wi-Fi de um celular ou roteador local).
2.  No **Computador do Servidor**, abra o terminal e digite `ipconfig`. Procure pelo campo `Endereço IPv4` (ex: `192.168.1.15`).
3.  Inicie o **Servidor** no Computador 1 usando o comando de execução correspondente.
4.  Copie o arquivo JAR gerado (`target/grpc-pdf-converter-1.0-SNAPSHOT-shaded.jar`) para o **Computador do Cliente** (ou clone o projeto nele e compile).
5.  Inicie o **Cliente** no Computador 2.
6.  Quando solicitado no menu do Cliente, digite o IP do Servidor (ex: `192.168.1.15`).
7.  Escolha a opção **1**, digite o caminho completo de uma imagem ou arquivo de texto local (ex: `C:\Users\Usuario\Desktop\foto.png`).
8.  Verifique a barra de progresso. O arquivo convertido em PDF será salvo automaticamente na mesma pasta do arquivo original com o nome `<nome_original>.pdf`.
