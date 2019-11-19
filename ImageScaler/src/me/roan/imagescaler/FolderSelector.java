package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FolderSelector extends JPanel implements DropTargetListener, DocumentListener{
	/**
	 * Serial ID.
	 */
	private static final long serialVersionUID = 8349454444582863534L;
	/**
	 * The file chooser that is used
	 */
	protected static JFileChooser chooser;
	private Consumer<String> listener;
	
	public FolderSelector(){
		super(new BorderLayout());
		
		JButton selin = new JButton("Select");
		JTextField lin = new JTextField("");
		
		this.add(lin, BorderLayout.CENTER);
		this.add(selin, BorderLayout.LINE_END);
		this.add(new JLabel("Folder: "), BorderLayout.LINE_START);
		
		selin.addActionListener((e)->{
			if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
				lin.setText(chooser.getSelectedFile().getAbsolutePath());
				if(listener != null){
					listener.accept(lin.getText());
				}
			}
		});
		
		new DropTarget(lin, this);
		lin.getDocument().addDocumentListener(this);
	}
	
	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		//TODO
	}
	
	@Override
	public void insertUpdate(DocumentEvent e){
		// TODO Auto-generated method stub
		//System.out.println("Insert: " + lin.getText());
	}

	@Override
	public void removeUpdate(DocumentEvent e){
		// TODO Auto-generated method stub
		//System.out.println("Remove: " + lin.getText());

	}

	@Override
	public void changedUpdate(DocumentEvent e){
		// TODO Auto-generated method stub
		//System.out.println("Change: " + lin.getText());

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
				System.out.println("DND: " + dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
			}catch(UnsupportedFlavorException | IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	static{
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
	}
}
