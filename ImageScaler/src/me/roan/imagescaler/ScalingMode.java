package me.roan.imagescaler;

import java.awt.image.BufferedImage;

public enum ScalingMode {

	QUALITY("Quality", BufferedImage.SCALE_SMOOTH),
	FAST("Speed", BufferedImage.SCALE_FAST);
	
	private final String name;
	protected final int mode;
	
	private ScalingMode(String name, int mode){
		this.name = name;
		this.mode = mode;
	}
	
	@Override
	public String toString(){
		return name;
	}
}
