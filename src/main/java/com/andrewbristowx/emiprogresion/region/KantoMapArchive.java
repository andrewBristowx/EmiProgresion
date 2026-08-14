package com.andrewbristowx.emiprogresion.region;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Safe, Minecraft-independent handling of the versioned Wild Kanto archive. */
final class KantoMapArchive {
    static final long MAX_ARCHIVE_BYTES = 900L * 1024L * 1024L;
    static final long MAX_EXTRACTED_BYTES = 1_200L * 1024L * 1024L;
    static final long MAX_ENTRY_BYTES = 64L * 1024L * 1024L;
    static final int MAX_ENTRIES = 1_000;
    private static final Set<String> INCLUDED_DIRECTORIES = Set.of("region", "entities", "poi");

    private KantoMapArchive() {}

    static String sha256(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest()).toUpperCase();
    }

    static ExtractionResult extractDimension(Path archive, Path destination) throws IOException {
        if (Files.size(archive) > MAX_ARCHIVE_BYTES) {
            throw new IOException("El archivo de Kanto supera el límite de seguridad.");
        }
        Files.createDirectories(destination);
        Map<String, Integer> counts = new TreeMap<>();
        long totalBytes = 0L;
        int totalEntries = 0;

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.indexOf('\\') >= 0) {
                    throw new IOException("Entrada ZIP inválida: " + name);
                }
                String[] parts = name.split("/");
                if (parts.length < 2 || !INCLUDED_DIRECTORIES.contains(parts[1])) continue;

                Path relative = Path.of(parts[1]);
                for (int index = 2; index < parts.length; index++) {
                    if (!parts[index].isEmpty()) relative = relative.resolve(parts[index]);
                }
                Path output = destination.resolve(relative).normalize();
                if (!output.startsWith(destination.normalize())) {
                    throw new IOException("La entrada ZIP intenta salir del directorio de instalación: " + name);
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                    continue;
                }
                if (++totalEntries > MAX_ENTRIES) {
                    throw new IOException("El archivo de Kanto contiene demasiadas entradas.");
                }
                long declaredSize = entry.getSize();
                if (declaredSize > MAX_ENTRY_BYTES) {
                    throw new IOException("Entrada demasiado grande: " + name);
                }
                Files.createDirectories(output.getParent());
                long written = copyLimited(zip.getInputStream(entry), output, MAX_ENTRY_BYTES);
                totalBytes += written;
                if (totalBytes > MAX_EXTRACTED_BYTES) {
                    throw new IOException("La extracción de Kanto supera el límite de seguridad.");
                }
                counts.merge(parts[1], 1, Integer::sum);
            }
        } catch (IOException | RuntimeException failure) {
            deleteTree(destination);
            throw failure;
        }

        for (String directory : INCLUDED_DIRECTORIES) {
            if (counts.getOrDefault(directory, 0) == 0) {
                deleteTree(destination);
                throw new IOException("El archivo no contiene la carpeta requerida: " + directory);
            }
        }
        Path spawnRegion = destination.resolve("region").resolve("r.0.0.mca");
        if (!Files.isRegularFile(spawnRegion) || Files.size(spawnRegion) < 1024L) {
            deleteTree(destination);
            throw new IOException("Falta la región que contiene Pueblo Paleta.");
        }
        return new ExtractionResult(totalEntries, totalBytes, Map.copyOf(counts));
    }

    private static long copyLimited(InputStream source, Path destination, long limit) throws IOException {
        long total = 0L;
        try (InputStream input = source;
             OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) continue;
                total += read;
                if (total > limit) throw new IOException("Entrada ZIP expandida por encima del límite.");
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    record ExtractionResult(int entries, long bytes, Map<String, Integer> counts) {}
}
