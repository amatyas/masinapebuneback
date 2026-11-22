package com.caradchecker.springbatch.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Preconditions;

public final class ZipUtil {

    private ZipUtil() {
    }

    public static void compressDirectory(File sourceDirectory, File zipFile) throws IOException {
        Preconditions.checkState(sourceDirectory.exists(), "Source directory is not exists: %s", sourceDirectory);
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            compressDirectory(sourceDirectory.getAbsoluteFile(), sourceDirectory, out);
        }
    }

    private static void compressDirectory(File rootDir, File sourceDir, ZipOutputStream out) throws IOException {
        for (File file : Preconditions.checkNotNull(sourceDir.listFiles())) {
            if (file.isDirectory()) {
                compressDirectory(rootDir, new File(sourceDir, file.getName()), out);
            } else {
                String zipEntryName = getRelativeZipEntryName(rootDir, file);
                compressFile(out, file, zipEntryName);
            }
        }
    }

    private static String getRelativeZipEntryName(File rootDir, File file) {
        return StringUtils.removeStart(file.getAbsolutePath(), rootDir.getAbsolutePath());
    }

    public static void compressFile(File file, File zipFile) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            compressFile(out, file, file.getName());
        }
    }

    private static void compressFile(ZipOutputStream out, File file, String zipEntityName) throws IOException {
        ZipEntry entry = new ZipEntry(zipEntityName);
        out.putNextEntry(entry);

        try (FileInputStream in = new FileInputStream(file)) {
            IOUtils.copy(in, out);
        }
    }

    public static List<File> extractArchive(File targetDirectory, File zipFile) throws IOException {
    	List<File> extractedFiles = new ArrayList<File>();
    	FileInputStream fis = null;
    	ZipInputStream zis = null;
    	try { 
	    	fis = new FileInputStream(zipFile);
	    	zis = new ZipInputStream(fis);
    		extractedFiles = extractStream(targetDirectory, zis);
        }
	    finally {
	    	if(fis != null) {
	    		try { fis.close(); } catch(Exception e) {}
	    	}
	    	if(zis != null) {
	    		try { zis.close(); } catch(Exception e) {}
	    	}
	    }
    	return extractedFiles;
    }

    private static List<File>  extractStream(File targetDirectory, ZipInputStream zis) throws IOException {
        ZipEntry zipEntry = zis.getNextEntry();
        List<File> extractedFiles = new ArrayList<File>();
        while (zipEntry != null) {
            File fileExtracted = extractEntry(targetDirectory, zis, zipEntry);
            extractedFiles.add(fileExtracted);
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        return extractedFiles; 
    }

    private static File extractEntry(File targetDirectory, ZipInputStream zis, ZipEntry zipEntry) throws IOException {
        File newFile = newFile(targetDirectory, zipEntry);
        if (zipEntry.isDirectory()) {
            FileUtils.forceMkdir(newFile);
        } else {
            FileUtils.forceMkdirParent(newFile);
            try (FileOutputStream fos = new FileOutputStream(newFile)) {
                IOUtils.copy(zis, fos);
            }
        }
        return newFile; 
    }

    private static File newFile(File targetDirectory, ZipEntry zipEntry) throws IOException {
        File targetFile = new File(targetDirectory, zipEntry.getName());

        String targetDirPath = targetDirectory.getCanonicalPath();
        String targetFilePath = targetFile.getCanonicalPath();

        if (!targetFilePath.startsWith(targetDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return targetFile;
    }
}