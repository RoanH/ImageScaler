package me.roan.imagescaler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JOptionPane;

import com.twelvemonkeys.image.ResampleOp;

/**
 * Class responsible for actually scaling all
 * the images using the indicated number of threads
 * @author Roan
 */
public class Worker{
	/**
	 * Number of images that have been scaled so far
	 */
	protected static final AtomicInteger completed = new AtomicInteger(0);
	/**
	 * Whether or not we are currently running or paused
	 */
	protected static volatile boolean running = false;
	/**
	 * List of files to scale
	 */
	private static List<File> files;

	/**
	 * Reads the list of images to rescale
	 * from the selected input directory
	 * and returns the number of images
	 * that are going to be rescaled
	 * @return The number of images that
	 *         are going to be rescaled
	 */
	public static final int prepare(){
		files = getImages(Main.inputDir);
		return files.size();
	}

	/**
	 * Starts the threads and rescales all the images
	 * @param listener The ProgressListener to notify of
	 *        any progress that's made
	 */
	public static final void start(ProgressListener listener){
		ExecutorService executor = Executors.newFixedThreadPool(Main.threads);
		completed.set(0);
		running = true;

		Main.outputDir.mkdirs();

		for(File img : files){
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
					JOptionPane.showMessageDialog(null, "An internal error occurred while scaling: " + img.getName() + "\n" + e.getMessage(), "Image Scaler", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
				if(completed.incrementAndGet() == files.size()){
					executor.shutdown();
				}
				listener.progress();
			});
		}
	}

	/**
	 * Simply interface that gets called when progress was made
	 * @author Roan
	 */
	@FunctionalInterface
	protected static abstract interface ProgressListener{
		/**
		 * Called when progress was made
		 */
		public abstract void progress();
	}

	/**
	 * Rescales the given image in accordance with the
	 * selected options.
	 * @param file The image to rescale
	 * @throws IOException When an IOException occurs
	 */
	private static final void scale(File file) throws IOException{
		String name = file.getAbsolutePath().replace(Main.inputDir.getAbsolutePath(), "");
		int dot = name.lastIndexOf('.');
		String ext = name.substring(dot + 1);
		name = Main.renameRegex.matcher(name.substring(name.startsWith(File.separator) ? 1 : 0, dot)).replaceAll(Main.renameReplace) + name.substring(dot);
		File out = new File(Main.outputDir, name);
		if(Main.overwrite || !out.exists()){
			Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(ext);
			if(!readers.hasNext()){
				throw new IllegalArgumentException("Cannot read files with the " + ext + " extension.");
			}
			ImageReader reader = readers.next();
			reader.setInput(ImageIO.createImageInputStream(file));
			BufferedImage img = reader.read(0);
			BufferedImage output = new ResampleOp((int)Math.round(img.getWidth() * Main.scale), (int)Math.round(img.getHeight() * Main.scale), Main.mode.mode).filter(img, null);
			out.createNewFile();
			Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(ext);
			if(!writers.hasNext()){
				throw new IllegalArgumentException("Cannot write files with the " + ext + " extension.");
			}
			ImageWriter writer = writers.next();
			ImageOutputStream stream = ImageIO.createImageOutputStream(out);
			writer.setOutput(stream);
			writer.write(output);
			writer.dispose();
			stream.flush();
			stream.close();
			reader.dispose();
			img.flush();
			output.flush();
		}
	}

	/**
	 * Gets a list of all the files in the
	 * input folder that math the filter regex
	 * @param dir The directory to search
	 * @return The list of matching image files
	 */
	private static final List<File> getImages(File dir){
		List<File> list = new ArrayList<File>();
		for(File file : dir.listFiles()){
			if(file.isDirectory()){
				list.addAll(getImages(file));
			}else if(Main.regex.matcher(file.getName()).matches()){
				list.add(file);
			}
		}
		return list;
	}
}
