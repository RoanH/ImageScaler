package me.roan.imagescaler;

import java.awt.BorderLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.TransferHandler;

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
		
		this.setTransferHandler(new FolderTransferHandler());
	}
	
	@Override
	public void setEnabled(boolean enabled){
		//TODO
	}
	
	private static final class FolderTransferHandler extends TransferHandler{
		
		public boolean canImport(TransferSupport info){
			return info.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		}
		
		public boolean importData(TransferSupport info){
			if(!info.isDrop() || !canImport(info)){
				return false;
			}
			
			try{
				System.out.println("DND: " + info.getTransferable().getTransferData(DataFlavor.javaFileListFlavor));
			}catch(UnsupportedFlavorException | IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return false;
		}
	}
	
	static{
		chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
	}
}
