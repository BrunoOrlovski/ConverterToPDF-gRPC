package com.proto.filetransfer;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PdfConverter {

    /**
     * Converte o arquivo recebido para PDF com base na sua extensão.
     * 
     * @param fileName Nome do arquivo original.
     * @param fileBytes Conteúdo em bytes do arquivo.
     * @return Array de bytes representando o arquivo PDF gerado.
     * @throws IOException Se houver erro de leitura dos dados.
     * @throws DocumentException Se houver erro na formatação do PDF.
     */
    public static byte[] convertToPdf(String fileName, byte[] fileBytes) throws IOException, DocumentException {
        String ext = getFileExtension(fileName).toLowerCase();
        
        System.out.println("[Conversor] Iniciando conversão do arquivo: " + fileName + " (" + ext + ") - Tamanho: " + fileBytes.length + " bytes");
        
        if ("txt".equals(ext)) {
            return convertTextToPdf(fileBytes);
        } else if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)) {
            return convertImageToPdf(fileBytes);
        } else {
            throw new IllegalArgumentException("Formato não suportado para conversão: ." + ext);
        }
    }

    private static byte[] convertTextToPdf(byte[] fileBytes) throws DocumentException {
        String text = new String(fileBytes, StandardCharsets.UTF_8);
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        
        // Divide o texto em linhas e adiciona no documento
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                document.add(new Paragraph(" "));
            } else {
                document.add(new Paragraph(line));
            }
        }
        
        document.close();
        return out.toByteArray();
    }

    private static byte[] convertImageToPdf(byte[] fileBytes) throws IOException, DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        
        // Obtém a instância da imagem a partir dos bytes
        Image image = Image.getInstance(fileBytes);
        
        // Ajusta o tamanho da imagem para caber na página respeitando as margens
        float pageWidth = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
        float pageHeight = document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin();
        image.scaleToFit(pageWidth, pageHeight);
        
        // Centraliza a imagem na página
        image.setAlignment(Image.ALIGN_CENTER);
        
        document.add(image);
        document.close();
        
        return out.toByteArray();
    }

    private static String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return fileName.substring(lastIndex + 1);
    }
}
