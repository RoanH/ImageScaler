package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Represents a small component used to select a folder
 * or file. Folders or files can be select with this
 * component either by trying, dropping or pasting.
 * @author Roan
 */
public class FolderSelector extends JPanel implements DropTargetListener, DocumentListener, ActionListener{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 8349454444582863534L;
	/**
	 * The file chooser that is used.
	 */
	protected static JFileChooser chooser;
	/**
	 * Consumer that gets notified when the select folder or file changes.
	 */
	private Consumer<String> listener;
	/**
	 * Text field to display and directly modify the selected element.
	 */
	private JTextField field = new JTextField("");
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
	public FolderSelector(Consumer<String> listener){
		super(new BorderLayout());
		
		this.listener = listener;
		
		this.add(field, BorderLayout.CENTER);
		this.add(select, BorderLayout.LINE_END);
		this.add(new JLabel("Folder: "), BorderLayout.LINE_START);
		
		select.addActionListener(this);
		new DropTarget(field, this);
		field.getDocument().addDocumentListener(this);
	}
	
	/**
	 * Sets the listener to send selection updates to.
	 * @param listener The listener to send selection
	 *        changes to (can be <code>null</code>).
	 */
	public void setListener(Consumer<String> listener){
		this.listener = listener;
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
	 * Gets the selected target as a file.
	 * @return The selected target.
	 */
	public File getFile(){
		return new File(getText());
	}
	
	/**
	 * Sets the select element to the
	 * given string.
	 * @param text The new selected element.
	 */
	public void setText(String text){
		field.setText(text);
	}
	
	/**
	 * Sends a content update to the
	 * listener if one is set.
	 * @see #setListener(Consumer)
	 */
	private void update(){
		if(listener != null){
			listener.accept(field.getText());
		}
	}
	
	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		field.setEnabled(enabled);
		select.setEnabled(enabled);
	}
	
	@Override
	public void actionPerformed(ActionEvent e){
		if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
			field.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}
	
	@Override
	public void insertUpdate(DocumentEvent e){
		update();
	}

	@Override
	public void removeUpdate(DocumentEvent e){
		update();
	}

	@Override
	public void changedUpdate(DocumentEvent e){
		update();
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde){		
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde){		
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde){		
	}

	@Override
	public void dragExit(DropTargetEvent dte){		
	}

	@Override
	public void drop(DropTargetDropEvent dtde){
		if(dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) && this.isEnabled()){
			try{
				dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				@SuppressWarnings("unchecked")
				List<File> files = (List<File>)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
				if(files.size() > 0){
					field.setText(files.get(0).getAbsolutePath());
				}
			}catch(UnsupportedFlavorException | IOException e){
				//Pity, but not important
			}
		}
	}
	
	static{
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(false);
	}
}
