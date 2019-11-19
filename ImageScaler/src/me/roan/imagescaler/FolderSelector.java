package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FolderSelector extends JPanel{
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
		
		System.out.println(lin.getTransferHandler());
		//lin.setTransferHandler(new FolderTransferHandler(lin.getTransferHandler()));
		new DropTarget(lin, new DropTargetListener(){

			@Override
			public void dragEnter(DropTargetDragEvent dtde){
				// TODO Auto-generated method stub
				
			}

			@Override
			public void dragOver(DropTargetDragEvent dtde){
				// TODO Auto-generated method stub
				
			}

			@Override
			public void dropActionChanged(DropTargetDragEvent dtde){
				// TODO Auto-generated method stub
				
			}

			@Override
			public void dragExit(DropTargetEvent dte){
				// TODO Auto-generated method stub
				
			}

			@Override
			public void drop(DropTargetDropEvent dtde){
				// TODO Auto-generated method stub
				System.out.println("DROP: " + dtde);
			}
			
		});
		lin.getDocument().addDocumentListener(new DocumentListener(){

			@Override
			public void insertUpdate(DocumentEvent e){
				// TODO Auto-generated method stub
				System.out.println("Insert: " + lin.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent e){
				// TODO Auto-generated method stub
				System.out.println("Remove: " + lin.getText());

			}

			@Override
			public void changedUpdate(DocumentEvent e){
				// TODO Auto-generated method stub
				System.out.println("Change: " + lin.getText());

			}
			
		});
	}
	
	@Override
	public void setEnabled(boolean enabled){
		//TODO
	}
	
	private static final class FolderTransferHandler extends TransferHandler{
		private TransferHandler text;
		
		private FolderTransferHandler(TransferHandler original){
			text = original;
		}
		
		@Override
		public boolean canImport(TransferSupport info){
			System.out.println("Support: " + info);
			return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor) || text.canImport(info);
		}
		
		@Override
		public boolean importData(TransferSupport info){
			if(!info.isDrop()){
				return false;
			}
			
			if(info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)){
				try{
					System.out.println("DND: " + info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
				}catch(UnsupportedFlavorException | IOException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if(info.isDataFlavorSupported(DataFlavor.stringFlavor)){
				text.importData(info);
			}
			
			
			
			
			return false;
		}
		
        @Override
		public int getSourceActions(JComponent c){
        	return text.getSourceActions(c);
        }
        
//        protected Transferable createTransferable(JComponent comp) {
//        	return text.c
//        }
//
//        protected void exportDone(JComponent source, Transferable data, int action) {
//        	
//        }
        
        public boolean importData(JComponent comp, Transferable t) {
        	return text.importData(comp, t);
        }
        
        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
        	return text.canImport(comp, flavors);
        }


	}
	
	static{
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
	}
}
