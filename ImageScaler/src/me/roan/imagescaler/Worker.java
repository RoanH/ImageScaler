package me.roan.imagescaler;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 * Class responsible for actually scaling all
 * the images using the indicated number of threads
 * @author Roan
 */
public class Worker {
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
				
		for(File img : files){
			executor.submit(()->{
				while(!running){
					try {
						System.out.println("Wait");
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
				try {
					scale(img);
				} catch (Exception e) {
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
		File out = new File(Main.outputDir, name);
		if(Main.overwrite || !out.exists()){
			BufferedImage img = ImageIO.read(file);
			Image scaled = img.getScaledInstance((int)Math.round((double)img.getWidth() * Main.scale), (int)Math.round((double)img.getHeight() * Main.scale), Main.mode.mode);
			name = Main.renameRegex.matcher(name.substring(name.startsWith(File.separator) ? 1 : 0, dot)).replaceAll(Main.renameReplace) + name.substring(dot);
			out.mkdirs();
			out.createNewFile();
			BufferedImage data = toBufferedImage(scaled);
			ImageIO.write(data, ext, out);
			img.flush();
			scaled.flush();
			data.flush();
		}
	}
	
	/**
	 * Converts the given image to a BufferedImage
	 * @param img The image to convert
	 * @return The converted image
	 */
	private static final BufferedImage toBufferedImage(Image img){
		BufferedImage buffer = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics g = buffer.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return buffer;
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
