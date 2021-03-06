package io.projectenv.core.commons.archive.impl.accessor;

import io.projectenv.core.commons.archive.impl.accessor.tar.TarArchiveAccessor;
import io.projectenv.core.commons.archive.impl.accessor.zip.ZipArchiveAccessor;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;

import java.io.*;
import java.util.Map;

public class ArchiveAccessorFactory {

    private static final Map<String, ArchiveSpecificAccessorFactory> FACTORIES = Map.of(
            ".tar.gz", ArchiveAccessorFactory::createTarGzArchiveAccessor,
            ".tar.xz", ArchiveAccessorFactory::createTarXzArchiveAccessor,
            ".tar", ArchiveAccessorFactory::createTarArchiveAccessor,
            ".zip", ArchiveAccessorFactory::createZipArchiveAccessor
    );

    public static ArchiveAccessor createArchiveAccessor(File archive) throws IOException {
        for (Map.Entry<String, ArchiveSpecificAccessorFactory> factoryEntry : FACTORIES.entrySet()) {
            if (archive.getName().toLowerCase().endsWith(factoryEntry.getKey())) {
                return factoryEntry.getValue().createArchiveAccessor(archive);
            }
        }

        throw new IllegalArgumentException("unsupported archive " + archive.getName());
    }

    private static ArchiveAccessor createZipArchiveAccessor(File archive) throws IOException {
        var zipFile = new ZipFile(archive);

        return new ZipArchiveAccessor(zipFile);
    }

    private static ArchiveAccessor createTarXzArchiveAccessor(File archive) throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new FileInputStream(archive));
        InputStream tarInputStream = new BufferedInputStream(new XZCompressorInputStream(originalInputStream));

        return createTarArchiveAccessor(tarInputStream);
    }

    private static ArchiveAccessor createTarGzArchiveAccessor(File archive) throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new FileInputStream(archive));
        InputStream tarInputStream = new BufferedInputStream(new GzipCompressorInputStream(originalInputStream));

        return createTarArchiveAccessor(tarInputStream);
    }

    private static ArchiveAccessor createTarArchiveAccessor(File archive) throws IOException {
        InputStream originalInputStream = new BufferedInputStream(new FileInputStream(archive));

        return createTarArchiveAccessor(originalInputStream);
    }

    private static ArchiveAccessor createTarArchiveAccessor(InputStream tarInputStream) {
        var tarArchiveInputStream = new TarArchiveInputStream(tarInputStream);

        return new TarArchiveAccessor(tarArchiveInputStream);
    }

    private interface ArchiveSpecificAccessorFactory {

        ArchiveAccessor createArchiveAccessor(File archive) throws IOException;

    }


}
