package dev.roanh.imagescaler;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.twelvemonkeys.image.ResampleOp;

import dev.roanh.util.Dialog;

/**
 * Class responsible for actually scaling all
 * the images using the indicated number of threads
 * @author Roan
 */
public class Worker{
	/**
	 * Number of images that have been scaled so far
	 */
	private final AtomicInteger completed = new AtomicInteger(0);
	/**
	 * Whether or not we are currently running or paused
	 */
	private volatile boolean running = false;
	/**
	 * The directory to write rescaled images to
	 */
	private static Path outputDir;
	/**
	 * The directory to search for images to rescale
	 */
	private Path inputDir;
	/**
	 * List of files to scale
	 */
	private List<Path> files = new ArrayList<Path>();
	/**
	 * Regex that is used on all file names to optionally modify them
	 */
	private Pattern renameRegex;
	/**
	 * Replacement string for file name parts
	 * matched by the {@link #renameRegex} regex.
	 */
	private String renameReplace;
	/**
	 * The factor to scale the input images by
	 */
	private double scale;
	/**
	 * Whether or not to overwrite existing files
	 */
	private boolean overwrite;
	/**
	 * The scaling algorithm that is used
	 */
	private ScalingMode mode;

	/**
	 * Constructs a new rescaling working with the given configuration options and workload.
	 * @param input The input directory to read from, could be a single file.
	 * @param output The output directory to write to, should be <code>null</code>
	 *        when the input is a single file.
	 * @param subdirs Whether or not to parse subdirectories of the input directory.
	 * @param matchRegex Regex used to match the name of the files to convert.
	 * @param renameRegex Regex used to match the part of the file name to replace.
	 * @param replacement The replacement for the part matched by the rename regex.
	 * @param extensions Array of file extensions to parse.
	 * @param overwrite Whether or not to overwrite existing files (if applicable).
	 * @param mode The rescaling algorithm to use.
	 * @param scale The scaling factor to rescale the images by.
	 * @throws IOException When an IOException occurs.
	 */
	public Worker(Path input, Path output, boolean subdirs, Pattern matchRegex, Pattern renameRegex, String replacement, String[] extensions, boolean overwrite, ScalingMode mode, double scale) throws IOException{
		inputDir = input;
		outputDir = output;
		renameReplace = replacement;
		this.renameRegex = renameRegex;
		this.overwrite = overwrite;
		this.mode = mode;
		this.scale = scale;
		
		if(Files.isDirectory(input)){
			Files.find(input, subdirs ? Integer.MAX_VALUE : 1, (path, attr)->{
				if(attr.isRegularFile()){
					String name = path.getFileName().toString();
					int dot = name.lastIndexOf('.');
					if(dot != -1){
						String ext = name.substring(dot + 1);
						name = name.substring(0, dot);
						if(matchRegex.matcher(name).matches()){
							for(String e : extensions){
								if(e.equalsIgnoreCase(ext)){
									return true;
								}
							}
						}
					}
				}
				return false;
			}).forEach(files::add);
		}else if(Files.isRegularFile(input)){
			files = Collections.singletonList(input);
			inputDir = input.getParent();
			if(output == null){
				outputDir = inputDir;
			}
		}
	}
	
	/**
	 * Gets the number of files scheduled to be rescaled.
	 * @return The number of files to be rescaled.
	 */
	public int getWorkloadSize(){
		return files.size();
	}
	
	/**
	 * Sets whether the working is currently processing files.
	 * @param shouldRun True if the worker should process files.
	 */
	public final void setRunning(boolean shouldRun){
		running = shouldRun;
	}

	/**
	 * Checks if the worker is currently running and processing files.
	 * @return True if the worker is currently processing files.
	 */
	public boolean isRunning(){
		return running;
	}
	
	/**
	 * Starts the threads and rescales all the images.
	 * @param threads Number of rescale threads to use.
	 * @param listener The ProgressListener to notify of
	 *        any progress that is made.
	 */
	public final void start(int threads, ProgressListener listener){
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		completed.set(0);
		running = true;

		for(Path img : files){
			executor.submit(()->{
				while(!running){
					try{
						Thread.sleep(1000);
					}catch(InterruptedException e){
					}
				}
				
				try{
					scale(img);
				}catch(Exception e){
					synchronized(listener){
						listener.error(img, e);
					}
					e.printStackTrace();
				}catch(OutOfMemoryError e){
					//Note that this can only really have been caused by failing to secure enough memory
					//for the data structure backing the image. Given that the allocation failed the memory
					//most likely was never allocated in the first place. Therefore catching this error
					//should be relatively safe. And even if we do crash again later due to running
					//out of memory that is fine.
					System.gc();
					Dialog.showMessageDialog(
						"The program ran out of memory while scaling: "
						+ img.getFileName().toString()
						+ "\nIf this happens more often try lowering the 'Thread' value."
						+ "\n\nAlternatively you can try running the program with more RAM."
						+ "\n\nOn the off chance that you're running 32bit Java on a 64bit system"
						+ "\nswitching to 64bit Java will most likely fix the issue too."
					);
				}finally{
					synchronized(listener){
						int finished = completed.incrementAndGet();
						listener.progress(finished);
						if(finished == files.size()){
							listener.done();
							executor.shutdown();
						}
					}
				}
			});
		}
	}

	/**
	 * Rescales the given image in accordance with the
	 * selected options.
	 * @param file The image to rescale.
	 * @throws IOException When an IOException occurs.
	 */
	private final void scale(Path file) throws IOException{
		Path relative = inputDir.relativize(file);
		String name = relative.getFileName().toString();
		int dot = name.lastIndexOf('.');
		String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
		name = renameRegex.matcher(name.substring(0, dot)).replaceAll(renameReplace) + name.substring(dot);
		Path out = relative.getParent() == null ? outputDir.resolve(name) : outputDir.resolve(relative.getParent()).resolve(name);
		
		if(Double.compare(scale, 1.0D) == 0){
			Files.createDirectories(out.getParent());
			Files.copy(file, out, StandardCopyOption.REPLACE_EXISTING);
		}else if(overwrite || Files.notExists(out)){
			Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(ext);
			if(!readers.hasNext()){
				throw new IllegalArgumentException("Cannot read files with the " + ext + " extension.");
			}
			
			ImageReader reader = readers.next();
			try(InputStream inStream = Files.newInputStream(file)){
				try(ImageInputStream imageStream = ImageIO.createImageInputStream(inStream)){
					reader.setInput(imageStream);
					BufferedImage img = reader.read(0);
					BufferedImage output = new ResampleOp(
						Math.max(1, (int)Math.round(img.getWidth() * scale)),
						Math.max(1, (int)Math.round(img.getHeight() * scale)),
						mode.mode
					).filter(img, null);

					Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(ext);
					if(!writers.hasNext()){
						throw new IllegalArgumentException("Cannot write files with the " + ext + " extension.");
					}

					Files.createDirectories(out.getParent());
					ImageWriter writer = writers.next();
					try(OutputStream outStream = Files.newOutputStream(out)){
						try(ImageOutputStream stream = ImageIO.createImageOutputStream(outStream)){
							writer.setOutput(stream);
							writer.write(output);

							writer.dispose();
							stream.flush();
							reader.dispose();
							img.flush();
							output.flush();
						}
						outStream.flush();
					}
				}
			}
		}
	}

	/**
	 * Simple interface that gets called when progress was made.
	 * @author Roan
	 */
	public static abstract interface ProgressListener{
		
		/**
		 * Called when progress was made.
		 * @param completed Total number of files completed so far.
		 */
		public abstract void progress(int completed);
		
		/**
		 * Called when an error occurred.
		 * @param file The file that caused the error.
		 * @param e The exception.
		 */
		public abstract void error(Path file, Exception e);
		
		/**
		 * Called when all files have finished processing.
		 */
		public abstract void done();
	}
}
