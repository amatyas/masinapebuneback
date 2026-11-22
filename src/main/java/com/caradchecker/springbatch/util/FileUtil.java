package com.caradchecker.springbatch.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

	private final static Logger log = LoggerFactory.getLogger(FileUtil.class);

	
	public static List<File> listFilesWithLimit(String filePath, int maxFiles, FilenameFilter fn) throws IOException {
		int i = 1;
		long start = System.currentTimeMillis();
		Path dir = FileSystems.getDefault().getPath(filePath);
		
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path entry) throws IOException {
            	File f = entry.toFile();
            	return fn.accept(f.getParentFile(), f.getName());
            }
        };
		
		DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter);
		List<File> filesToReturn = new ArrayList<File>();
		for (Path path : stream) {
			filesToReturn.add(path.toFile());
			if (++i > maxFiles)
				break;
		}
		stream.close();
		long stop = System.currentTimeMillis();
		//log.info("Scanning folder: "+filePath+". Fetched "+filesToReturn.size()+" files. Took:"+(stop-start)+" millis");
		return filesToReturn;
	}
	
}
