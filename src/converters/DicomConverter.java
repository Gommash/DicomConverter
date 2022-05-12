package converters;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.pixelmed.display.ConsumerFormatImageMaker;

import helpers.FileHelper;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
* Dicom to JPG and PNG Converter Program
* 
* @author Anle1802, Miil1800
* 
*/
public class DicomConverter {
	private static boolean overwriteMode = false;
	private static int batchSize = 650; // Batch size
	private static AtomicInteger nConverts = new AtomicInteger(0); // counts how many directories that are converted
	private static int nDirs = 0; // Total number of directories in root directory
	/** 
	 * Dicom Program
	 * @throws ArgumentParserException  if path to directory is missing/invalid
	 * @param args
	 */
	public static void main(String[] args) {
		String path=""; // Path to search from
		// Parsing Arguments
		ArgumentParser parser = ArgumentParsers.newFor("DicomConverter").build()
	                .description("Recursively converts all .dcm files to JPG and PNG");
	        parser.addArgument("-s","--src")
	                .type(String.class)
	                .help("The directory path to search from");
	        parser.addArgument("-b","--bsize")
	        		.type(Integer.class)
	                .help("Number of directories for each task to search through");
	        parser.addArgument("-f","--force")
	        	.action(Arguments.storeTrue())
	        	.setDefault(false)
            	.help("forcing overwriting the jpg and png files");
	        try {
	            Namespace res = parser.parseArgs(args);
	            path = res.get("src");
	            System.out.println(path);
	            if(!new File(path).exists() && !new File(path).isDirectory())
	            	throw new ArgumentParserException("path must be a valid directory!",parser);
	            if(res.get("bsize")!=null)
	            	batchSize = res.get("bsize");
	            overwriteMode = res.get("force");
	        } catch (ArgumentParserException e) {
	            parser.handleError(e);
	            System.exit(1);
	        }
	    // Run the converter
	    if(!path.isEmpty()) {
	    	System.out.println("Starting the Dicom Converter...");
	    	convertDICOM(path);
	    }
	}
	
	/**
	 * Starting Dicom Converter Tasks
	 * @param dir Directory path to search from
	 */
	private static void convertDICOM(String dir) {
		File[] files = new File(dir).listFiles();
		nDirs = files.length;
		ExecutorService pool = Executors.newCachedThreadPool();
		
		// Dividing the workload into batches.
		// number of tasks = number of {files and dirs} divided by batchsize
		int i=0;
		while(i<files.length) {
			List<String> paths = new ArrayList<String>();
			for(int k = 0; k < batchSize; k++) {
				if(i<files.length) {
					paths.add(files[i].getPath());
					i++;
				}
				else
					break;
			}
			if(paths.size() > 0) {
				pool.execute(new DicomConverterTask(paths));
			}
		}
		pool.shutdown();
	}
	
	/**
	 * DicomConverterTask - Searching recursively for .dcm files to convert into JPG and PNG files
	 */
	public static class DicomConverterTask implements Runnable   
	{
	    private List<String> dirs; // The directories / Files to search through
	      
	    public DicomConverterTask(List<String>dirs)
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
				read_files(directory);
				nConverts.incrementAndGet();
				nDir++;
				if(nDir % 4 == 0) {	
					System.out.println("Current Thread ID-" + Thread.currentThread().getId() + " For Thread-" + Thread.currentThread().getName()+", Progress: "+nDir+"/"+dirs.size()+", "+((float)(nDir / (float)dirs.size())*100)+"%"+", Total Progress: "+nConverts.get()+"/"+nDirs+", "+((float)(nConverts.get() / (float)nDirs)*100)+"%");
				}
			}
			System.out.println("Current Thread ID-" + Thread.currentThread().getId() + " For Thread-" + Thread.currentThread().getName()+", Has completed, Total Progress: "+nConverts.get()+"/"+nDirs+", "+((float)(nConverts.get() / (float)nDirs)*100)+"%");
	    }
	    
		private void convert_image_file(String imgPath) {
	        try {
	        	if(overwriteMode || !FileHelper.filetypeExists(imgPath+".jpg", "jpg"))
	        		ConsumerFormatImageMaker.convertFileToEightBitImage(imgPath+".dcm", imgPath+".jpg", "jpeg",-1,-1,-1,-1,-1,-1,-1, -1, -1, -1,100, ConsumerFormatImageMaker.NO_ANNOTATIONS);
	        	if(overwriteMode || !FileHelper.filetypeExists(imgPath+".png", "png"))
	        	ConsumerFormatImageMaker.convertFileToEightBitImage(imgPath+".dcm", imgPath+".png", "png",-1,-1,-1,-1,-1,-1,-1, -1, -1, -1,100, ConsumerFormatImageMaker.NO_ANNOTATIONS);
	        } catch (Exception e) {
				e.printStackTrace();
			}
		}
	    
	    private void read_files(String path) {
	    	try {
	    	File f = new File(path);
	    	if(f.exists()) {
	    		if(f.isFile()) {
	    			Optional<String> ext = FileHelper.getFileExtension(f.getPath());
					if( !ext.isEmpty() && ext.get().equals("dcm")) {
						convert_image_file(f.getPath().substring(0,f.getPath().lastIndexOf(".")));
					}
	    		}
	    		else if(f.isDirectory()) {
	    	    	for(File file : new File(path).listFiles()) {
	    				read_files(file.getPath());
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
