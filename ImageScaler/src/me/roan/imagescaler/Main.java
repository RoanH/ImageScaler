package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import me.roan.util.ClickableLink;
import me.roan.util.Dialog;
import me.roan.util.Util;

/**
 * Relatively simple program that rescales images
 * in a folder that match a regex
 * and writes them to some other folder.
 * 
 * @author Roan
 */
public class Main{
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
	protected static ScalingMode mode = ScalingMode.LANCZOS;
	/**
	 * Number of rescale threads to use
	 */
	protected static int threads = Runtime.getRuntime().availableProcessors();
	/**
	 * Regex used to match the files to convert
	 */
	protected static Pattern regex = Pattern.compile(".+@2x\\.(png|jpe*g|PNG|JPE*G)");
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
		//TODO issue #23 switch Util.installUI();
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}catch(Throwable t){
		}
		
		JFrame frame = new JFrame("Image Scaler");
		Dialog.setDialogTitle("Image Scaler");
		Dialog.setParentFrame(frame);
		try{
			Image icon = ImageIO.read(ClassLoader.getSystemResource("icon.png"));
			Dialog.setDialogIcon(icon);
			frame.setIconImage(icon);
		}catch(IOException e2){
		}
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		JCheckBox samefolder = new JCheckBox("Write to input folder", true);
		FolderSelector fout = new FolderSelector();
		FolderSelector fin = new FolderSelector(data->{
			if(samefolder.isSelected()){
				fout.setText(data.contains(".") ? "Not applicable input is a file" : data);
			}
		});
		samefolder.addActionListener((e)->{
			if(samefolder.isSelected()){
				fout.setEnabled(false);
				fout.setText(fin.getText());
			}else{
				fout.setEnabled(true);
			}
		});

		JPanel input = new JPanel(new BorderLayout());
		input.setBorder(BorderFactory.createTitledBorder("Input Folder"));
		input.add(fin, BorderLayout.CENTER);

		JPanel output = new JPanel(new BorderLayout());
		output.setBorder(BorderFactory.createTitledBorder("Output Folder"));
		output.add(samefolder, BorderLayout.PAGE_START);
		output.add(fout, BorderLayout.CENTER);
		fout.setEnabled(false);

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
		scalef.addChangeListener((e)->{
			Main.scale = (double)scalef.getValue();
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
			inputDir = new File(fin.getText());
			//TODO handle file case
			if(!inputDir.exists()){
				Dialog.showErrorDialog("Input directory does not exist!");
			}else{
				outputDir = new File(fout.getText());
				try{
					Main.regex = Pattern.compile(regex.getText());
				}catch(PatternSyntaxException e1){
					Dialog.showErrorDialog("Invalid file name regex: " + e1.getMessage());
					return;
				}
				try{
					Main.renameRegex = Pattern.compile(renameMatch.getText());
				}catch(PatternSyntaxException e1){
					Dialog.showErrorDialog("Invalid file rename regex: " + e1.getMessage());
					return;
				}
				Main.renameReplace = renameReplace.getText();
				final int total = Worker.prepare();
				if(total == 0){
					ptext.setText("No files to rescale");
					bar.setMaximum(1);
					bar.setValue(1);
					return;
				}
				bar.setMaximum(total);
				
				renameReplace.setEnabled(false);
				renameMatch.setEnabled(false);
				fout.setEnabled(false);
				fin.setEnabled(false);
				over.setEnabled(false);
				samefolder.setEnabled(false);
				mode.setEnabled(false);
				scalef.setEnabled(false);
				threads.setEnabled(false);
				start.setEnabled(false);
				regex.setEnabled(false);
				pause.setEnabled(true);
				
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
							fin.setEnabled(true);
							samefolder.setEnabled(true);
							if(!samefolder.isSelected()){
								fout.setEnabled(true);
							}
							over.setEnabled(true);
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
		JPanel links = new JPanel(new GridLayout(1, 2, -2, 0));
		JLabel forum = new JLabel("<html><font color=blue><u>Forums</u></font> -</html>", SwingConstants.RIGHT);
		JLabel git = new JLabel("<html>- <font color=blue><u>GitHub</u></font></html>", SwingConstants.LEFT);
		links.add(forum);
		links.add(git);
		version.add(links);
		version.add(Util.getVersionLabel("ImageScaler", "v2.2"));//XXX the version number - don't forget build.gradle
		forum.addMouseListener(new ClickableLink("https://osu.ppy.sh/community/forums/topics/762684"));
		git.addMouseListener(new ClickableLink("https://github.com/RoanH/ImageScaler"));

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
}
