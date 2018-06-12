package me.roan.imagescaler;

import java.awt.image.BufferedImage;

/**
 * Enum listing the available image scaling modes
 * @author Roan
 */
public enum ScalingMode {
	/**
	 * Indicates the quality over speed algorithm
	 */
	QUALITY("Quality", BufferedImage.SCALE_SMOOTH),
	/**
	 * Indicates the speed over quality algorithm
	 */
	FAST("Speed", BufferedImage.SCALE_FAST);
	
	/**
	 * Display name of the algorithm
	 */
	private final String name;
	/**
	 * Identifier of the algorithm
	 */
	protected final int mode;
	
	/**
	 * Constructs a new ScalingMode
	 * with the given name and mode identifier
	 * @param name The display name of the algorithm
	 * @param mode The mode identifier of the algorithm
	 */
	private ScalingMode(String name, int mode){
		this.name = name;
		this.mode = mode;
	}
	
	@Override
	public String toString(){
		return name;
	}
}
