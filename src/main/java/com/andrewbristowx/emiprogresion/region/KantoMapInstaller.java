package com.andrewbristowx.emiprogresion.region;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Installs the versioned Wild Kanto chunks before any server levels are loaded. */
public final class KantoMapInstaller {
    public static final String MAP_VERSION = "clean-alpha1";
    public static final String ARCHIVE_NAME = "WildKanto-EMIPOKEMON-clean-alpha1.zip";
    public static final String DEFAULT_URL = "https://github.com/andrewBristowx/EmiProgresion/releases/download/wild-kanto-clean-alpha1/" + ARCHIVE_NAME;
    public static final String EXPECTED_SHA256 = "F9B66243F9E26F9ED248D05149B473C3140C22407E00697EF90A8FB83D596321";
    private static final String MARKER_NAME = "emiprogresion-wild-kanto.txt";
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss")
            .withLocale(Locale.ROOT).withZone(ZoneOffset.UTC);
    private static volatile InstallState state = InstallState.IDLE;
    private static volatile String detail = "Pendiente del primer arranque del servidor.";
    private static volatile Path lastBackup;

    private KantoMapInstaller() {}

    public static void onServerStarting(MinecraftServer server) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        if (!config.autoInstallWildKanto) {
            update(InstallState.DISABLED, "Instalación automática desactivada en la configuración.");
            return;
        }
        if (!server.isDedicatedServer() && !config.installWildKantoOnIntegratedServer) {
            update(InstallState.DISABLED, "La descarga automática solo se ejecuta en servidores dedicados.");
            return;
        }

        Path worldRoot = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        Path destination;
        try {
            destination = dimensionPath(worldRoot, config.kantoWorld);
        } catch (IllegalArgumentException invalidDimension) {
            update(InstallState.FAILED, invalidDimension.getMessage());
            EmiProgresion.LOGGER.error("Wild Kanto automatic installation failed", invalidDimension);
            return;
        }

        try {
            if (Files.isRegularFile(destination.resolve(MARKER_NAME))) {
                update(InstallState.ALREADY_INSTALLED, "Wild Kanto " + MAP_VERSION + " ya estaba instalado.");
                return;
            }
            if (looksLikeCompleteManualImport(destination)) {
                writeMarker(destination, "importación manual detectada", config.wildKantoExpectedSha256);
                update(InstallState.ALREADY_INSTALLED, "Se detectó una importación completa existente; no se reemplazó.");
                return;
            }
            install(config, worldRoot, destination);
        } catch (Exception failure) {
            update(InstallState.FAILED, "No se pudo instalar Wild Kanto: " + conciseMessage(failure));
            EmiProgresion.LOGGER.error("Wild Kanto automatic installation failed; Kanto will remain locked", failure);
        }
    }

    private static void install(EmiProgresionConfig config, Path worldRoot, Path destination) throws Exception {
        Path temporary = Files.createTempDirectory(worldRoot, ".emiprogresion-kanto-install-");
        Path stagedDimension = temporary.resolve("dimension");
        Path archive = localArchive();
        boolean downloaded = false;
        try {
            if (!Files.isRegularFile(archive)) {
                archive = temporary.resolve(ARCHIVE_NAME);
                update(InstallState.DOWNLOADING, "Descargando Wild Kanto " + MAP_VERSION + "…");
                download(config.wildKantoDownloadUrl, archive);
                downloaded = true;
            } else {
                EmiProgresion.LOGGER.info("Using local Wild Kanto archive at {}", archive);
            }

            String actualSha = KantoMapArchive.sha256(archive);
            String expectedSha = config.wildKantoExpectedSha256.trim().toUpperCase(Locale.ROOT);
            if (!actualSha.equals(expectedSha)) {
                throw new IOException("SHA-256 incorrect; esperado " + expectedSha + " y recibido " + actualSha + ".");
            }

            update(InstallState.EXTRACTING, "Verificación correcta; extrayendo region, entities y poi…");
            KantoMapArchive.ExtractionResult result = KantoMapArchive.extractDimension(archive, stagedDimension);
            writeMarker(stagedDimension, "instalación automática", actualSha);
            installAtomically(worldRoot, stagedDimension, destination);
            config.storyNpcSetupComplete = false;
            EmiProgresionConfig.save();
            update(InstallState.INSTALLED, "Wild Kanto instalado: " + result.entries() + " archivos, "
                    + formatMegabytes(result.bytes()) + " MB extraídos.");
            EmiProgresion.LOGGER.info("Wild Kanto {} installed in {} with {} files", MAP_VERSION, destination, result.entries());
        } finally {
            try {
                KantoMapArchive.deleteTree(temporary);
            } catch (IOException cleanupFailure) {
                EmiProgresion.LOGGER.warn("Could not remove temporary Wild Kanto installer directory {}", temporary, cleanupFailure);
            }
            if (downloaded) EmiProgresion.LOGGER.info("Removed the verified temporary Wild Kanto download after installation attempt.");
        }
    }

    private static void download(String url, Path destination) throws Exception {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IOException("La URL automática debe usar HTTPS.");
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(45))
                .header("User-Agent", "EmiProgresion/0.1.0-alpha.5")
                .GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            try (InputStream ignored = response.body()) {
                throw new IOException("La descarga respondió HTTP " + response.statusCode() + ".");
            }
        }
        long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
        if (declared > KantoMapArchive.MAX_ARCHIVE_BYTES) {
            try (InputStream ignored = response.body()) {
                throw new IOException("La descarga anunciada supera el límite de seguridad.");
            }
        }

        long total = 0L;
        long nextLog = 64L * 1024L * 1024L;
        try (InputStream input = response.body();
             OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read == 0) continue;
                total += read;
                if (total > KantoMapArchive.MAX_ARCHIVE_BYTES) {
                    throw new IOException("La descarga superó el límite de seguridad.");
                }
                output.write(buffer, 0, read);
                if (total >= nextLog) {
                    EmiProgresion.LOGGER.info("Wild Kanto download: {} MB", formatMegabytes(total));
                    nextLog += 64L * 1024L * 1024L;
                }
            }
        }
        if (declared >= 0L && total != declared) {
            throw new IOException("Descarga incompleta: " + total + " de " + declared + " bytes.");
        }
        EmiProgresion.LOGGER.info("Wild Kanto download completed: {} MB", formatMegabytes(total));
    }

    static Path installAtomically(Path worldRoot, Path stagedDimension, Path destination) throws IOException {
        Path backup = null;
        if (Files.exists(destination)) {
            Path backupRoot = worldRoot.resolve("emiprogresion-backups")
                    .resolve("kanto-before-auto-" + BACKUP_TIME.format(Instant.now()));
            Files.createDirectories(backupRoot.getParent());
            move(destination, backupRoot);
            backup = backupRoot;
            lastBackup = backupRoot;
            EmiProgresion.LOGGER.warn("Existing Kanto dimension moved safely to {}", backupRoot);
        }
        try {
            Files.createDirectories(destination.getParent());
            move(stagedDimension, destination);
        } catch (IOException installFailure) {
            if (backup != null && Files.exists(backup) && !Files.exists(destination)) {
                try {
                    move(backup, destination);
                } catch (IOException rollbackFailure) {
                    installFailure.addSuppressed(rollbackFailure);
                }
            }
            throw installFailure;
        }
        return backup;
    }

    private static void move(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, destination);
        }
    }

    private static Path dimensionPath(Path worldRoot, String dimensionId) {
        ResourceLocation id = ResourceLocation.tryParse(dimensionId);
        if (id == null || "minecraft".equals(id.getNamespace())) {
            throw new IllegalArgumentException("La dimensión Kanto configurada no es válida: " + dimensionId);
        }
        Path dimensions = worldRoot.resolve("dimensions");
        Path destination = dimensions.resolve(id.getNamespace()).resolve(id.getPath()).normalize();
        if (!destination.startsWith(dimensions.normalize())) {
            throw new IllegalArgumentException("La ruta de la dimensión Kanto no es segura.");
        }
        return destination;
    }

    private static boolean looksLikeCompleteManualImport(Path destination) throws IOException {
        return countFiles(destination.resolve("region")) >= 100
                && countFiles(destination.resolve("entities")) >= 40
                && countFiles(destination.resolve("poi")) >= 80
                && Files.isRegularFile(destination.resolve("region").resolve("r.0.0.mca"));
    }

    private static long countFiles(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) return 0L;
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private static Path localArchive() {
        return FabricLoader.getInstance().getConfigDir().resolve("emiprogresion").resolve(ARCHIVE_NAME);
    }

    private static void writeMarker(Path destination, String method, String sha256) throws IOException {
        Files.createDirectories(destination);
        String text = "Wild Kanto by RobotJoel\n"
                + "Version: " + MAP_VERSION + "\n"
                + "Method: " + method + "\n"
                + "SHA-256: " + sha256 + "\n"
                + "Source: https://www.planetminecraft.com/project/wild-kanto-a-cobblemon-region-map/\n"
                + "License: CC BY-NC-SA 4.0\n";
        Files.writeString(destination.resolve(MARKER_NAME), text);
    }

    private static String conciseMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private static long formatMegabytes(long bytes) {
        return Math.round(bytes / (1024.0D * 1024.0D));
    }

    private static void update(InstallState newState, String newDetail) {
        state = newState;
        detail = newDetail;
        EmiProgresion.LOGGER.info("Wild Kanto installer [{}]: {}", newState, newDetail);
    }

    public static InstallState state() {
        return state;
    }

    public static String detail() {
        return detail;
    }

    public static Path lastBackup() {
        return lastBackup;
    }

    public enum InstallState {
        IDLE, DISABLED, ALREADY_INSTALLED, DOWNLOADING, EXTRACTING, INSTALLED, FAILED
    }
}
