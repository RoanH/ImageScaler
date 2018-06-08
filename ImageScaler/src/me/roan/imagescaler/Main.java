package me.roan.imagescaler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

public class Main {

	public static void main(String[] args){
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(null);
		File f = chooser.getSelectedFile();
		for(File fn : getImages(f)){
			System.out.println(fn.getName());
		}
		
		
		
		
	}
	
	private static final List<File> getImages(File dir){
		List<File> list = new ArrayList<File>();
		for(File file : dir.listFiles()){
			if(file.isDirectory()){
				list.addAll(getImages(file));
			}else if(file.getName().matches(".+@2x\\..*")){
				list.add(file);
			}
		}
		return list;
	}
}
