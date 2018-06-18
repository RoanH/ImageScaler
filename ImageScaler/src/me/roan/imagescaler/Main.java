package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Relatively simple program that rescales images
 * in a folder that match a regex
 * and writes them to some other folder.
 * 
 * @author Roan
 */
public class Main {
	/**
	 * The directory to write rescaled images to
	 */
	protected static File outputDir;
	/**
	 * The directory to search for images to rescale
	 */
	protected static File inputDir;
	/**
	 * The factor to scale the input images by
	 */
	protected static double scale = 0.5D;
	/**
	 * Whether or not to overwrite existing files
	 */
	protected static boolean overwrite = false;
	/**
	 * The scaling algorithm that is used
	 */
	protected static ScalingMode mode = ScalingMode.QUALITY;
	/**
	 * The file chooser that is used
	 */
	protected static JFileChooser chooser;
	/**
	 * Number of rescale threads to use
	 */
	protected static int threads = Runtime.getRuntime().availableProcessors();
	/**
	 * Regex used to match the files to convert
	 */
	protected static Pattern regex = Pattern.compile(".+@2x\\..*");
	/**
	 * Regex that is used on all file names to optionally modify them
	 */
	protected static Pattern renameRegex = Pattern.compile("@2x");
	/**
	 * Replacement string for file name parts
	 * matched by the {@link #renameRegex} regex.
	 */
	protected static String renameReplace = "";

	/**
	 * Starts the program and shows the GUI
	 * @param args No valid command line arguments
	 */
	public static void main(String[] args){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
		}
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		
		JFrame frame = new JFrame("Image Scaler");
		try {
			frame.setIconImage(ImageIO.read(ClassLoader.getSystemResource("icon.png")));
		} catch (IOException e2) {
		}
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JPanel input = new JPanel(new BorderLayout());
		JTextField lout = new JTextField("");
		JCheckBox samefolder = new JCheckBox("Write to input folder", true);
		input.setBorder(BorderFactory.createTitledBorder("Input Folder"));
		JButton selin = new JButton("Select");
		JTextField lin = new JTextField("");
		input.add(lin, BorderLayout.CENTER);
		input.add(selin, BorderLayout.LINE_END);
		input.add(new JLabel("Folder: "), BorderLayout.LINE_START);
		selin.addActionListener((e)->{
			if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
				lin.setText(chooser.getSelectedFile().getAbsolutePath());
				if(samefolder.isSelected()){
					lout.setText(lin.getText());
				}
			}
		});
		
		JPanel output = new JPanel(new BorderLayout());
		output.setBorder(BorderFactory.createTitledBorder("Output Folder"));
		JButton selout = new JButton("Select");
		output.add(lout, BorderLayout.CENTER);
		output.add(selout, BorderLayout.LINE_END);
		output.add(samefolder, BorderLayout.PAGE_START);
		output.add(new JLabel("Folder: "), BorderLayout.LINE_START);
		lout.setEnabled(false);
		selout.setEnabled(false);
		samefolder.addActionListener((e)->{
			if(samefolder.isSelected()){
				lout.setEnabled(false);
				selout.setEnabled(false);
				lout.setText(lin.getText());
			}else{
				lout.setEnabled(true);
				selout.setEnabled(true);
			}
		});
		selout.addActionListener((e)->{
			if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
				lout.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});
		
		JPanel options = new JPanel(new BorderLayout());
		options.setBorder(BorderFactory.createTitledBorder("Options"));
		JCheckBox over = new JCheckBox("Overwrite existing files?", overwrite);
		JComboBox<ScalingMode> mode = new JComboBox<ScalingMode>(ScalingMode.values());
		mode.setSelectedItem(Main.mode);
		JPanel labels = new JPanel(new GridLayout(2, 1, 0, 5));
		JPanel sels = new JPanel(new GridLayout(2, 1, 0, 5));
		JSpinner scalef = new JSpinner(new SpinnerNumberModel(Main.scale, 0, Short.MAX_VALUE, 0.01));
		options.add(over, BorderLayout.PAGE_START);
		labels.add(new JLabel("Scaling algorithm: "));
		labels.add(new JLabel("Scaling factor: "));
		sels.add(mode);
		sels.add(scalef);
		options.add(labels, BorderLayout.LINE_START);
		options.add(sels, BorderLayout.CENTER);
		over.addActionListener((e)->{
			overwrite = over.isSelected();
		});
		mode.addActionListener((e)->{
			Main.mode = (ScalingMode)mode.getSelectedItem();
		});
		
		JPanel advoptions = new JPanel(new BorderLayout());
		advoptions.setBorder(BorderFactory.createTitledBorder("Advanced Options"));
		JPanel labelsadv = new JPanel(new GridLayout(3, 1, 0, 5));
		JPanel selsadv = new JPanel(new GridLayout(3, 1, 0, 5));
		JSpinner threads = new JSpinner(new SpinnerNumberModel(Main.threads, 1, Main.threads, 1));
		JTextField regex = new JTextField(Main.regex.pattern());
		regex.setToolTipText("Matches the files that will be rescaled.");
		JTextField renameMatch = new JTextField(Main.renameRegex.pattern());
		renameMatch.setToolTipText("Matches a part of the file name that can be changed.");
		JTextField renameReplace = new JTextField(Main.renameReplace);
		renameReplace.setToolTipText("The string to use as a replacement for the regions found by the regex.");
		JPanel rename = new JPanel(new GridLayout(1, 2, 4, 0));
		rename.add(renameMatch);
		rename.add(renameReplace);
		labelsadv.add(new JLabel("File name regex: "));
		labelsadv.add(new JLabel("File rename regex: "));
		labelsadv.add(new JLabel("Threads: "));
		selsadv.add(regex);
		selsadv.add(rename);
		selsadv.add(threads);
		advoptions.add(labelsadv, BorderLayout.LINE_START);
		advoptions.add(selsadv, BorderLayout.CENTER);
		
		JPanel progress = new JPanel(new BorderLayout());
		progress.setBorder(BorderFactory.createTitledBorder("Progress"));
		JProgressBar bar = new JProgressBar();
		bar.setMinimum(0);
		progress.add(bar, BorderLayout.CENTER);
		JLabel ptext = new JLabel("Waiting...", SwingConstants.CENTER);
		progress.add(ptext, BorderLayout.PAGE_START);
		
		JPanel controls = new JPanel(new GridLayout(1, 2, 5, 0));
		controls.setBorder(BorderFactory.createTitledBorder("Controls"));
		JButton pause = new JButton("Pause");
		JButton start = new JButton("Start");
		controls.add(start);
		controls.add(pause);
		pause.setEnabled(false);
		pause.addActionListener((e)->{
			if(Worker.running){
				Worker.running = false;
				pause.setText("Resume");
			}else{
				Worker.running = true;
				pause.setText("Pause");
			}
		});
		start.addActionListener((e)->{
			inputDir = new File(lin.getText());
			if(!inputDir.exists()){
				JOptionPane.showMessageDialog(frame, "Input Directory does not exist!", "Image Scaler", JOptionPane.ERROR_MESSAGE);
			}else{
				outputDir = new File(lout.getText());
				try{
					Main.regex = Pattern.compile(regex.getText());
				}catch(PatternSyntaxException e1){
					JOptionPane.showMessageDialog(frame, "Invalid file name regex: " + e1.getMessage(), "Image Scaler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				try{
					Main.renameRegex = Pattern.compile(renameMatch.getText());
				}catch(PatternSyntaxException e1){
					JOptionPane.showMessageDialog(frame, "Invalid file rename regex: " + e1.getMessage(), "Image Scaler", JOptionPane.ERROR_MESSAGE);
					return;
				}
				Main.renameReplace = renameReplace.getText();
				renameReplace.setEnabled(false);
				renameMatch.setEnabled(false);
				selin.setEnabled(false);
				lin.setEnabled(false);
				selout.setEnabled(false);
				lout.setEnabled(false);
				over.setEnabled(false);
				samefolder.setEnabled(false);
				mode.setEnabled(false);
				scalef.setEnabled(false);
				threads.setEnabled(false);
				start.setEnabled(false);
				regex.setEnabled(false);
				pause.setEnabled(true);
				final int total = Worker.prepare();
				if(total == 0){
					renameReplace.setEnabled(true);
					renameMatch.setEnabled(true);
					selin.setEnabled(true);
					lin.setEnabled(true);
					selout.setEnabled(true);
					lout.setEnabled(true);
					over.setEnabled(true);
					samefolder.setEnabled(true);
					mode.setEnabled(true);
					scalef.setEnabled(true);
					threads.setEnabled(true);
					start.setEnabled(true);
					regex.setEnabled(true);
					pause.setEnabled(false);
					ptext.setText("No files to rescale");
					bar.setMaximum(1);
					bar.setValue(1);
					return;
				}
				bar.setMaximum(total);
				final Object lock = new Object();
				Worker.start(()->{
					synchronized(lock){
						int done = Worker.completed.get();
						bar.setValue(done);
						ptext.setText(done + "/" + total);
						progress.repaint();
						if(done == total){
							renameReplace.setEnabled(true);
							renameMatch.setEnabled(true);
							selin.setEnabled(true);
							lin.setEnabled(true);
							selout.setEnabled(true);
							lout.setEnabled(true);
							over.setEnabled(true);
							samefolder.setEnabled(true);
							mode.setEnabled(true);
							scalef.setEnabled(true);
							threads.setEnabled(true);
							start.setEnabled(true);
							regex.setEnabled(true);
							pause.setEnabled(false);
						}
					}
				});
			}
		});
		
		JPanel version = new JPanel(new GridLayout(2, 1, 0, 2));
		version.setBorder(BorderFactory.createTitledBorder("Information"));
		JLabel ver = new JLabel("<html><center><i>Version: v1.0, latest version: <font color=gray>loading</font></i></center></html>", SwingConstants.CENTER);
		version.add(ver);
		new Thread(()->{
			String v = checkVersion();//XXX the version number 
			ver.setText("<html><center><i>Version: v1.0, latest version: " + (v == null ? "unknown :(" : v) + "</i></center></html>");
		}, "Version Checker").start();
		JPanel links = new JPanel(new GridLayout(1, 2, -2, 0));
		JLabel forum = new JLabel("<html><font color=blue><u>Forums</u></font> -</html>", SwingConstants.RIGHT);
		JLabel git = new JLabel("<html>- <font color=blue><u>GitHub</u></font></html>", SwingConstants.LEFT);
		links.add(forum);
		links.add(git);
		version.add(links);
		version.add(ver);
		forum.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent e) {
				if(Desktop.isDesktopSupported()){
					try {
						Desktop.getDesktop().browse(new URL("https://osu.ppy.sh/community/forums/topics/762684").toURI());
					} catch (IOException | URISyntaxException e1) {
						//pity
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		git.addMouseListener(new MouseListener(){

			@Override
			public void mouseClicked(MouseEvent e) {
				if(Desktop.isDesktopSupported()){
					try {
						Desktop.getDesktop().browse(new URL("https://github.com/RoanH/ImageScaler").toURI());
					} catch (IOException | URISyntaxException e1) {
						//pity
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		
		panel.add(input);
		panel.add(output);
		panel.add(options);
		panel.add(advoptions);
		panel.add(progress);
		panel.add(controls);
		panel.add(version);
		
		frame.add(panel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setSize(400, panel.getPreferredSize().height + frame.getInsets().top + frame.getInsets().bottom);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
	
	/**
	 * Checks the Image Scaler version to see
	 * if we are running the latest version
	 * @return The latest version
	 */
	private static final String checkVersion(){
		try{ 			
			HttpURLConnection con = (HttpURLConnection) new URL("https://api.github.com/repos/RoanH/ImageScaler/tags").openConnection(); 			
			con.setRequestMethod("GET"); 		
			con.setConnectTimeout(10000); 					   
			BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream())); 	
			String line = reader.readLine(); 		
			reader.close(); 	
			String[] versions = line.split("\"name\":\"v");
			int max_main = 1;
			int max_sub = 0;
			String[] tmp;
			for(int i = 1; i < versions.length; i++){
				tmp = versions[i].split("\",\"")[0].split("\\.");
				if(Integer.parseInt(tmp[0]) > max_main){
					max_main = Integer.parseInt(tmp[0]);
					max_sub = Integer.parseInt(tmp[1]);
				}else if(Integer.parseInt(tmp[0]) < max_main){
					continue;
				}else{
					if(Integer.parseInt(tmp[1]) > max_sub){
						max_sub = Integer.parseInt(tmp[1]);
					}
				}
			}
			return "v" + max_main + "." + max_sub;
		}catch(Exception e){ 	
			return null;
			//No Internet access or something else is wrong,
			//No problem though since this isn't a critical function
		}
	}
}
