package com.proto.filetransfer;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class FileServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 50051;
        
        System.out.println("==========================================================");
        System.out.println("        SERVIDOR DE CONVERSÃO PARA PDF gRPC INICIADO      ");
        System.out.println("==========================================================");
        System.out.println("[Servidor] Iniciando o servidor na porta " + port + "...");
        
        Server server = ServerBuilder.forPort(port)
                .addService(new FileServiceImpl())
                .build();

        server.start();
        
        System.out.println("[Servidor] Servidor rodando e aguardando conexões na porta " + port + "!");
        System.out.println("[Servidor] DICA: Caso vá se conectar de outra máquina na rede local,");
        System.out.println("           use o IP local desta máquina (obtido via 'ipconfig').");
        System.out.println("==========================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Servidor] Recebida solicitação de parada. Encerrando gRPC...");
            if (server != null) {
                server.shutdown();
            }
            System.out.println("[Servidor] Servidor finalizado.");
        }));

        server.awaitTermination();
    }
}
