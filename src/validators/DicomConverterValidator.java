package validators;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import helpers.FileHelper;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class DicomConverterValidator {

	private static int batchSize = 650;




	public static void main(String[] args) {
		String path=""; // Path to search from
		// Parsing Arguments
		ArgumentParser parser = ArgumentParsers.newFor("DicomConverter").build()
	                .description("Recursively converts all .dcm files to JPG and PNG");
	        parser.addArgument("-s","--src")
	                .type(String.class)
	                .help("The directory path to search from");
	        parser.addArgument("-b","--batchsize")
            .type(Integer.class)
            .setDefault(650)
            .help("Number of directories for each task to search through");
	        try {
	            Namespace res = parser.parseArgs(args);
	            path = res.get("src");
	            batchSize = res.get("batchsize");
	            
	            if(!new File(path).exists() && !new File(path).isDirectory())
	            	throw new ArgumentParserException("path must be a valid directory!",parser);
	            
	        } catch (ArgumentParserException e) {
	            parser.handleError(e);
	            System.exit(1);
	        }
	    // Run the converter
	    if(!path.isEmpty()) {
	    	System.out.println("Starting the Dicom Validator...");
	    	exploredirs(path);
	    }
	}
	
	private static void exploredirs(String dir) {
		File[] files = new File(dir).listFiles();
		ExecutorService pool = Executors.newCachedThreadPool();
		
		// Dividing the workload into batches.
		// number of tasks = number of {files and dirs} divided by batchsize
		int i=0;
		while(i<files.length) {
			List<String> paths = new ArrayList<String>();
			for(int k = 0; k < batchSize ; k++) {
				if(i<files.length) {
					paths.add(files[i].getPath());
					i++;
				}
				else
					break;
			}
			if(paths.size() > 0) {
				pool.execute(new ImageSearchTask(paths));
			}
		}
		pool.shutdown();	
	}


	
	
	public static class ImageSearchTask implements Runnable   
	{
	    private List<String> dirs; // The directories / Files to search through
	      
	    public ImageSearchTask(List<String>dirs)
	    {
	    	this.dirs = dirs;
	    }

	    /**
	     * 
	     */
	    public void run()
	    {
	    	int nDir = 0;
			for(String directory : dirs) {
				search_files(directory);
				nDir++;
				if(nDir % 4 == 0 || nDir == dirs.size()) {	
					System.out.println("Current Thread ID-" + Thread.currentThread().getId() + " For Thread-" + Thread.currentThread().getName()+", Progress: "+nDir+"/"+dirs.size()+", "+((float)(nDir / (float)dirs.size())*100)+"%");
				}
			}
	    }
	    
		public static void search_files(String path) {
	    	try {
	    	File f = new File(path);
	    	if(f.exists()) {
	    		if(f.isFile()) {
	    			Optional<String> ext = FileHelper.getFileExtension(f.getPath());
					if( !ext.isEmpty() && ext.get().equals("dcm")) {
						String fpath = f.getPath().substring(0,f.getPath().lastIndexOf("."));
						if(!FileHelper.filetypeExists(fpath+".png", "png") || !FileHelper.filetypeExists(fpath+".jpg", "jpg")) {
							System.err.println(fpath +".dcm, is not converted!");
						}
					}
	    		}
	    		else if(f.isDirectory()) {
	    	    	for(File file : new File(path).listFiles()) {
	    	    		search_files(file.getPath());
	    			}
	    		}
	    	}
	    	}catch(Exception e) {
	    		File errFile = new File("err.txt");
	    		if(!errFile.exists()) {
	    			try {
						errFile.createNewFile();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
	    		}
	    		String errMsg = "Error occured while exploring the path: "+path+"\n";
	    	    try {
	    	        FileWriter fwriter = new FileWriter(errFile.getPath());
	    	        fwriter.write(errMsg);
	    	        fwriter.close();
	    	      } catch (IOException e3) {
	    	        e3.printStackTrace();
	    	      }
	    		System.err.println(errMsg);	    		
	    	}
		}
	}
}
