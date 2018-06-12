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
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class Worker {
	
	protected static final AtomicInteger completed = new AtomicInteger(0);
	protected static volatile boolean running = false;
	private static List<File> files;
	
	public static final int prepare() throws PatternSyntaxException{
		files = getImages(Main.inputDir);
		return files.size();
	}

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
				} catch (IOException e) {
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
	
	@FunctionalInterface
	protected static abstract interface ProgressListener{
		
		public abstract void progress();
	}
	
	private static final void scale(File file) throws IOException{
		BufferedImage img = ImageIO.read(file);
		Image scaled = img.getScaledInstance((int)Math.round((double)img.getWidth() * Main.scale), (int)Math.round((double)img.getHeight() * Main.scale), Main.mode.mode);
		String name = file.getAbsolutePath().replace(Main.inputDir.getAbsolutePath(), "");
		int dot = name.lastIndexOf('.');
		String ext = name.substring(dot + 1);
		name = Main.renameRegex.matcher(name.substring(name.startsWith(File.separator) ? 1 : 0, dot)).replaceAll(Main.renameReplace) + name.substring(dot);
		File out = new File(Main.outputDir, name);
		out.mkdirs();
		out.createNewFile();
		BufferedImage data = toBufferedImage(scaled);
		ImageIO.write(data, ext, out);
		img.flush();
		scaled.flush();
		data.flush();
	}
	
	private static final BufferedImage toBufferedImage(Image img){
		BufferedImage buffer = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics g = buffer.createGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		return buffer;
	}
	
	private static final List<File> getImages(File dir) throws PatternSyntaxException{
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
