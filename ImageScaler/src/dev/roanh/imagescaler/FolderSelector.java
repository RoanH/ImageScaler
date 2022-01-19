package dev.roanh.imagescaler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import dev.roanh.util.Dialog;
import dev.roanh.util.FileTextField;
import dev.roanh.util.FileTextField.FileChangeListener;

/**
 * Represents a small component used to select a folder
 * or file. Folders or files can be select with this
 * component either by typing, dropping or pasting.
 * @author Roan
 */
public class FolderSelector extends JPanel implements ActionListener{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 8349454444582863534L;
	/**
	 * File text field used to show the path
	 * and handle drag drop actions.
	 */
	private FileTextField field = new FileTextField();
	/**
	 * Button to open the file chooser.
	 */
	private JButton select = new JButton("Select");
	
	/**
	 * Constructs a new FolderSelector.
	 */
	public FolderSelector(){
		this(null);
	}
	
	/**
	 * Constructs a new FolderSelector with the
	 * given update listener.
	 * @param listener The listener to send selection
	 *        changes to (can be <code>null</code>).
	 */
	public FolderSelector(FileChangeListener listener){
		super(new BorderLayout());
		
		field.setListener(listener);
		
		this.add(field, BorderLayout.CENTER);
		this.add(select, BorderLayout.LINE_END);
		this.add(new JLabel("Folder: "), BorderLayout.LINE_START);
		
		select.addActionListener(this);
	}
	
	/**
	 * Gets the selected element as a string.
	 * @return The select target element
	 *         (file or folder).
	 */
	public String getText(){
		return field.getText();
	}
	
	/**
	 * Gets the selected target as a path.
	 * @return The selected target.
	 */
	public Path getFile(){
		return field.getPath();
	}
	
	/**
	 * Sets the select element to the
	 * given string.
	 * @param text The new selected element.
	 */
	public void setText(String text){
		field.setText(text);
	}
	
	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		field.setEnabled(enabled);
		select.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e){
		Path file = Dialog.showFolderOpenDialog();
		if(file != null){
			field.setText(file.toAbsolutePath().toString());
		}
	}
}
