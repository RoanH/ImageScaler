package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

public class Main {
	
	private static File outputDir = new File("test");
	private static File inputDir;
	private static double scale = 0.5D;
	private static boolean overwrite = false;
	private static ScalingMode mode = ScalingMode.QUALITY;
	private static JFileChooser chooser;

	public static void main(String[] args){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
		}
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		JFrame frame = new JFrame("Image Scaler");
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JPanel input = new JPanel();
		input.setBorder(BorderFactory.createTitledBorder("Input Folder"));
		
		JPanel output = new JPanel();
		output.setBorder(BorderFactory.createTitledBorder("Output Folder"));
		
		JPanel options = new JPanel(new BorderLayout());
		options.setBorder(BorderFactory.createTitledBorder("Options"));
		JCheckBox over = new JCheckBox("Overwrite existing files?", overwrite);
		JComboBox<ScalingMode> mode = new JComboBox<ScalingMode>(ScalingMode.values());
		mode.setSelectedItem(Main.mode);
		options.add(over, BorderLayout.PAGE_START);
		options.add(new JLabel("Scaling algorithm: "), BorderLayout.LINE_START);
		options.add(mode, BorderLayout.CENTER);
		
		JPanel progress = new JPanel(new BorderLayout());
		progress.setBorder(BorderFactory.createTitledBorder("Progress"));
		JProgressBar bar = new JProgressBar();
		progress.add(bar, BorderLayout.CENTER);
		
		JPanel controls = new JPanel(new GridLayout(1, 2, 5, 0));
		controls.setBorder(BorderFactory.createTitledBorder("Controls"));
		JButton exit = new JButton("Exit");
		JButton start = new JButton("Start");
		controls.add(start);
		controls.add(exit);
		
		panel.add(input);
		panel.add(output);
		panel.add(options);
		panel.add(progress);
		panel.add(controls);
		
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
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
