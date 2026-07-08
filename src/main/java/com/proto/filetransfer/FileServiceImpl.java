package com.proto.filetransfer;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FileServiceImpl extends FileServiceGrpc.FileServiceImplBase {

    @Override
    public StreamObserver<ConvertRequest> convertToPdf(final StreamObserver<ConvertResponse> responseObserver) {
        return new StreamObserver<ConvertRequest>() {
            private String fileName;
            private final ByteArrayOutputStream fileBytesStream = new ByteArrayOutputStream();
            private boolean errorOccurred = false;

            @Override
            public void onNext(ConvertRequest request) {
                if (errorOccurred) return;

                if (request.hasMetadata()) {
                    fileName = request.getMetadata().getName();
                    System.out.println("[Servidor] Recebido metadados do arquivo: " + fileName);
                } else if (request.hasChunk()) {
                    byte[] chunk = request.getChunk().toByteArray();
                    try {
                        fileBytesStream.write(chunk);
                    } catch (IOException e) {
                        onError(e);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[Servidor] Erro recebido do cliente: " + t.getMessage());
                errorOccurred = true;
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                if (errorOccurred) return;

                if (fileName == null || fileName.isEmpty()) {
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription("O nome do arquivo não foi enviado nos metadados")
                            .asRuntimeException());
                    return;
                }

                byte[] inputBytes = fileBytesStream.toByteArray();
                System.out.println("[Servidor] Arquivo \"" + fileName + "\" totalmente recebido (" + inputBytes.length + " bytes). Convertendo...");

                try {
                    byte[] pdfBytes = PdfConverter.convertToPdf(fileName, inputBytes);
                    System.out.println("[Servidor] Conversão concluída! Tamanho do PDF: " + pdfBytes.length + " bytes. Enviando ao cliente...");

                    int chunkSize = 4096;
                    int offset = 0;
                    while (offset < pdfBytes.length) {
                        int currentChunkSize = Math.min(chunkSize, pdfBytes.length - offset);
                        ByteString byteString = ByteString.copyFrom(pdfBytes, offset, currentChunkSize);
                        
                        ConvertResponse response = ConvertResponse.newBuilder()
                                .setChunk(byteString)
                                .build();
                        
                        responseObserver.onNext(response);
                        offset += currentChunkSize;
                    }

                    // Finaliza a transmissão da resposta
                    responseObserver.onCompleted();
                    System.out.println("[Servidor] Envio do PDF finalizado.");
                } catch (IllegalArgumentException e) {
                    System.err.println("[Servidor] Argumento inválido: " + e.getMessage());
                    responseObserver.onError(Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException());
                } catch (Exception e) {
                    System.err.println("[Servidor] Erro interno na conversão: " + e.getMessage());
                    e.printStackTrace();
                    responseObserver.onError(Status.INTERNAL
                            .withDescription("Erro interno na conversão: " + e.getMessage())
                            .asRuntimeException());
                }
            }
        };
    }
}
