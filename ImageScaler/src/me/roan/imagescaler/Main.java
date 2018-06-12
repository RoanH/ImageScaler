package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.regex.PatternSyntaxException;

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

public class Main {
	
	protected static File outputDir = new File("test");
	protected static File inputDir;
	protected static double scale = 0.5D;
	protected static boolean overwrite = false;
	protected static ScalingMode mode = ScalingMode.QUALITY;
	protected static JFileChooser chooser;
	protected static int threads = Runtime.getRuntime().availableProcessors();
	protected static String regex = ".+@2x\\..*";

	public static void main(String[] args){
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable t) {
		}
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		
		JFrame frame = new JFrame("Image Scaler");
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
		JPanel labels = new JPanel(new GridLayout(4, 1, 0, 5));
		JPanel sels = new JPanel(new GridLayout(4, 1, 0, 5));
		JSpinner threads = new JSpinner(new SpinnerNumberModel(Main.threads, 1, Main.threads, 1));
		JSpinner scalef = new JSpinner(new SpinnerNumberModel(Main.scale, 0, Short.MAX_VALUE, 0.01));
		JTextField regex = new JTextField(Main.regex);
		options.add(over, BorderLayout.PAGE_START);
		labels.add(new JLabel("Scaling algorithm: "));
		labels.add(new JLabel("File name regex: "));
		labels.add(new JLabel("Scaling factor: "));
		labels.add(new JLabel("Threads: "));
		sels.add(mode);
		sels.add(regex);
		sels.add(scalef);
		sels.add(threads);
		options.add(labels, BorderLayout.LINE_START);
		options.add(sels, BorderLayout.CENTER);
		over.addActionListener((e)->{
			overwrite = over.isSelected();
		});
		mode.addActionListener((e)->{
			Main.mode = (ScalingMode)mode.getSelectedItem();
		});
		
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
				Main.regex = regex.getText();
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
				int count = 0;
				try{
					count = Worker.prepare();
				}catch(PatternSyntaxException e1){
					JOptionPane.showMessageDialog(frame, "Invalid file name regex: " + e1.getMessage(), "Image Scaler", JOptionPane.ERROR_MESSAGE);
					count = -1;
				}
				if(count <= 0){
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
					if(count == 0){
						ptext.setText("No files to convert");
						bar.setMaximum(1);
						bar.setValue(1);
					}
					return;
				}
				final int total = count;
				bar.setMaximum(total);
				final Object lock = new Object();
				Worker.start(()->{
					synchronized(lock){
						int done = Worker.completed.get();
						bar.setValue(done);
						ptext.setText(done + "/" + total);
						progress.repaint();
						if(done == total){
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
		
		JPanel version = new JPanel(new GridLayout(2, 1, 5, 0));
		version.setBorder(BorderFactory.createTitledBorder("Information"));
		JLabel links = new JLabel("forums - GitHub", SwingConstants.CENTER);
		JLabel ver = new JLabel("Version 1.0, latest version /shrug", SwingConstants.CENTER);
		version.add(links);
		version.add(ver);
		
		panel.add(input);
		panel.add(output);
		panel.add(options);
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
