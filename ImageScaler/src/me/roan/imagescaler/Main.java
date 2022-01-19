package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
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
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import me.roan.imagescaler.Worker.ProgressListener;
import dev.roanh.util.ClickableLink;
import dev.roanh.util.Dialog;
import dev.roanh.util.ExclamationMarkPath;
import dev.roanh.util.Util;

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
	protected static int threads = Math.min(4, Runtime.getRuntime().availableProcessors());
	/**
	 * Regex used to match the files to convert
	 */
	protected static Pattern regex = Pattern.compile(".+@2x");
	/**
	 * Regex that is used on all file names to optionally modify them
	 */
	protected static Pattern renameRegex = Pattern.compile("@2x");
	/**
	 * List of file extensions that is used to match the files to convert.
	 */
	protected static String[] extensions = new String[]{"png", "jpg", "jpeg"};
	/**
	 * Replacement string for file name parts
	 * matched by the {@link #renameRegex} regex.
	 */
	protected static String renameReplace = "";
	/**
	 * Whether or not to parse subdirectories.
	 */
	protected static boolean subdirectories = true;

	/**
	 * Starts the program and shows the GUI
	 * @param args The <tt>-relaunch</tt> argument
	 *        can be used to relaunch from the
	 *        temp directory if the current executable
	 *        location is affected by a JDK bug.
	 */
	public static void main(String[] args){
		Util.installUI();
		ExclamationMarkPath.check(args);
		
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
				fout.setText(data.contains(".") ? "Not applicable, input is a file" : data);
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
		JPanel checkboxes = new JPanel(new GridLayout(2, 1, 0, 0));
		options.setBorder(BorderFactory.createTitledBorder("Options"));
		JCheckBox over = new JCheckBox("Overwrite existing files", overwrite);
		JCheckBox subdir = new JCheckBox("Parse subdirectories", subdirectories);
		JComboBox<ScalingMode> mode = new JComboBox<ScalingMode>(ScalingMode.values());
		mode.setSelectedItem(Main.mode);
		JPanel labels = new JPanel(new GridLayout(2, 1, 0, 5));
		JPanel sels = new JPanel(new GridLayout(2, 1, 0, 5));
		JSpinner scalef = new JSpinner(new SpinnerNumberModel(Main.scale, 0, Short.MAX_VALUE, 0.01));
		checkboxes.add(over);
		checkboxes.add(subdir);
		options.add(checkboxes, BorderLayout.PAGE_START);
		labels.add(new JLabel("Scaling algorithm: "));
		labels.add(new JLabel("Scaling factor: "));
		sels.add(mode);
		sels.add(scalef);
		options.add(labels, BorderLayout.LINE_START);
		options.add(sels, BorderLayout.CENTER);
		over.addActionListener((e)->{
			overwrite = over.isSelected();
		});
		subdir.addActionListener((e)->{
			subdirectories = subdir.isSelected();
		});
		mode.addActionListener((e)->{
			Main.mode = (ScalingMode)mode.getSelectedItem();
		});
		scalef.addChangeListener((e)->{
			Main.scale = (double)scalef.getValue();
		});

		JPanel advoptions = new JPanel(new BorderLayout());
		advoptions.setBorder(BorderFactory.createTitledBorder("Advanced Options"));
		JPanel labelsadv = new JPanel(new GridLayout(4, 1, 0, 5));
		JPanel selsadv = new JPanel(new GridLayout(4, 1, 0, 5));
		JPanel helpadv = new JPanel(new GridLayout(4, 1, 0, 5));
		JSpinner threads = new JSpinner(new SpinnerNumberModel(Main.threads, 1, Runtime.getRuntime().availableProcessors(), 1));
		JTextField regex = new JTextField(Main.regex.pattern());
		regex.setToolTipText("Matches the files that will be rescaled (note .+ just matches any number of characters).");
		StringJoiner joiner = new StringJoiner(", ");
		for(String ext : extensions){
			joiner.add(ext);
		}
		JTextField extensionField = new JTextField(joiner.toString());
		extensionField.setToolTipText("File name extensions to match, case insensitive.");
		JTextField renameMatch = new JTextField(Main.renameRegex.pattern());
		renameMatch.setToolTipText("Matches a part of the file name that can be changed.");
		JTextField renameReplace = new JTextField(Main.renameReplace);
		renameReplace.setToolTipText("The string to use as a replacement for the regions found by the regex.");
		JPanel rename = new JPanel(new GridLayout(1, 3, 4, 0));
		rename.add(renameMatch);
		rename.add(renameReplace);
		labelsadv.add(new JLabel("File name regex: "));
		labelsadv.add(new JLabel("File extensions: "));
		labelsadv.add(new JLabel("File rename regex: "));
		labelsadv.add(new JLabel("Threads: "));
		selsadv.add(regex);
		selsadv.add(extensionField);
		selsadv.add(rename);
		selsadv.add(threads);
		JButton helpRegex = new JButton("?");
		JButton helpExt = new JButton("?");
		JButton helpRename = new JButton("?");
		JButton helpThreads = new JButton("?");
		helpRegex.setPreferredSize(new Dimension(helpRegex.getPreferredSize().height, helpRegex.getPreferredSize().height));
		helpExt.setPreferredSize(new Dimension(helpExt.getPreferredSize().height, helpExt.getPreferredSize().height));
		helpRename.setPreferredSize(new Dimension(helpRename.getPreferredSize().height, helpRename.getPreferredSize().height));
		helpThreads.setPreferredSize(new Dimension(helpThreads.getPreferredSize().height, helpThreads.getPreferredSize().height));
		Insets margin = new Insets(0, 0, 0, 0);
		helpRegex.setMargin(margin);
		helpExt.setMargin(margin);
		helpRename.setMargin(margin);
		helpThreads.setMargin(margin);
		String regexInfo = "Quick regex help:\n"
			+ "- A dot '.' matches any character.\n"
			+ "- A plus '+' matches the previous character 1~infinity times.\n"
			+ "(note that this makes '.+' a type of wildcard that matches anything)\n"
			+ "- A star '*' matches the previous character 0~infinity times.\n"
			+ "- A question mark '?' matches the previous character 0~1 times.\n"
			+ "- You have to write '\\.' if you want to match an actual dot.\n"
			+ "It shouldn't be too hard to find more information online on\n"
			+ "how to match more complicated patterns.";
		helpRegex.addActionListener(e->{
			Dialog.showMessageDialog(
				"This field specifies the regex used to match the names of\n"
				+ "files to rescale. The default of '.+@2x' matches only\n"
				+ "files with names that end with '@2x'.\n\n"
				+ regexInfo
			);
		});
		helpExt.addActionListener(e->{
			Dialog.showMessageDialog(
				"This field specifies a comma separated list of file extensions\n"
				+ "to match. File extensions are case insensitive."
			);
		});
		helpRename.addActionListener(e->{
			Dialog.showMessageDialog(
				"These two fields specify an action to generate a new name from\n"
				+ "the name of the original file. Much like the file name regex\n"
				+ "the first field contains a regex that matches part of the name\n"
				+ "of the original file. By default it matches the '@2x' part of\n"
				+ "a file name. The second field then specifies a string to replace\n"
				+ "this part of the filename with. By default this field is empty\n"
				+ "meaning that the '@2x' part of file name is simply removed.\n\n"
				+ regexInfo
			);
		});
		helpThreads.addActionListener(e->{
			Dialog.showMessageDialog(
				"This setting controls how many images can be rescaling at the same\n"
				+ "time. The higher this number, the faster the rescaling will be done.\n"
				+ "However setting this to a high number will also use up more CPU\n"
				+ "resources and more RAM. While CPU isn't that much of an issue\n"
				+ "if insufficient RAM is available the program will fail to rescale\n"
				+ "images and might even crash. If you plan on scaling very large\n"
				+ "images then you should either keep this value low or allocate\n"
				+ "a lot of RAM to this process.\n\n"
				+ "Allocated RAM: " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "MB"
			);
		});
		helpadv.add(helpRegex);
		helpadv.add(helpExt);
		helpadv.add(helpRename);
		helpadv.add(helpThreads);
		advoptions.add(labelsadv, BorderLayout.LINE_START);
		advoptions.add(selsadv, BorderLayout.CENTER);
		advoptions.add(helpadv, BorderLayout.LINE_END);

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
			inputDir = fin.getFile();
			if(!inputDir.exists()){
				Dialog.showErrorDialog("Input directory does not exist!");
			}else{
				outputDir = inputDir.isDirectory() ? new File(fout.getText()) : null;
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
				extensions = extensionField.getText().split(",");
				for(int i = 0; i < extensions.length; i++){
					extensions[i] = extensions[i].trim();
				}
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
				subdir.setEnabled(false);
				mode.setEnabled(false);
				scalef.setEnabled(false);
				threads.setEnabled(false);
				start.setEnabled(false);
				regex.setEnabled(false);
				pause.setEnabled(true);
				helpRegex.setEnabled(false);
				helpExt.setEnabled(false);
				helpRename.setEnabled(false);
				helpThreads.setEnabled(false);
				
				List<String> exceptions = new ArrayList<String>(0);
				Worker.start(new ProgressListener(){
					@Override
					public void progress(int completed){
						bar.setValue(completed);
						ptext.setText(completed + "/" + total);
						progress.repaint();
					}

					@Override
					public void error(File file, Exception e){
						exceptions.add(file.getAbsolutePath() + ": " + e.getMessage());
					}

					@Override
					public void done(){
						if(!exceptions.isEmpty()){
							JPanel msg = new JPanel(new BorderLayout());
							msg.add(new JLabel("Scaling finished with " + (exceptions.size() == 1 ? "1 error" : (exceptions.size() + " errors")) + ". These files were consequently skipped:"), BorderLayout.PAGE_START);
							msg.add(new JScrollPane(new JList<String>(exceptions.toArray(new String[0]))), BorderLayout.CENTER);
							Dialog.showMessageDialog(msg);
						}
						
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
						subdir.setEnabled(true);
						start.setEnabled(true);
						regex.setEnabled(true);
						pause.setEnabled(false);
						helpRegex.setEnabled(true);
						helpExt.setEnabled(true);
						helpRename.setEnabled(true);
						helpThreads.setEnabled(true);
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
		version.add(Util.getVersionLabel("ImageScaler", "v2.4"));//XXX the version number - don't forget build.gradle
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
