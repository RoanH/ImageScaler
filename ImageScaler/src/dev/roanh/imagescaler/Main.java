package dev.roanh.imagescaler;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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

import dev.roanh.imagescaler.Worker.ProgressListener;
import dev.roanh.util.ClickableLink;
import dev.roanh.util.Dialog;
import dev.roanh.util.ExclamationMarkPath;
import dev.roanh.util.Util;

/**
 * Relatively simple program that rescales images
 * in a folder that match a regex
 * and writes them to some other folder.
 * @author Roan
 */
public class Main{
	/**
	 * Active worker currently scaling images.
	 */
	private static Worker worker = null;

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
		FolderSelector fin = new FolderSelector((data, path)->{
			if(samefolder.isSelected()){
				fout.setText(Files.isRegularFile(path) ? "Not applicable, input is a file" : data);
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

		JPanel options = new JPanel(new BorderLayout());
		JPanel checkboxes = new JPanel(new GridLayout(2, 1, 0, 0));
		options.setBorder(BorderFactory.createTitledBorder("Options"));
		JCheckBox over = new JCheckBox("Overwrite existing files", false);
		JCheckBox subdir = new JCheckBox("Parse subdirectories", true);
		JComboBox<ScalingMode> mode = new JComboBox<ScalingMode>(ScalingMode.values());
		mode.setSelectedItem(ScalingMode.LANCZOS);
		JPanel labels = new JPanel(new GridLayout(2, 1, 0, 5));
		JPanel sels = new JPanel(new GridLayout(2, 1, 0, 5));
		JSpinner scalef = new JSpinner(new SpinnerNumberModel(0.0D, 0, Short.MAX_VALUE, 0.01));
		checkboxes.add(over);
		checkboxes.add(subdir);
		options.add(checkboxes, BorderLayout.PAGE_START);
		labels.add(new JLabel("Scaling algorithm: "));
		labels.add(new JLabel("Scaling factor: "));
		sels.add(mode);
		sels.add(scalef);
		options.add(labels, BorderLayout.LINE_START);
		options.add(sels, BorderLayout.CENTER);

		JPanel advoptions = new JPanel(new BorderLayout());
		advoptions.setBorder(BorderFactory.createTitledBorder("Advanced Options"));
		JPanel labelsadv = new JPanel(new GridLayout(4, 1, 0, 5));
		JPanel selsadv = new JPanel(new GridLayout(4, 1, 0, 5));
		JPanel helpadv = new JPanel(new GridLayout(4, 1, 0, 5));
		JSpinner threads = new JSpinner(new SpinnerNumberModel(Math.min(4, Runtime.getRuntime().availableProcessors()), 1, Runtime.getRuntime().availableProcessors(), 1));
		JTextField regex = new JTextField(".+@2x");
		regex.setToolTipText("Matches the files that will be rescaled (note .+ just matches any number of characters).");
		JTextField extensionField = new JTextField("png, jpg, jpeg");
		extensionField.setToolTipText("File name extensions to match, case insensitive.");
		JTextField renameMatch = new JTextField("@2x");
		renameMatch.setToolTipText("Matches a part of the file name that can be changed.");
		JTextField renameReplace = new JTextField("");
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
		
		pause.addActionListener((e)->{
			if(worker != null){
				if(worker.isRunning()){
					worker.setRunning(false);
					pause.setText("Resume");
				}else{
					worker.setRunning(true);
					pause.setText("Pause");
				}
			}
		});
		
		Consumer<Boolean> enableFun = enabled->{
			pause.setEnabled(!enabled);
			renameReplace.setEnabled(enabled);
			renameMatch.setEnabled(enabled);
			fout.setEnabled(enabled);
			fin.setEnabled(enabled);
			over.setEnabled(enabled);
			samefolder.setEnabled(enabled);
			subdir.setEnabled(enabled);
			mode.setEnabled(enabled);
			scalef.setEnabled(enabled);
			threads.setEnabled(enabled);
			start.setEnabled(enabled);
			regex.setEnabled(enabled);
			helpRegex.setEnabled(enabled);
			helpExt.setEnabled(enabled);
			helpRename.setEnabled(enabled);
			helpThreads.setEnabled(enabled);
			fout.setEnabled(!samefolder.isSelected());
		};
		enableFun.accept(true);
		
		start.addActionListener((e)->{
			Path inputDir = fin.getFile();
			if(Files.notExists(inputDir)){
				Dialog.showErrorDialog("Input directory does not exist!");
			}else{
				Path outputDir = Files.isDirectory(inputDir) ? fout.getFile() : null;
				
				Pattern matchRegex = null;
				try{
					matchRegex = Pattern.compile(regex.getText());
				}catch(PatternSyntaxException e1){
					Dialog.showErrorDialog("Invalid file name regex: " + e1.getMessage());
					return;
				}
				
				Pattern renameRegex = null;
				try{
					renameRegex = Pattern.compile(renameMatch.getText());
				}catch(PatternSyntaxException e1){
					Dialog.showErrorDialog("Invalid file rename regex: " + e1.getMessage());
					return;
				}
				
				String replacementText = renameReplace.getText();
				String[] extensions = extensionField.getText().split(",");
				for(int i = 0; i < extensions.length; i++){
					extensions[i] = extensions[i].trim();
				}

				try{
					Worker worker = new Worker(
						inputDir,
						outputDir,
						subdir.isSelected(),
						matchRegex,
						renameRegex,
						replacementText,
						extensions,
						over.isSelected(),
						(ScalingMode)mode.getSelectedItem(),
						(double)scalef.getValue()
					);
					
					if(worker.getWorkloadSize() == 0){
						ptext.setText("No files to rescale");
						bar.setMaximum(1);
						bar.setValue(1);
						return;
					}
					bar.setMaximum(worker.getWorkloadSize());
					bar.setValue(0);

					enableFun.accept(false);

					List<String> exceptions = new ArrayList<String>(0);
					worker.start((int)threads.getValue(), new ProgressListener(){
						@Override
						public void progress(int completed){
							bar.setValue(completed);
							ptext.setText(completed + "/" + worker.getWorkloadSize());
							progress.repaint();
						}

						@Override
						public void error(Path file, Exception e){
							exceptions.add(file.toAbsolutePath().toString() + ": " + e.getMessage());
						}

						@Override
						public void done(){
							if(!exceptions.isEmpty()){
								JPanel msg = new JPanel(new BorderLayout());
								msg.add(new JLabel("Scaling finished with " + (exceptions.size() == 1 ? "1 error" : (exceptions.size() + " errors")) + ". These files were consequently skipped:"), BorderLayout.PAGE_START);
								msg.add(new JScrollPane(new JList<String>(exceptions.toArray(new String[0]))), BorderLayout.CENTER);
								Dialog.showMessageDialog(msg);
							}

							enableFun.accept(true);
						}
					});
				}catch(IOException e1){
					e1.printStackTrace();
					Dialog.showErrorDialog("An internal error occurred:\nCause: " + e1.getMessage());
				}
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
