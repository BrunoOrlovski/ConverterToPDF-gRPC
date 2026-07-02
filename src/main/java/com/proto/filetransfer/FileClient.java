package com.proto.filetransfer;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FileClient {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("==========================================================");
        System.out.println("        CLIENTE DE CONVERSÃO PARA PDF gRPC INICIADO       ");
        System.out.println("==========================================================");
        
        System.out.print("[Cliente] Digite o IP do Servidor (deixe vazio para 'localhost'): ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) {
            host = "localhost";
        }
        
        int port = 50051;
        System.out.println("[Cliente] Conectando ao servidor em " + host + ":" + port + "...");
        
        // Criação do canal gRPC
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        
        try {
            while (true) {
                System.out.println("\n--- MENU ---");
                System.out.println("1. Converter arquivo para PDF (TXT, PNG, JPG)");
                System.out.println("2. Sair");
                System.out.print("Escolha uma opção: ");
                
                String choice = scanner.nextLine().trim();
                if ("2".equals(choice)) {
                    System.out.println("[Cliente] Finalizando aplicação.");
                    break;
                } else if ("1".equals(choice)) {
                    System.out.print("[Cliente] Digite o caminho completo do arquivo para conversão: ");
                    String filePath = scanner.nextLine().trim();
                    
                    // Remover aspas do caminho se arrastado para o console no Windows
                    if (filePath.startsWith("\"") && filePath.endsWith("\"")) {
                        filePath = filePath.substring(1, filePath.length() - 1);
                    }
                    
                    File file = new File(filePath);
                    if (!file.exists() || !file.isFile()) {
                        System.err.println("[Cliente] Erro: Arquivo não encontrado ou caminho inválido.");
                        continue;
                    }
                    
                    convertFileToPdf(channel, file);
                } else {
                    System.out.println("[Cliente] Opção inválida.");
                }
            }
        } finally {
            // Fechamento amigável do canal gRPC
            channel.shutdown();
            try {
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            scanner.close();
        }
    }

    private static void convertFileToPdf(ManagedChannel channel, final File file) {
        // Obter o diretório de destino (o mesmo do arquivo original) e nome do arquivo de saída
        String originalName = file.getName();
        String baseName = originalName.substring(0, originalName.lastIndexOf('.'));
        final File outputFile = new File(file.getParentFile(), baseName + ".pdf");
        
        System.out.println("[Cliente] Iniciando upload de: " + originalName);
        System.out.println("[Cliente] O resultado será salvo em: " + outputFile.getAbsolutePath());

        // Precisamos usar o Stub assíncrono para streams bidirecionais
        FileServiceGrpc.FileServiceStub stub = FileServiceGrpc.newStub(channel);
        
        final CountDownLatch latch = new CountDownLatch(1);
        final ByteArrayOutputStream pdfBytesStream = new ByteArrayOutputStream();
        
        // Define o observer que receberá os dados do PDF de volta do servidor
        StreamObserver<ConvertResponse> responseObserver = new StreamObserver<ConvertResponse>() {
            @Override
            public void onNext(ConvertResponse value) {
                // Acumula os bytes do PDF recebidos do servidor
                byte[] chunk = value.getChunk().toByteArray();
                try {
                    pdfBytesStream.write(chunk);
                } catch (IOException e) {
                    System.err.println("[Cliente] Erro ao acumular bytes do PDF: " + e.getMessage());
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("\n[Cliente] Erro durante a conversão no servidor: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                // Transmissão concluída pelo servidor, grava o PDF final
                byte[] pdfBytes = pdfBytesStream.toByteArray();
                System.out.println("\n[Cliente] PDF recebido completamente (" + pdfBytes.length + " bytes). Salvando arquivo...");
                
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(pdfBytes);
                    System.out.println("[Cliente] SUCESSO: Arquivo salvo com sucesso!");
                } catch (IOException e) {
                    System.err.println("[Cliente] Erro ao gravar o arquivo PDF final: " + e.getMessage());
                }
                
                latch.countDown();
            }
        };

        // Inicia a chamada RPC bidirecional
        StreamObserver<ConvertRequest> requestObserver = stub.convertToPdf(responseObserver);

        try {
            // 1. Enviar os metadados (nome do arquivo original)
            ConvertRequest metadataRequest = ConvertRequest.newBuilder()
                    .setMetadata(Metadata.newBuilder().setName(originalName).build())
                    .build();
            requestObserver.onNext(metadataRequest);

            // 2. Enviar o arquivo em pedaços (chunks) de 4KB
            byte[] buffer = new byte[4096];
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                int bytesRead;
                long totalSent = 0;
                long fileLength = file.length();
                
                while ((bytesRead = bis.read(buffer)) != -1) {
                    ConvertRequest chunkRequest = ConvertRequest.newBuilder()
                            .setChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                            .build();
                    requestObserver.onNext(chunkRequest);
                    totalSent += bytesRead;
                    
                    // Exibir porcentagem de envio
                    int progress = (int) ((totalSent * 100) / fileLength);
                    System.out.print("\r[Cliente] Enviando arquivo... " + progress + "%");
                }
                System.out.println("\n[Cliente] Envio finalizado. Aguardando servidor converter...");
            }

            // Informa ao servidor que o cliente terminou de enviar
            requestObserver.onCompleted();

            // Aguarda a resposta do servidor (no máximo 5 minutos)
            if (!latch.await(5, TimeUnit.MINUTES)) {
                System.err.println("[Cliente] Tempo limite excedido aguardando resposta do servidor.");
            }

        } catch (Exception e) {
            System.err.println("[Cliente] Erro ao enviar arquivo: " + e.getMessage());
            requestObserver.onError(e);
        }
    }
}
