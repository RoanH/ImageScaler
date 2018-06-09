package me.roan.imagescaler;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main {
	
	private static File outputDir = new File("test");
	private static File inputDir;
	private static double scale = 0.5D;
	private static boolean overwrite = false;
	private static ScalingMode mode = ScalingMode.QUALITY;

	public static void main(String[] args){
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(null);
		inputDir = chooser.getSelectedFile();
		for(File fn : getImages(inputDir)){
			System.out.println(fn.getName());
			try {
				scale(fn);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
		
		JFrame frame = new JFrame("Image Scaler");
		JPanel panel = new JPanel();
		
		
	}
	
	private static final void scale(File file) throws IOException{
		BufferedImage img = ImageIO.read(file);
		Image scaled = img.getScaledInstance((int)Math.round((double)img.getWidth() * scale), (int)Math.round((double)img.getHeight() * scale), mode.mode);
		String name = file.getAbsolutePath().replace(inputDir.getAbsolutePath(), "");
		int dot = name.lastIndexOf('.');
		String ext = name.substring(dot + 1);
		name = name.substring(name.startsWith(File.separator) ? 1 : 0, dot - 3) + name.substring(dot);
		File out = new File(outputDir, name);
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
	
	private static final List<File> getImages(File dir){
		List<File> list = new ArrayList<File>();
		for(File file : dir.listFiles()){
			if(file.isDirectory()){
				list.addAll(getImages(file));
			}else if(file.getName().matches(".+@2x\\..*")){
				list.add(file);
			}
		}
		return list;
	}
}
