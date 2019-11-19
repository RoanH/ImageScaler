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

public class FolderSelector extends JPanel implements DropTargetListener, DocumentListener, ActionListener{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 8349454444582863534L;
	/**
	 * The file chooser that is used
	 */
	protected static JFileChooser chooser;
	private Consumer<String> listener;
	private JTextField field = new JTextField("");
	private JButton select = new JButton("Select");
	
	public FolderSelector(){
		this(null);
	}
	
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
	
	public void setListener(Consumer<String> listener){
		this.listener = listener;
	}
	
	public String getText(){
		return field.getText();
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
		// TODO Auto-generated method stub
		//System.out.println("Insert: " + lin.getText());
		if(listener != null){
			listener.accept(field.getText());
		}	
	}

	@Override
	public void removeUpdate(DocumentEvent e){
		// TODO Auto-generated method stub
		//System.out.println("Remove: " + lin.getText());
		if(listener != null){
			listener.accept(field.getText());
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e){
		// TODO Auto-generated method stub
		//System.out.println("Change: " + lin.getText());
		if(listener != null){
			listener.accept(field.getText());
		}
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
				System.out.println("DND: " + files);
				if(files.size() > 0){
					field.setText(files.get(0).getAbsolutePath());
				}
			}catch(UnsupportedFlavorException | IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	static{
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(false);
	}
}
