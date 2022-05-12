package helpers;

import java.io.File;
import java.util.Optional;

public class FileHelper {
	/**
	 * Returns the file extension
	 * @param filepath - The complete file path
	 * @return File Extension
	 */
	public static Optional<String> getFileExtension(String filepath) {
	    return Optional.ofNullable(filepath)
	      .filter(f -> f.contains("."))
	      .map(f -> f.substring(filepath.lastIndexOf(".") + 1));
	}
	/**
	 * Does files with the filetype exist in the directory?
	 * @param path a directory path
	 * @param filetype The filetype to check for
	 * @return true - file(s) of that filetype exists, false - the directory does not contain any files with that filetype
	 */
	public static boolean filetypeExists(String path, String filetype) {
		File f = new File(path);
		if(f.exists()) {
			if(f.isFile())
				return getFileExtension(f.getPath()).get().equals(filetype);
			else if(f.isDirectory()) {
				for(File file : f.listFiles()) {
					if(file.isFile() && getFileExtension(file.getPath()).get().equals(filetype))
						return true;
				}
			}
		}
		return false;
	}
}
